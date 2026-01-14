package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.Map;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Test File path "/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv"
public class FileScanner {

    // Logger instance
    // private static Logger logger = new Logger();

    private volatile Status status = Status.IDLE;

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov" };

    // This method gets all media from the specified directory. We get all movies in this case.
    // Mapping will show up as follows:
    // - Map <FileName>, <pathToFile>
    // This is later used in the cleanupDirectory() method. 
    public Map<String, Path> indexAllMedia(String dirPath) {
        try {
            return Files.walk(Paths.get(dirPath))
                    .filter(FileScanner::isMediaFile)
                    .collect(Collectors.toMap(
                            path -> path.getFileName().toString(),
                            path -> path,
                            (a, b) -> a // handle duplicate filenames safely
                    ));
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyMap();
        }
    }

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

    public Status copyOffNAS(String path, String destination) {
        status = Status.RUNNING;

        try {
            System.out.println("Starting copy from: " + Paths.get(path) + " to: " + Paths.get(destination) + "...");

            Files.copy(Paths.get(path), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File successfully copied to Target: " + Paths.get(destination).toString());
            return Status.COMPLETED;
        } catch (IOException e) {
            System.out.println("File not found: " + e);
            return Status.FAILED;
        } finally {
            if(status == Status.RUNNING) {
                status = Status.COMPLETED;
            }
        }
    }

    // Loops through temp directory, grabs all file names, runs it against a map to determine where it is on the NAS. 
    public Status cleanupDirectory(Map<String, Path> nasIndex) {        
        // Changes status of method to "RUNNING".
        status = Status.RUNNING;

        Path tempMediaDir = Paths.get("/tmp/nascopiestest/");

        // List of files in the tempMediaDir
        try (Stream<Path> files = Files.list(tempMediaDir)) {
            files
                // Filters based on another Method if it's a media file (Follows extension rule)    
                .filter(FileScanner::isMediaFile)
                // For each file in that list...
                .forEach(tempPath -> {
                    try {
                        // Grabs the file name from the passed in Map
                        Path nasPath = nasIndex.get(tempPath.getFileName().toString());

                        System.out.println("NASIndex: " + nasIndex);
                        // If the path is null, exits the method
                        if (nasPath == null) {
                            System.err.println("No NAS match for: " + tempPath);
                            return;
                        }
                        // Expected file path (On the NAS)
                        long expected = Files.size(nasPath);
                        // Actual file path (Local copy)
                        long actual = Files.size(tempPath);

                        System.out.println(expected + " | " + actual);

                        // If the nas file isn't the same size as the actual path...
                        if (expected != actual) {
                            // Delete the file, prevents inconsistencies if interupted. 
                            // Files.delete(tempPath);
                            System.out.println("Deleted (incomplete copy): " + tempPath);
                        }
                    } catch (IOException e) {
                        System.err.println("Failed to process: " + e);
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
            return Status.FAILED;
        }
        // If no errors were caught, return Status COMPLETED.
        return Status.COMPLETED;
    }

    public long getFileSize(String file) {

        long sizeOfFile = file.length();

        return sizeOfFile;
    }

    public long getExpectedFileSize(Path nasPath) throws IOException {
        return Files.size(nasPath);
    }

    public String getMediaFileName(String file) {
        Path sourcePath = Paths.get(file);

        String fileName = sourcePath.getFileName().toString();

        return fileName;
    }
}
