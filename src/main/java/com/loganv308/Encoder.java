package com.loganv308;

import java.io.IOException;
import java.nio.file.Path;

//import com.loganv308.Database;
//import com.loganv308.FileScanner;

public class Encoder {

    // private Database db;
    // private FileScanner files;

    public Status reEncode(Path filePath) {
        String out = "";

        Process p = null;

        int exitCode = 0;

        try {

            System.out.println("Starting re-encode of: " + filePath + "\n");

            // Gets the encoding of whichever file you direct it to. 
            ProcessBuilder pb = new ProcessBuilder(
                "ffmpeg", 
                "-i", filePath.toString(), 
                "-c:v", "libx265",
                "-vtag", "hvc1",
                "-vf", "scale=1920:1080", 
                "-crf 20", "-c:a copy", 
                filePath.toString() 
            );

            // Assigned the processbuilder starting method to Process p;
            System.out.println("Process Started..." + "\n");
            p = pb.start();

            exitCode = p.waitFor();

            // Captures the output stream and reads it to the output String variable
            out = new String(p.getInputStream().readAllBytes());
            System.out.println("Output: " + out + "\n");
            out.trim();

        } catch (IOException | InterruptedException e) {
            System.out.println(e.getMessage());
            return Status.FAILED;
        }

        return exitCode == 0 ? Status.COMPLETED : Status.FAILED;
    }

    // This function will get the media encoding of a specified path
    public static Encoding getMediaEncoding(Path filePath) {
        try {
            // Gets the encoding of whichever file you direct it to. 
            ProcessBuilder pb = new ProcessBuilder(
                "ffprobe",
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePath.toString()
            );

            // Assigned the processbuilder starting method to Process p;
            Process p = pb.start();

            // Captures the output stream and reads it to the output String variable
            String out = new String(p.getInputStream().readAllBytes());

            p.waitFor();

            return fromEncoding(out);

        } catch (Exception e) {
            System.err.println("Failed to probe encoding: " + filePath);
            return Encoding.UNKNOWN;
        }
    }

    public static boolean isAbove1080p(Path mediaFile) {
        try {
            ProcessBuilder pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    mediaFile.toString()
            );

            Process p = pb.start();

            String output = new String(p.getInputStream().readAllBytes()).trim();
            p.waitFor();

            if (output.isEmpty()) {
                return false;
            }

            int height = Integer.parseInt(output);

            return height > 1080;

        } catch (Exception e) {
            System.err.println("Failed to probe resolution: " + mediaFile);
            return false; // fail safe: don't re-encode on error
        }
    }

    // Returns the proper encoding type. 
    public static Encoding fromEncoding(String encodingName) {
        return switch (encodingName.toLowerCase()) {
            case "h264" -> Encoding.H264;
            case "h265" -> Encoding.H265;
            case "hevc" -> Encoding.HEVC;
            case "vc1" -> Encoding.VC1;
            case "av1" -> Encoding.AV1;
            case "mpeg2video" -> Encoding.MPEG2VIDEO;
            default -> Encoding.UNKNOWN;
        };
    }

    public static boolean isWrongEncoding(Path mediaFile) {
        Encoding encoding = getMediaEncoding(mediaFile);

        // example: only allow HEVC
        return encoding != Encoding.HEVC;
    }
}