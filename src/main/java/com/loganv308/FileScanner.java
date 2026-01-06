package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;
import java.util.stream.Stream;

// Test File path "/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv"
public class FileScanner {

    // Logger instance
    // private static Logger logger = new Logger();

    private volatile Status status = Status.IDLE;

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov" };

    // This method gets all media from the specified directory. We get all movies in this case. 
    public List<Path> getAllMedia(String dirPath) {
        try {
            return Files.walk(Paths.get(dirPath))
                    .filter(FileScanner::isMediaFile) // check each extension
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
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

    public Status cleanupDirectory() {
        Path tempMediaDir = Paths.get("/tmp/nascopiestest/");

        // Takes in a Stream of paths from the temp media directory. Filters if it's a media file or not using the isMediaFile() method. 
        // For each file, it will try to delete if it's a media file (Checks for extension), then deletes and logs. 
        try (Stream<Path> files = Files.list(tempMediaDir)) {
            files
                .filter(FileScanner::isMediaFile)
                .forEach(path -> {
                    try {
                        Files.delete(path);
                        System.out.println("Deleted: " + path);
                    } catch (IOException e) {
                        System.err.println("Failed to delete: " + path);
                    }
                });
        } catch (IOException e) {
            e.printStackTrace();
            return Status.FAILED;
        }

        return Status.COMPLETED;
    }
}
