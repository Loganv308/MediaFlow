package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Map;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;
import java.util.logging.Logger;

import com.loganv308.cache.FileRecord;
import com.loganv308.cache.PersistentCache;
import com.loganv308.enums.JobState;

public class Runner {

    private static final Logger log = LoggerFactory.initLogger(Runner.class);

    public static void main(String[] args) {

        PersistentCache persistentCache = new PersistentCache();
        Runtime.getRuntime().addShutdownHook(new Thread(persistentCache::save));

        AppConfig config = new Utils().loadConfig();

        Encoder encoder = new Encoder(config.pathConfig);
        FileScanner fileScanner = new FileScanner(config.pathConfig, encoder);
        SpaceSavingCalculator ssc = new SpaceSavingCalculator(0.0);

        ScheduledExecutorService scheduler = Executors.newSingleThreadScheduledExecutor();

        Runnable cycle = () -> runCycle(config, fileScanner, encoder, ssc, persistentCache);

        scheduler.scheduleWithFixedDelay(cycle, 0, config.scanIntervalMillis, TimeUnit.MILLISECONDS);

        Runtime.getRuntime().addShutdownHook(new Thread(() -> {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(30, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
        }));
    }

    // Runs a single scan-and-process pass over the NAS. Scheduled to run again
    // after config.scanIntervalMillis once this returns, regardless of outcome.
    private static void runCycle(AppConfig config, FileScanner fileScanner, Encoder encoder,
                                  SpaceSavingCalculator ssc, PersistentCache persistentCache) {
        try {
            Path nasRoot = config.pathConfig.nasRoot;
            Path tempDir = config.pathConfig.tempDir;
            Files.createDirectories(tempDir);

            if (!Files.exists(nasRoot)) {
                log.info("NAS Media path not accessible, will retry next cycle...");
                return;
            }

            log.info("Getting media...");
            Map<String, Path> nasIndex = fileScanner.indexAllMedia(nasRoot, persistentCache.getCache());
            log.info("Files needing re-encode: " + nasIndex.size());

            if (nasIndex.isEmpty()) {
                log.info("No media found this cycle.");
                return;
            }

            fileScanner.cleanTempDirectory();

            for (Map.Entry<String, Path> entry : nasIndex.entrySet()) {
                try {
                    processFile(entry.getKey(), entry.getValue(), tempDir,
                        fileScanner, encoder, ssc, persistentCache);
                } catch (Exception e) {
                    // One file's failure must not abort the rest of the cycle.
                    log.severe("Skipping file due to error: " + entry.getKey() + " | " + e.getMessage());
                }
            }
        } catch (IOException e) {
            log.severe("Cycle failed with IOException: " + e.getMessage());
        } catch (RuntimeException e) {
            log.severe("Cycle failed with unexpected error: " + e.getMessage());
        }
    }

    // Copies one NAS file locally, re-encodes it, and writes the result back over
    // the original on the NAS (via the same mounted path used to read it) if it's
    // actually smaller. relativeKey is the file's path relative to the NAS root
    // (forward-slash normalized) and is used as the cache key.
    private static void processFile(String relativeKey, Path nasOriginalPath, Path tempDir,
                                      FileScanner fileScanner, Encoder encoder,
                                      SpaceSavingCalculator ssc, PersistentCache persistentCache) throws Exception {

        Map<String, FileRecord> cache = persistentCache.getCache();
        long lastModified = Files.getLastModifiedTime(nasOriginalPath).toMillis();
        String localTempPath = tempDir.resolve(nasOriginalPath.getFileName().toString()).toString();

        FileRecord record = cache.computeIfAbsent(relativeKey,
            k -> new FileRecord(nasOriginalPath, relativeKey, lastModified));
        record.setLastModified(lastModified);

        try {
            // 1. Measure original file on NAS
            double nasFileGB = ssc.getFileSizeGB(nasOriginalPath);
            log.info("NAS file size: " + nasFileGB + " GB (" + relativeKey + ")");

            // 2. Copy from NAS to local temp
            fileScanner.nasTransfer(nasOriginalPath.toString(), localTempPath);
            record.setState(JobState.COPIED);
            log.info("Copy complete: " + relativeKey);

            // 3. Re-encode locally
            log.info("Re-encoding: " + relativeKey);
            String outputEncodedPath = encoder.reEncode(localTempPath);
            record.setState(JobState.ENCODED);

            // 4. Measure re-encoded file
            double encodedGB = ssc.getFileSizeGB(Path.of(outputEncodedPath));
            log.info("Re-encoded size: " + encodedGB + " GB");

            // 5. Only write back if we actually saved space
            if (encodedGB < nasFileGB) {
                double saved = nasFileGB - encodedGB;
                log.info("Space saved: " + saved + " GB — writing back to NAS.");

                ssc.recordSaving(relativeKey, nasFileGB, encodedGB);

                fileScanner.nasTransfer(outputEncodedPath, nasOriginalPath.toString());
                record.setState(JobState.WRITTEN_BACK);
                log.info("Write-back complete: " + relativeKey);
            } else {
                log.info("Re-encoded file is NOT smaller (" + encodedGB
                    + " GB vs original " + nasFileGB
                    + " GB) — keeping original, skipping write-back.");

                ssc.recordSkipped(relativeKey, nasFileGB, encodedGB);
                record.setState(JobState.SKIPPED_NOT_SMALLER);
            }

            Files.deleteIfExists(Path.of(outputEncodedPath));

            log.info("Total saved so far: " + ssc.getTotalSavedGB() + " GB");

        } catch (Exception e) {
            record.setState(JobState.FAILED);
            record.setLastError(e.getMessage());
            record.incrementRetryCount();
            throw e;
        } finally {
            Files.deleteIfExists(Path.of(localTempPath));
            persistentCache.save();
        }
    }
}
