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

    // private static Encoder enc = new Encoder();

    private static FileScanner fs = new FileScanner();

    public static void main(String[] args) {
        
        // List<String> mediaFiles = fs.getAllMedia(mediaMount);

        // for (String file : mediaFiles){
        //     String p = enc.getMediaEncoding(file);

        //     // ENUM for every Encoding type present
        //     Encoding encodeType = Encoder.fromEncoding(p);
            
        //     System.out.println(encodeType);
        // }
        testreEncode();
            
    }

    public static void testreEncode() {
        try {
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
                System.out.println(i + "\n");
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}