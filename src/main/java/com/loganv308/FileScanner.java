package com.loganv308;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Collections;
import java.util.List;
import java.util.stream.Collectors;

// Test File path "/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv"
public class FileScanner {

    // Stringbuilder to hold output log
    // private static StringBuilder output = new StringBuilder();

    // Logger instance
    // private static Logger logger = new Logger();

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov" };

    // This method gets all media from the specified directory. We get all movies in this case. 
    public List<String> getAllMedia(String dirPath) {
        try {
            return Files.walk(Paths.get(dirPath))
                    .filter(Files::isRegularFile) // only files, no folders
                    .map(Path::toString) //
                    .filter(FileScanner::isMediaFile) // check each extension
                    .collect(Collectors.toList());
        } catch (IOException e) {
            e.printStackTrace();
            return Collections.emptyList();
        }
    }

    private static boolean isMediaFile(String path) {
        String lower = path.toLowerCase();
        for (String ext : EXTENSIONS) {
            if (lower.endsWith(ext)) return true;
        }
        return false;
    }

}
