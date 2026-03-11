package com.loganv308;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
// import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;

import com.loganv308.cache.PersistentCache;

public class Runner extends Thread {

    // Grabs environment variables from .env file
    // private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    // private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    private static Encoder enc = new Encoder();

    private static FileScanner fs = new FileScanner();

    private static utils ut = new utils();

    public static void main(String[] args) {

        PersistentCache persistentCache = new PersistentCache();
        
        Runtime.getRuntime().addShutdownHook(new Thread(persistentCache::save));

        System.out.println("MediaFlow Thread is running...");

        while(true) {
            try {
                // Initialize variables
                String tempDir = "";
                Path nasRoot = Paths.get("");

                // Determine OS and set paths accordingly
                if(ut.getOS().contains("win")) {
                    // Windows OS
                    tempDir = "C:\\tmp\\nascopiestest\\";

                    // If the temp directory doesn't exist, create it
                    if(!Files.exists(Paths.get(tempDir))) {
                        Files.createDirectories(Paths.get(tempDir));
                    }
                    // Check if NAS media path is accessible 
                    if(!Files.exists(Paths.get("Y:\\movies"))) {
                        System.out.println("NAS Media path not found, retrying in 10 minutes...");
                        try {
                            Thread.sleep(600000); // Sleep for 10 minutes
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return;
                    } else {
                        nasRoot = Paths.get("Y:\\movies");
                    }
                // Else if OS == Linux or Mac
                } else if(ut.getOS().contains("nix") || ut.getOS().contains("nux") || ut.getOS().contains("mac")) {
                    tempDir = "/tmp/nascopiestest/";
                    nasRoot = Paths.get("/mnt/NASMedia/movies");
                }

                System.out.println("Getting media...");

                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot, persistentCache.getCache());

                System.out.println("Size of list: " + nasIndex.size());

                if (nasIndex.isEmpty()) {
                    System.out.println("No media found, retrying in 10 minutes...");
                    try {
                        Thread.sleep(600000); // Sleep for 10 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }

                // Logs number of files needing re-encode
                System.out.println("Files needing re-encode: " + nasIndex.size());
                
                for(Map.Entry<String, Path> i : nasIndex.entrySet()) {
                    String fileName = i.getKey();
                    Path nasOriginalPath = i.getValue();
                    String formattedLocalFile = tempDir + "\\" + fileName;
                    
                    // Transfers the file from the NAS.
                    fs.nasTransfer(i.getValue().toString(), formattedLocalFile);

                    System.out.println("Copy complete for: " + fileName);
                    System.out.println("Re-encoding starting for " + fileName + " in progress...");

                    // Re-encoding logic for the local file.
                    enc.reEncode(formattedLocalFile);

                    System.out.println("Encoding complete for: " + formattedLocalFile);
                    System.out.println("Copying back to NAS...");

                    // Transfers the file back TO the NAS using the original file. 
                    fs.nasTransfer(formattedLocalFile, nasOriginalPath.toString());

                    // Will comment this out to cleanup directory, needs some minor re-working. 
                    // fs.cleanupDirectory(nasIndex);
                }

                // Gets list of files in temp directory
                // Refactor this perhaps.
                // List<Path> tempDirMediaList = fs.getTempPaths();

                // // Cleans up temp directory
                // for(Path p : tempDirMediaList) {
                //     System.out.println("Deleting temp file: " + p.toString());
                // }

            } catch (Exception e) {
                System.out.println(e);
            }
        }   
    }
}