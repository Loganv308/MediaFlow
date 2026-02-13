package com.loganv308;

import java.io.IOException;
import java.nio.file.Path;

import com.loganv308.enums.Encoding;
import com.loganv308.enums.Status;

public class Encoder {

    private Status status = Status.IDLE;

    private static utils ut = new utils();

    private static ProcessBuilder pb = null;

    public Status reEncode(String filePath) {
        String out = "";

        Process p = null;

        int exitCode = 0;

        status = Status.RUNNING;

        try {

            System.out.println("Starting re-encode of: " + filePath + "\n");

            if(ut.getOS().contains("win")) {
                filePath = filePath.replace("\\", "/");
                pb = new ProcessBuilder(
                    ".\\ffmpeg\\ffmpeg.exe", 
                    "-i", filePath, 
                    "-c:v", "libx265",
                    "-vtag", "hvc1",
                    "-vf", "scale=1920:1080", 
                    "-crf 20", "-c:a copy", 
                    filePath
                );
            } else {
                // Gets the encoding of whichever file you direct it to. 
                pb = new ProcessBuilder(
                    "ffmpeg", 
                    "-i", filePath, 
                    "-c:v", "libx265",
                    "-vtag", "hvc1",
                    "-vf", "scale=1920:1080", 
                    "-crf 20", "-c:a copy", 
                    filePath
                );
            }
            
            // Assigned the processbuilder starting method to Process p;
            System.out.println("Process Started..." + "\n");
            
            p = pb.start();

            exitCode = p.waitFor();

            // Captures the output stream and reads it to the output String variable
            out = new String(p.getInputStream().readAllBytes());
            System.out.println("Output: " + out + "\n");
            out.trim();

        } catch (IOException | InterruptedException e) {
            System.out.println(e);
            return Status.FAILED;
        }

        return exitCode == 0 ? Status.COMPLETED : Status.FAILED;
    }

    // This function will get the media encoding of a specified path
    public static Encoding getMediaEncoding(Path filePath) {
        try {
            if(ut.getOS().contains("win")) {
                filePath = Path.of(filePath.toString().replace("\\", "/"));

                // Gets the encoding of whichever file you direct it to. 
                pb = new ProcessBuilder(
                    ".\\ffmpeg\\ffprobe.exe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    filePath.toString()
                );

            } else {
                // Gets the encoding of whichever file you direct it to. 
                pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=codec_name",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    filePath.toString()
                );
            }
            
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

    public boolean isAbove1080p(Path mediaFile) {
        try {
            if(ut.getOS().contains("win")) {
                pb = new ProcessBuilder(
                    ".\\ffmpeg\\ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    mediaFile.toString()
                );
            } else {
                pb = new ProcessBuilder(
                    "ffprobe",
                    "-v", "error",
                    "-select_streams", "v:0",
                    "-show_entries", "stream=height",
                    "-of", "default=noprint_wrappers=1:nokey=1",
                    mediaFile.toString()
                );
            }

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

    // public static Encoding isWrongEncoding(Path mediaFile) {
    //     Encoding encoding = getMediaEncoding(mediaFile);

    //     System.out.println("Encoding: " + encoding);
        
    //     // example: only allow HEVC
    //     return encoding;
    // }
}