package com.loganv308;

import java.io.IOException;
import java.nio.file.DirectoryStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Stream;

import com.loganv308.cache.FileRecord;
import com.loganv308.enums.Encoding;

public class FileScanner {

    // Logger instance
    // private static Logger logger = new Logger();

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov" };

    private static final Path tempMediaDir = Paths.get("/tmp/nascopiestest/");

    // This method gets all media from the specified directory. We get all movies in this case.
    // Mapping will show up as follows:
    // - Map <FileName>, <pathToFile>
    // This is later used in the cleanupDirectory() method. 
    public Map<String, Path> indexAllMedia(Path dirPath, Map<String, FileRecord> cache) {

        // Initial map to append results too
        Map<String, Path> index = new HashMap<>();

        // Initializes an ArrayDeque
        Deque<Path> stack = new ArrayDeque<>();

        // Push the directory paths to the stack
        stack.push(dirPath);
        
        // While the stack does not contain file paths.
        while(!stack.isEmpty()) {
            
            // The dir variable equals stack.pop() and removes the most recently added path from the stack.
            // Will always process the deepest directory first, then go up from there. 
            // Equal to Path dir = stack.removeFirst();
            Path dir = stack.pop();
            System.out.println("Processing directory: " + dir);

            // Opens directory listing for dir variable. Returns DirectoryStream that lazily iterates entries.
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                // Iterates over each entry in the directory, each "p" is either a file or directory.
                for(Path p : stream) {
                    // If "p" is a directory...
                    if(Files.isDirectory(p)) {
                        System.out.println("Found subdirectory: " + p);
                        // It will add the directory to the stack. (Y:\movies\movieTitle) 
                        stack.push(p);
                    // Else, if the file is a media file...
                    } else if (FileScanner.isMediaFile(p)) {

                        String key = p.getFileName().toString();
                        long currentLastModified = Files.getLastModifiedTime(p).toMillis();
                        FileRecord cached = cache.get(key);

                            // If cached and file hasn't changed, reuse it
                        if (cached != null && cached.getLastModified() == currentLastModified) {
                            System.out.println("Cache hit, skipping: " + key);
                            index.putIfAbsent(key, p);
                            continue;
                        }
                        Encoding enc = Encoder.getMediaEncoding(p);
                        if (enc != Encoding.HEVC && enc != Encoding.H265) {
                            index.putIfAbsent(key, p);
                            cache.put(key, new FileRecord(p, currentLastModified));
                        } else {
                            System.out.println(p + " is already HEVC...");
                            // Cache it so we don't re-check encoding next run
                            cache.put(key, new FileRecord(p, currentLastModified));
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
    public List<Path> getTempPaths() {
        
        List<Path> tempFiles = new ArrayList<Path>();

        try(Stream<Path> files = Files.list(tempMediaDir)) {
            files
                .forEach (s -> {
                    tempFiles.add(s);
                });
        } catch (IOException e) {
            e.printStackTrace();
        }

        return tempFiles;
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

    public void nasTransfer(String path, String destination) {

        try {
            System.out.println("Starting copy from: " + Paths.get(path) + " to: " + Paths.get(destination) + "...");

            Files.copy(Paths.get(path), Paths.get(destination), StandardCopyOption.REPLACE_EXISTING);

            System.out.println("File successfully copied to Target: " + Paths.get(destination).toString());

        } catch (IOException e) {
            System.out.println("File not found: " + e);

        }
    }

    // Loops through temp directory, grabs all file names, runs it against a map to determine where it is on the NAS. 
    public void cleanupDirectory(Map<String, Path> nasIndex) {        

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
        }
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
