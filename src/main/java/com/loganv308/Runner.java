package com.loganv308;

import java.net.URI;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import io.github.cdimascio.dotenv.Dotenv;

public class Runner {

    // Grabs environment variables from .env file
    private static final Dotenv dotenv = Dotenv.load();

    // Media mount key in the .env file
    private static final String mediaMount = dotenv.get("MEDIA_MOUNT");

    private static Encoder enc = new Encoder();

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
            // Get Media Encoding
            String p = "/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv";

            Path sourcePath = Paths.get(p);
            
            String fileName = sourcePath.getFileName().toString();

            String o = "/tmp/nascopiestest/";

            Path newMediaPath = Paths.get(o, fileName);

            System.out.println("Media encoding String: " + p);
            System.out.println("Temp Directory String: " + o);
            System.out.println("SourcePath: " + sourcePath);
            System.out.println("Media File name: " + fileName);

            boolean exists = Files.exists(newMediaPath);

            System.out.println("New media path: " + newMediaPath);

            if(exists) {
                System.out.println("File already exists, started reencode...");
                enc.reEncode(newMediaPath.toString());
            } else {
                fs.copyOffNAS(p, o);
                enc.reEncode(newMediaPath.toString());
            }

        } catch (Exception e) {
            System.out.println(e);
        }
    }
}