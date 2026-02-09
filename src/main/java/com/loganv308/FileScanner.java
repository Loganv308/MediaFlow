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

import com.loganv308.enums.Status;

public class FileScanner {

    // Logger instance
    // private static Logger logger = new Logger();

    private volatile Status status = Status.IDLE;

    private static final String[] EXTENSIONS = { ".mp4", ".mkv", ".avi", ".mov" };

    private static final Path tempMediaDir = Paths.get("/tmp/nascopiestest/");

    // This method gets all media from the specified directory. We get all movies in this case.
    // Mapping will show up as follows:
    // - Map <FileName>, <pathToFile>
    // This is later used in the cleanupDirectory() method. 
    public Map<String, Path> indexAllMedia(Path dirPath) {

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

            // Opens directory listing for dir variable. Returns DirectoryStream that lazily iterates entries.
            try(DirectoryStream<Path> stream = Files.newDirectoryStream(dir)) {
                // Iterates over each entry in the directory, each "p" is either a file or directory.
                for(Path p : stream) {
                    // If "p" is a directory...
                    if(Files.isDirectory(p)) {
                        // It will add the sub directory to the stack. Will be scheduled to process later.
                        stack.push(p);
                    // Else, if the file is a media file...
                    } else if (FileScanner.isMediaFile(p)) {
                        // Looks up the filename in the "index" map, using the filename as a key.
                        index.putIfAbsent(p.getFileName().toString(), p);
                        
                        // Same thing as the following code:
                        
                        //if (!index.containsKey(key)) {
                            //index.put(key, p);
                        //}
                    }
                }
            // Standard error handling for IOException (File missing)
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
        // Returns the index map
        return index;
    } 
    

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
