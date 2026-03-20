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

    private static utils ut = new utils();
    private static Encoder enc = new Encoder();
    private static FileScanner fs = new FileScanner();
    private static Logger logger = LoggerFactory.initLogger();
    private static String tempDirString = "C:\\tmp\\nascopiestest\\";
    private static SpaceSavingCalculator ssc = new SpaceSavingCalculator(0.0);

    public static void main(String[] args) {

        // Cache class object call
        PersistentCache persistentCache = new PersistentCache();
        
        Runtime.getRuntime().addShutdownHook(new Thread(persistentCache::save));

        while(true) {
            try {
                Path nasRoot = Paths.get("");

                // Determine OS and set paths accordingly
                if(ut.getOS().contains("win")) {

                    // If the temp directory doesn't exist, create it
                    if(!Files.exists(Paths.get(tempDirString))) {
                        Files.createDirectories(Paths.get(tempDirString));
                    }
                    // Check if NAS media path is accessible 
                    if(!Files.exists(Paths.get("Y:\\Test"))) {
                        LoggerFactory.logInfo("NAS Media path not found, retrying in 10 minutes...");
                        try {
                            Thread.sleep(600000); // Sleep for 10 minutes
                        } catch (InterruptedException e) {
                            Thread.currentThread().interrupt();
                        }
                        return;
                    } else {
                        nasRoot = Paths.get("Y:\\Test");
                    }
                // Else if OS == Linux or Mac
                } else if(ut.getOS().contains("nix") || ut.getOS().contains("nux") || ut.getOS().contains("mac")) {
                    tempDirString = "/tmp/nascopiestest/";
                    nasRoot = Paths.get("Y:\\Test");
                }

                System.out.println("Getting media...");

                // Main NAS Index Map, runs against the NAS and utilizes the Cache. 
                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot, persistentCache.getCache());

                System.out.println("Size of list: " + nasIndex.size());

                // Checks if NAS list is empty.
                if (nasIndex.isEmpty()) {
                    System.out.println("No media found, retrying in 10 minutes...");

                    Thread.sleep(600000); // Sleep for 10 minutes

                    return;
                }

                // Logs number of files needing re-encode
                System.out.println("Files needing re-encode: " + nasIndex.size());
                
                /** 
                Deletes all files in the C:/tmp/nascopiestest/ to ensure no duplicates and always fresh copies of re-encoded media. 
                */
                fs.cleanTempDirectory();
                
                for(Map.Entry<String, Path> i : nasIndex.entrySet()) {
                    String fileName = i.getKey();
                    Path nasOriginalPath = i.getValue();

                    try {
                        Double nasFileSize = ssc.getExpectedFileSize(nasOriginalPath);
                    
                        System.out.println("NAS File Size: " + nasFileSize);
                        
                        // Formatted String, this puts the temp directory string and filename together.
                        String formattedLocalFile = tempDirString + "\\" + fileName;
                        
                        // Transfers the file from the NAS.
                        fs.nasTransfer(i.getValue().toString(), formattedLocalFile);

                        System.out.println("Copy complete for: " + fileName);
                        System.out.println("Re-encoding starting for " + fileName + " in progress...");

                        // Re-encoding logic for the local file.
                        String outputEncodedPath = enc.reEncode(formattedLocalFile);

                        Double localFilePathSize = ssc.getExpectedFileSize(Path.of(outputEncodedPath));

                        Double totalSaved = ssc.spaceSaved(nasFileSize, localFilePathSize);

                        ssc.storeValue(totalSaved, ssc);

                        System.out.println("Encoding complete for: " + formattedLocalFile);
                        System.out.println("Space saved: " + totalSaved);
                        System.out.println("Copying back to NAS...");

                        // Transfers the file back to the NAS using the new encoded file, and original NAS path. 
                        fs.nasTransfer(outputEncodedPath.toString(), nasOriginalPath.toString());

                        //
                        LoggerFactory.logInfo("Rescanning in 10 minutes...");

                        // Sleep for 10 minutes before re-scanning NAS. 
                        Thread.sleep(600000);
                        
                    } catch (RuntimeException e) {
                        System.err.println("Skipping file due to error: " + fileName);
                        System.err.println("Reason: " + e.getMessage());
                    }
                }

            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } catch (IOException e) {
                LoggerFactory.logError("File not found.");
            } 
        }   
    }
}