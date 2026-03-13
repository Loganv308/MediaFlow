package com.loganv308;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import com.loganv308.enums.Encoding;

public class Encoder {
    private static FileScanner fs = new FileScanner();

    private static utils ut = new utils();

    private static ProcessBuilder pb = null;

    // Main re-encoding method, will run a specific FFMPEG command to run against each media file needing re-encoding. 
    public void reEncode(String filePath) {
        Process p = null;

        String fileExtension = fs.getFileExtension(filePath);

        try {

            System.out.println("Starting re-encode of: " + filePath + "\n");

            String ffmpegBin = ut.getOS().contains("win") ? ".\\ffmpeg\\ffmpeg.exe" : "ffmpeg";

            if(ut.getOS().contains("win")) filePath = filePath.replace("\\", "/");

            pb = new ProcessBuilder(
                ffmpegBin, 
                "-i", filePath, 
                "-c:v", "h264_nvenc",
                "-preset", "p5",
                "-cq", "28",
                "-rc", "vbr",
                "-filter_complex", "[0:v]scale='min(1920,iw)':'min(1080,ih)':force_original_aspect_ratio=decrease[outv]",
                "-map", "[outv]",
                "-map", "0:a?",
                "-c:a", "copy",
                filePath + "Reencoded - " + ut.getDate().toString() + fileExtension
            );
            
            // Assigned the processbuilder starting method to Process p;
            System.out.println("Process Started..." + "\n");
            
            // Starts the process
            p = pb.start();

            // Start standard output consumer thread
            InputStreamConsumer outputConsumer = new InputStreamConsumer(p.getInputStream(), "OUTPUT");
            Thread outputThread = new Thread(outputConsumer);
            outputThread.start();

            // Start error consumer thread
            InputStreamConsumer errorConsumer = new InputStreamConsumer(p.getErrorStream(), "ERROR");
            Thread errorThread = new Thread(errorConsumer);
            errorThread.start();

            // Wait for FFmpeg to finish
            int exitCode = p.waitFor();

            // Wait for both threads to finish reading streams
            errorThread.join();
            outputThread.join();

            // Now you can safely get the output/errors
            String errors = errorConsumer.getOutput().trim();
            String output = outputConsumer.getOutput().trim();

            // Print output
            System.out.println("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                System.out.println("FFmpeg messages:\n" + errors);
            }

            System.out.println("FFmpeg exited with code: " + exitCode);
            System.out.println("Output:\n" + output);

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
            }
            
        } catch (IOException e) {
            System.err.println("IOException (File): " + e);
        } catch (InterruptedException e) {
            System.err.println("Interrupted Exception: ");
        }
    }

    // This function will get the media encoding of a specified path
    public static Encoding getMediaEncoding(Path filePath) {
        try {
            // if OS == win then use ffmpeg executable in ffmpeg folder, otherwise use ffmpeg command via linux terminal. 
            String ffmpegBin = ut.getOS().contains("win") ? ".\\ffmpeg\\ffmpeg.exe" : "ffmpeg";

            // Convert to String from Path
            String filePathStr = filePath.toString();

            // Gets the encoding of whichever file you direct it to. 
            pb = new ProcessBuilder(
                ffmpegBin,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePathStr
            );
            
            // Assigned the processbuilder starting method to Process p;
            Process p = pb.start();

            // Start error consumer thread
            InputStreamConsumer errorConsumer = new InputStreamConsumer(p.getErrorStream(), "ERROR");
            Thread errorThread = new Thread(errorConsumer);
            errorThread.start();

            InputStreamConsumer outputConsumer = new InputStreamConsumer(p.getInputStream(), "OUTPUT");
            Thread outputThread = new Thread(outputConsumer);
            outputThread.start();

            // Wait for FFmpeg to finish
            int exitCode = p.waitFor();

            // Wait for both threads to finish reading streams
            errorThread.join();
            outputThread.join();

            // Now you can safely get the output/errors
            String errors = errorConsumer.getOutput().trim();
            String output = outputConsumer.getOutput().trim();

            // Print output
            System.out.println("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                System.out.println("FFmpeg messages:\n" + errors);
            }

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
            }
            
            // The .trim() is NECESSARY here, without it, ffprobe outputs "hevc\n" instead of just "hevc"
            return fromEncoding(output);

        } catch (Exception e) {
            System.err.println("Failed to probe encoding: " + filePath);
            return Encoding.UNKNOWN;
        }
    }

    // Checks if media file is above 1080p resolution. 
    public boolean isAbove1080p(Path mediaFile) {
        try {
            // if OS == win then use ffmpeg executable in ffmpeg folder, otherwise use ffmpeg command via linux terminal. 
            String ffmpegBin = ut.getOS().contains("win") ? ".\\ffmpeg\\ffmpeg.exe" : "ffmpeg";

            // Convert to String from Path
            String filePathStr = mediaFile.toString();

            // Process builder string
            pb = new ProcessBuilder(
                ffmpegBin,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=height",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePathStr
            );

            // Starts process 
            Process p = pb.start();

            // Start error consumer thread
            InputStreamConsumer errorConsumer = new InputStreamConsumer(p.getErrorStream(), "ERROR");
            Thread errorThread = new Thread(errorConsumer);
            errorThread.start();

            InputStreamConsumer outputConsumer = new InputStreamConsumer(p.getInputStream(), "OUTPUT");
            Thread outputThread = new Thread(outputConsumer);
            outputThread.start();

            // Wait for FFmpeg to finish
            int exitCode = p.waitFor();

            // Wait for both threads to finish reading streams
            errorThread.join();
            outputThread.join();

            // Now you can safely get the output/errors
            String errors = errorConsumer.getOutput().trim();
            String output = outputConsumer.getOutput().trim();

            // Print output
            System.out.println("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                System.out.println("FFmpeg messages:\n" + errors);
            }

            System.out.println("FFmpeg exited with code: " + exitCode);
            System.out.println("Output:\n" + output);

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
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
        Encoding resultEncoding;
        
        switch (encodingName.toLowerCase()) {
            case "h264":
                resultEncoding = Encoding.H264;
                break;
            case "h265":
                resultEncoding = Encoding.H265;
                break;
            case "hevc":
                resultEncoding = Encoding.HEVC;
                break;
            case "vc1":
                resultEncoding = Encoding.VC1;
                break;
            case "av1":
                resultEncoding = Encoding.AV1;
                break;
            case "mpeg2video":
                resultEncoding = Encoding.MPEG2VIDEO;
                break;
            default:
                resultEncoding = Encoding.UNKNOWN;
                break;
        };
        return resultEncoding;
    }
}

// InputStream class. Used for FFMPEG methods. 
class InputStreamConsumer implements Runnable {
        private InputStream inputStream;
        private String type;
        private StringBuilder output = new StringBuilder();

        public InputStreamConsumer(InputStream inputStream, String type) {
            this.inputStream = inputStream;
            this.type = type;
        }

        public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
                System.out.println(type + "> " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

        public String getOutput() {
            return output.toString();
        }
    }