package com.loganv308;

import java.io.IOException;

//import com.loganv308.Database;
//import com.loganv308.FileScanner;

public class Encoder {

    // private Database db;
    // private FileScanner files;

    public String reEncode(String filePath) {
        String out = "";

        try {
            Process p;

            // Gets the encoding of whichever file you direct it to. 
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            );
            
            // Assigned the processbuilder starting method to Process p;
            p = pb.start();

            // Captures the output stream and reads it to the output String variable
            out = new String(p.getInputStream().readAllBytes());

        } catch (IOException e) {
            System.out.println(e);
        }
        return out.trim();
    }

    // This function will get the media encoding of a specified path
    public String getMediaEncoding(String filePath) {
        // Initial output String initialized beforehand
        String out = "";

        try {
            Process p;
            
            // Gets the encoding of whichever file you direct it to. 
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath
            );

            // Assigned the processbuilder starting method to Process p;
            p = pb.start();

            // Captures the output stream and reads it to the output String variable
            out = new String(p.getInputStream().readAllBytes());

        } catch (IOException e) {
            System.out.println(e);
        }
        // Returns output
        return out.trim();
    }

    public static Encoding fromEncoding(String encodingName) {
        try {
            switch (encodingName.toLowerCase()) {
                case "h264":
                    return Encoding.H264;
                case "h265":
                    return Encoding.H265;
                case "hevc":
                    return Encoding.HEVC;
                case "vc1":
                    return Encoding.VC1;
                case "av1":
                    return Encoding.AV1;
                case "mpeg2video":
                    return Encoding.MPEG2VIDEO;
                default:
                    return Encoding.UNKNOWN;
            }
        } catch (Exception e) {
            // Log Exception 
            System.out.println(e);
            return Encoding.UNKNOWN;
        }
        
    }
}