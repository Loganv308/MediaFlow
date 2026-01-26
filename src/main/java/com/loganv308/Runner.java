package com.loganv308;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;
// import io.github.cdimascio.dotenv.Dotenv;
import java.util.Map;

public class Runner {

    // Grabs environment variables from .env file
    // private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    // private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    private static Encoder enc = new Encoder();

    private static FileScanner fs = new FileScanner();

    public static void main(String[] args) {
        while(true) {
            try {
                String tempDir = "/tmp/nascopiestest/";

                System.out.println("Getting media...");

                Path nasRoot = Paths.get("/mnt/NASMedia/movies");

                Map<String, Path> nasIndex = fs.indexAllMedia(nasRoot.toString());

                if (nasIndex.isEmpty()) {
                    System.out.println("No media found.");
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