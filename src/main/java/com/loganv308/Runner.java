package com.loganv308;

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

                String tempDir = "";
                Path nasRoot = Paths.get("");

                // Determine OS and set paths accordingly
                if(ut.getOS().contains("win")) {
                    tempDir = "C:\\tmp\\nascopiestest\\";
                    nasRoot = Paths.get("Y:\\movies");
                // Else if OS == Linux or Mac
                } else if(ut.getOS().contains("nix") || ut.getOS().contains("nux") || ut.getOS().contains("mac")) {
                    tempDir = "/tmp/nascopiestest/";
                    nasRoot = Paths.get("/mnt/NASMedia/movies");
                }

                System.out.println("Getting media...");

                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot.toString());

                if (nasIndex.isEmpty()) {
                    System.out.println("No media found, retrying in 10 minutes...");
                    try {
                        Thread.sleep(600000); // Sleep for 10 minutes
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                    return;
                }

                List<Path> needsReencode = nasIndex.values().stream()
                    .filter(Encoder::isWrongEncoding)
                    .filter(Encoder::isAbove1080p)
                    .toList();

                System.out.println("Files needing re-encode: ");
                
                for(Path i : needsReencode) {
                    fs.copyOffNAS(i.toString(), tempDir);
                    System.out.println("Copy complete for: " + i.toString());
                }

                List<Path> tempDirMediaList = fs.getTempPaths();

                for(Path p : tempDirMediaList) {
                    System.out.println("Re-encoding for " + p.toString() + " in progress...");
                    enc.reEncode(p.toString());
                }
                
                // TODO: Utilize outside classes to scan NAS for new and previous media, encode once found if needed. 

            } catch (Exception e) {
                System.out.println(e);
            }
        }   
    }
}