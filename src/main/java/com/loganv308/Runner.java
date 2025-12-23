package com.loganv308;

import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class Runner {

    // Grabs environment variables from .env file
    private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    public static void main(String[] args) {
        Encoder enc = new Encoder();

        FileScanner fs = new FileScanner();

        // Get Media Encoding
        // String p = enc.getMediaEncoding("/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv");
        
        List<String> mediaFiles = fs.getAllMedia(mediaMount);

        for (String file : mediaFiles){
            String p = enc.getMediaEncoding(file);

            // ENUM for every Encoding type present
            Encoding encodeType = Encoder.fromEncoding(p);
            
            System.out.println(encodeType);
        }
            
    }
}