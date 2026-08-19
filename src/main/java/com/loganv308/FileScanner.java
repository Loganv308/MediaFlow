package com.loganv308;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.HashMap;
import java.util.Map;

import com.loganv308.cache.FileRecord;
import com.loganv308.enums.Encoding;
import com.loganv308.enums.JobState;

public class FileScanner {

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov", ".m2ts" };

    private final Path tempMediaDir;
    private final Encoder encoder;
    private final Database database;

    public FileScanner(PathConfig paths, Encoder encoder, Database database) {
        this.tempMediaDir = paths.tempDir;
        this.encoder = encoder;
        this.database = database;
    }

    // This method gets all media from the specified directory. We get all movies in this case.
    // Mapping will show up as follows:
    // - Map <NAS-relative path>, <pathToFile>
    // Keys are the file's path relative to dirPath (forward-slash normalized), so files
    // are uniquely identified across subfolders even when basenames collide.
    public Map<String, Path> indexAllMedia(Path dirPath, Map<String, FileRecord> cache) {

        // Initial map to append results too
        Map<String, Path> index = new HashMap<>();

        // Local stack, scoped to this call only.
        Deque<Path> stack = new ArrayDeque<>();
        stack.push(dirPath);

        // While the stack does not contain file paths.
        while (!stack.isEmpty()) {

            // The dir variable equals stack.pop() and removes the most recently added path from the stack.
            // Will always process the deepest directory first, then go up from there.
            Path dir = stack.pop();
            System.out.println("Processing directory: " + dir);

            // Opens directory listing for dir variable. Returns DirectoryStream that lazily iterates entries.
            try (DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                // Iterates over each entry in the directory, each "p" is either a file or directory.
                for (Path p : stream) {
                    // If "p" is a directory...
                    if (Files.isDirectory(p)) {
                        System.out.println("Found subdirectory: " + p);
                        // It will add the directory to the stack. (Y:\movies\movieTitle)
                        stack.push(p);
                    // Else, if the file is a media file...
                    } else if (FileScanner.isMediaFile(p)) {

                        String key = dirPath.relativize(p).toString().replace("\\", "/");
                        long currentLastModified = Files.getLastModifiedTime(p).toMillis();
                        long currentSize = Files.size(p);
                        FileRecord cached = cache.get(key);

                        // If cached and file hasn't changed, decide whether it's done or still owes work.
                        // "Unchanged" requires EITHER mtime or size to still match, not both — SMB/CIFS
                        // mounts can report slightly different mtimes across separate mount sessions
                        // (e.g. after a container restart), so mtime alone is too fragile a signal here.
                        boolean unchanged = cached != null
                            && (cached.getLastModified() == currentLastModified || cached.getFileSizeBytes() == currentSize);

                        if (unchanged) {
                            JobState state = cached.getState();
                            if (state == JobState.WRITTEN_BACK || state == JobState.ALREADY_COMPLIANT
                                || state == JobState.SKIPPED_NOT_SMALLER) {
                                System.out.println("Cache hit, already handled, skipping: " + key);
                            } else {
                                System.out.println("Cache hit, still owes work, re-queueing: " + key);
                                index.putIfAbsent(key, p);
                            }
                            continue;
                        }

                        Encoding enc = encoder.getMediaEncoding(p);
                        if (enc == Encoding.UNKNOWN) {
                            // Probe failed (transient NAS/network hiccup, mount not fully settled, etc).
                            // Do NOT treat this as "confirmed not HEVC" — leave it uncached so it gets
                            // probed fresh next cycle instead of triggering a needless re-encode.
                            System.out.println("Could not determine encoding for " + p + ", will retry next cycle.");
                        } else if (enc != Encoding.HEVC && enc != Encoding.H265) {
                            // Still not HEVC — but if we've already tried re-encoding this exact
                            // file before and confirmed it wasn't worth it, don't blindly re-attempt
                            // just because the local NAS-side cache lost track of it (e.g. a CIFS
                            // mount remount shifting mtimes). Postgres remembers this permanently,
                            // independent of whether the fragile local cache stayed in sync.
                            if (database.wasPreviouslySkipped(key)) {
                                System.out.println("Already confirmed not worth re-encoding, skipping: " + key);
                                FileRecord skipped = new FileRecord(p, key, currentLastModified);
                                skipped.setFileSizeBytes(currentSize);
                                skipped.setState(JobState.SKIPPED_NOT_SMALLER);
                                cache.put(key, skipped);
                            } else {
                                index.putIfAbsent(key, p);
                                FileRecord record = new FileRecord(p, key, currentLastModified);
                                record.setFileSizeBytes(currentSize);
                                cache.put(key, record);
                            }
                        } else {
                            System.out.println(p + " is already HEVC...");
                            FileRecord compliant = new FileRecord(p, key, currentLastModified);
                            compliant.setFileSizeBytes(currentSize);
                            compliant.setState(JobState.ALREADY_COMPLIANT);
                            // Cache it so we don't re-check encoding next run
                            cache.put(key, compliant);
                        }
                    }
                }
            // Standard error handling for IOException (File missing)
            } catch (IOException e) {
                System.err.println("Failed to read directory: " + dir + " -> " + e.getMessage());
            }
        }
        System.out.println("Indexing complete. Total media files: " + index.size());
        // Returns the index map
        return index;
    }

    // Gets all temporary paths in /tmp directory
    public void cleanTempDirectory() {
        try (DirectoryStream<Path> files = Files.newDirectoryStream(tempMediaDir)) {
            for (Path p : files) {
                System.out.println("Deleting file: " + p);
                Files.delete(p);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    // Simple boolean to check if file ends with a specific extension from a custom list of extensions.
    private static boolean isMediaFile(Path path) {
        if (!Files.isRegularFile(path)) {
            return false;
        }

        String filename = path.getFileName().toString().toLowerCase();

        for (String ext : EXTENSIONS) {
            if (filename.endsWith(ext)) {
                return true;
            }
        }
        return false;
    }

    // Transfers to and from a location (in this case the NAS), specify your path and then destination.
    public void nasTransfer(String path, String destination) throws IOException {
        System.out.println("Starting copy from: " + Paths.get(path) + " to: " + Paths.get(destination) + "...");

        Files.copy(Paths.get(path), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);

        System.out.println("File successfully copied to Target: " + Paths.get(destination).toString());
    }

    // Grabs only the file name of the media file
    public static String getMediaFileName(String file) {
        Path sourcePath = Paths.get(file);

        return sourcePath.getFileName().toString();
    }

    // Grabs the file extension
    public static String getFileExtension(String fileName) {
        if (fileName == null) {
            return null;
        }
        int dotIndex = fileName.lastIndexOf(".");

        if (dotIndex >= 0) {
            return fileName.substring(dotIndex);
        }

        return "No file extension...";
    }
}
