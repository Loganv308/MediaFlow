package com.loganv308;

import java.io.BufferedReader;
import java.io.File;
import java.io.IOException;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

import org.json.JSONArray;
import org.json.JSONObject;

import io.github.cdimascio.dotenv.Dotenv;

import com.loganv308.Logger;

// Test File path "/mnt/NASMedia/movies/Flow (2024)/Flow 2024 2160p AMZN WEB DL DDP5 1 H 265 FLUX.mkv"

public class FileScanner {

    private static final Dotenv dotenv = Dotenv.load();
    private static final String MEDIAMOUNT = dotenv.get("MEDIA_MOUNT");

    // private static StringBuilder output = new StringBuilder();
    
    // private static List<File> mediaFiles = new ArrayList<>();

    // private static Logger logger = new Logger();

    public static void walkDirectory(File file) {
        if(file.isFile()) {
            System.out.println("Media File Found: " + file.getAbsolutePath());
        }
    }

    public static Process getEncoding(String filePath) {
        // Gets the encoding of whichever file you direct it to. 
        ProcessBuilder pb = new ProcessBuilder(
            "ffprobe -v error -select_streams v:0 -show_entries stream=codec_name -of default=noprint_wrappers=1:nokey=1 " + filePath
        );

        Process p = null;

        try {
            p = pb.start();

        } catch (IOException e) {
            System.out.println(e);
        }

        return p;
    }

    public static String readJson(Process p) {

        StringBuilder json = null;

        try (BufferedReader reader = new BufferedReader(new InputStreamReader(p.getInputStream()))) {
            json = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                json.append(line);
            }
        } catch (IOException e) {
            System.out.println(e);
        }

        JSONObject obj = new JSONObject(json.toString());
        JSONArray streams = obj.getJSONArray("streams");
        
        
        String s = "";
        return s;
    }
}
