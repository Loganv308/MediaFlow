package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
// import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;
import java.util.logging.Logger;

import com.loganv308.cache.PersistentCache;

public class Runner extends Thread {

    // Grabs environment variables from .env file
    // private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    // private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    private static Utils ut = new Utils();
    private static Encoder enc = new Encoder();
    private static FileScanner fs = new FileScanner();
    private static Logger log = LoggerFactory.initLogger(Runner.class);
    private static SpaceSavingCalculator ssc = new SpaceSavingCalculator(0.0);

    // Resolved at runtime
    private static Path nasRoot;
    private static String tempDirString;

    /**
     * Detects the current OS and configures file paths accordingly.
     * @throws UnsupportedOperationException if the OS is not supported.
     */
    public static void main(String[] args) {

        // Cache class object call
        PersistentCache persistentCache = new PersistentCache();
        Runtime.getRuntime().addShutdownHook(new Thread(persistentCache::save));

        PathConfig config = ut.configurePaths();

        nasRoot = config.nasRoot;
        tempDirString = config.tempDir.toString();

        while(true) {
            try {
                Files.createDirectories(Paths.get(tempDirString));

                // If the temp directory doesn't exist, create it
                if(!Files.exists(nasRoot)) {
                    log.info("NAS Media path not accessible, retrying in 10 minutes...");
                    try {
                        Thread.sleep(600000); // Sleep for 10 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return;
                    }
                    continue;
                }

                log.info("Getting media...");

                // Main NAS Index Map, runs against the NAS and utilizes the Cache. 
                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot, persistentCache.getCache());

                log.info("Size of list: " + nasIndex.size());

                // Checks if NAS list is empty.
                if (nasIndex.isEmpty()) {
                    log.info("No media found, retrying in 10 minutes...");

                    Thread.sleep(600000); // Sleep for 10 minutes

                    return;
                }

                // Logs number of files needing re-encode
                log.info("Files needing re-encode: " + nasIndex.size());
                
                /** 
                Deletes all files in the C:/tmp/nascopiestest/ to ensure no duplicates and always fresh copies of re-encoded media. 
                */
                fs.cleanTempDirectory();
                
                for(Map.Entry<String, Path> i : nasIndex.entrySet()) {
                    String fileName = i.getKey();
                    Path nasOriginalPath = i.getValue();

                    try {
                        Double nasFileSize = ssc.getExpectedFileSize(nasOriginalPath);

                        log.info("NAS File Size: " + nasFileSize);
                        
                        // Formatted String, this puts the temp directory string and filename together.
                        String formattedLocalFile = tempDirString + "\\" + fileName;
                        
                        // Transfers the file from the NAS.
                        fs.nasTransfer(i.getValue().toString(), formattedLocalFile);

                        log.info("Copy complete for: " + fileName);
                        log.info("Re-encoding starting for " + fileName + " in progress...");

                        // Re-encoding logic for the local file.
                        String outputEncodedPath = enc.reEncode(formattedLocalFile);

                        Double localFilePathSize = ssc.getExpectedFileSize(Path.of(outputEncodedPath));

                        System.out.println("localFilePathSize: " + localFilePathSize);

                        Double totalSaved = ssc.spaceSaved(nasFileSize, localFilePathSize);

                        System.out.println("TOTALSAVED: " + totalSaved);

                        ssc.storeValue(totalSaved, ssc);

                        log.info("Encoding complete for: " + formattedLocalFile);
                        log.info("Space saved: " + totalSaved);
                        log.info("Copying back to NAS...");

                        // Transfers the file back to the NAS using the new encoded file, and original NAS path. 
                        // fs.nasTransfer(outputEncodedPath.toString(), nasOriginalPath.toString());

                        log.info("Rescanning in 10 minutes...");

                        // Sleep for 10 minutes before re-scanning NAS. 
                        Thread.sleep(600000);
                        
                    } catch (RuntimeException e) {
                        log.severe("Skipping file due to error: " + fileName);
                        log.severe("Reason: " + e.getMessage());
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                log.severe("File not found.");
            } 
        }   
    }
}