package com.loganv308;

import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
// import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;

public class Runner extends Thread {

    // Grabs environment variables from .env file
    // private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    // private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    private static Encoder enc = new Encoder();

    private static FileScanner fs = new FileScanner();

    private static utils ut = new utils();

    public static void main(String[] args) {
        
        Runner thread = new Runner();
        
        thread.start();

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

                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot.toString());

                System.out.println(nasIndex);

                if (nasIndex.isEmpty()) {
                    System.out.println("No media found, retrying in 10 minutes...");
                    try {
                        Thread.sleep(600000); // Sleep for 10 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }

                // Filters the index for files needing re-encoding
                List<Path> needsReencode = nasIndex.values().stream()
                    .filter(Encoder::isWrongEncoding)
                    .filter(Encoder::isAbove1080p)
                    .toList();

                // Logs number of files needing re-encode
                System.out.println("Files needing re-encode: " + needsReencode.size());
                
                for(Path i : needsReencode) {
                    fs.copyOffNAS(i.toString(), tempDir);
                    System.out.println("Copy complete for: " + i.toString());
                    System.out.println("Re-encoding for " + i.toString() + " in progress...");
                    enc.reEncode(i.toString());
                }

                // Gets list of files in temp directory
                List<Path> tempDirMediaList = fs.getTempPaths();

                // Cleans up temp directory
                // for(Path p : tempDirMediaList) {

                // }



            } catch (Exception e) {
                System.out.println(e);
            }
        }   
    }
}