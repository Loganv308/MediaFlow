package com.loganv308;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import java.util.logging.Logger;

import com.loganv308.enums.Encoding;

public class Encoder {
    // Utils objects instance
    private final Utils ut = new Utils();

    // Path config setup, makes it easier to grab the correct file path based on OS.
    private final PathConfig paths;

    // ffmpeg and ffprobe path setup based on OS.
    private final String ffmpegBin;
    private final String ffprobeBin;

    // Initialize custom LoggerFactory
    private static Logger log = LoggerFactory.initLogger(Encoder.class);

    public Encoder(PathConfig paths) {
        this.paths = paths;
        this.ffmpegBin = paths.ffmpegBin;
        this.ffprobeBin = paths.ffprobeBin;
    }

    // Main re-encoding method, will run a specific FFMPEG command to run against each media file needing re-encoding.
    public String reEncode(String filePath) {

        // Normalize the filepath if Windows
        filePath = paths.normalizePath(filePath);

        // Initialize process as NULL
        Process p = null;

        // Get the file extension of the filepath.
        String fileExtension = FileScanner.getFileExtension(filePath);

        // Outputted file path being returned to the main process
        String basePath = filePath.substring(0, filePath.lastIndexOf('.'));
        String formattedOutputString = basePath + "_Reencoded_" + ut.getDate().toString() + fileExtension;

        try {
            log.info("Starting re-encode of: " + filePath + "\n");

            ProcessBuilder pb = buildEncodeCommand(filePath, fileExtension, formattedOutputString);

            // Assigned the processbuilder starting method to Process p;
            log.info("Process Started..." + "\n");

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
            log.info("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                log.info("FFmpeg messages:\n" + errors);
            }

            log.info("FFmpeg exited with code: " + exitCode);
            log.info("Output:\n" + output);

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
            }

        } catch (IOException e) {
            log.severe("IOException (File): " + e);
            throw new RuntimeException("Re-encode failed for " + filePath, e);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            log.severe("Interrupted Exception: " + e);
            throw new RuntimeException("Re-encode interrupted for " + filePath, e);
        }

        return formattedOutputString;
    }

    // Builds the ffmpeg command for a HEVC (NVENC) re-encode. Adds the hvc1
    // tag for mp4/mov containers, which many Apple/QuickTime-family players
    // require to recognize HEVC (ffmpeg's default tag is hev1).
    private ProcessBuilder buildEncodeCommand(String filePath, String fileExtension, String formattedOutputString) {
        boolean needsHvc1Tag = fileExtension != null
            && (fileExtension.equalsIgnoreCase(".mp4") || fileExtension.equalsIgnoreCase(".mov"));

        java.util.List<String> command = new java.util.ArrayList<>(java.util.List.of(
            ffmpegBin,
            "-i", filePath,
            "-c:v", "hevc_nvenc",
            "-preset", "p5",
            "-cq", "28",
            "-rc", "vbr",
            "-filter_complex", "[0:v]scale='min(1920,iw)':'min(1080,ih)':force_original_aspect_ratio=decrease[outv]",
            "-map", "[outv]",
            "-map", "0:a?",
            "-c:a", "copy"
        ));

        if (needsHvc1Tag) {
            command.add("-tag:v");
            command.add("hvc1");
        }

        command.add(formattedOutputString);

        return new ProcessBuilder(command);
    }

    // This function will get the media encoding of a specified path
    public Encoding getMediaEncoding(Path filePath) {
        try {

            // Normalize the filepath if Windows
            filePath = paths.normalizePath(filePath);

            // Convert to String from Path
            String filePathStr = filePath.toString();

            // Gets the encoding of whichever file you direct it to.
            ProcessBuilder pb = new ProcessBuilder(
                ffprobeBin,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=codec_name",
                "-of", "default=noprint_wrappers=1:nokey=1",
                filePathStr
            );

            // Assigned the processbuilder starting method to Process p;
            Process p = pb.start();

            // Start normal output consumer thread
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
            log.info("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                log.info("FFmpeg messages:\n" + errors);
            }

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
            }

            // The .trim() is NECESSARY here, without it, ffprobe outputs "hevc\n" instead of just "hevc"
            return fromEncoding(output);

        } catch (Exception e) {
            log.severe("Failed to probe encoding: " + filePath + " | " + e);
            return Encoding.UNKNOWN;
        }
    }

    // Checks if media file is above 1080p resolution.
    public boolean isAbove1080p(Path mediaFile) {
        try {
            // Normalize the filepath if Windows
            mediaFile = paths.normalizePath(mediaFile);

            // Process builder string
            ProcessBuilder pb = new ProcessBuilder(
                ffprobeBin,
                "-v", "error",
                "-select_streams", "v:0",
                "-show_entries", "stream=height",
                "-of", "default=noprint_wrappers=1:nokey=1",
                mediaFile.toString()
            );

            // Starts process
            Process p = pb.start();

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
            log.info("FFmpeg exited with code: " + exitCode);
            if (!errors.isEmpty()) {
                log.info("FFmpeg messages:\n" + errors);
            }

            // Handle errors if FFmpeg failed
            if (exitCode != 0) {
                throw new RuntimeException("FFmpeg failed: " + errors);
            }

            int height = Integer.parseInt(output);

            return height > 1080;

        } catch (Exception e) {
            log.severe("Failed to probe resolution: " + mediaFile + " | " + e);
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
    private static Logger log = LoggerFactory.initLogger(InputStreamConsumer.class);

    public InputStreamConsumer(InputStream inputStream, String type) {
        this.inputStream = inputStream;
        this.type = type;
    }

    public void run() {
        try (BufferedReader br = new BufferedReader(new InputStreamReader(inputStream))) {
            String line;
            while ((line = br.readLine()) != null) {
                output.append(line).append("\n");
                log.info(type + "> " + line);
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public String getOutput() {
        return output.toString();
    }
}
