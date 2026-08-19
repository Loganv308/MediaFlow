package com.loganv308;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.file.Path;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Set;
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

        List<String> command = new ArrayList<>(List.of(
            ffmpegBin,
            "-i", filePath,
            "-c:v", "hevc_nvenc",
            "-preset", "p5",
            "-cq", "28",
            "-rc", "vbr",
            "-filter_complex", "[0:v]scale='min(1920,iw)':'min(1080,ih)':force_original_aspect_ratio=decrease[outv]",
            "-map", "[outv]"
        ));

        // Map only audio streams that actually have valid parameters. Some source files
        // (particularly .m2ts remuxes) contain a malformed extra audio stream — e.g. an
        // AC-3 track reporting 0 channels/no sample rate — and blindly copying every audio
        // stream via "0:a?" makes ffmpeg fail to write a header for that one bad stream,
        // which aborts the entire encode (the whole file, not just that track).
        List<String> validAudioStreams = getValidAudioStreamIndexes(filePath);
        if (validAudioStreams.isEmpty()) {
            log.warning("No valid audio streams found for " + filePath + " — output will have no audio.");
        } else {
            for (String streamIndex : validAudioStreams) {
                command.add("-map");
                command.add("0:" + streamIndex);
            }
            command.add("-c:a");
            command.add("copy");
        }

        if (needsHvc1Tag) {
            command.add("-tag:v");
            command.add("hvc1");
        }

        command.add(formattedOutputString);

        return new ProcessBuilder(command);
    }

    // Returns the absolute stream indexes (e.g. "3") of audio streams with a valid,
    // non-zero channel count. Falls back to mapping all audio streams ("0:a?" via an
    // empty list being treated the same as "couldn't determine") only if the probe
    // itself fails — a probe failure shouldn't silently drop every audio track.
    // Deduplicated: some transport-stream files (e.g. .m2ts with multiple programs)
    // have ffprobe list the same stream more than once.
    private List<String> getValidAudioStreamIndexes(String filePath) {
        Set<String> valid = new LinkedHashSet<>();
        try {
            ProcessBuilder pb = new ProcessBuilder(
                ffprobeBin,
                "-v", "error",
                "-select_streams", "a",
                "-show_entries", "stream=index,channels",
                "-of", "csv=p=0",
                filePath
            );

            Process p = pb.start();
            InputStreamConsumer outputConsumer = new InputStreamConsumer(p.getInputStream(), "OUTPUT");
            Thread outputThread = new Thread(outputConsumer);
            outputThread.start();

            InputStreamConsumer errorConsumer = new InputStreamConsumer(p.getErrorStream(), "ERROR");
            Thread errorThread = new Thread(errorConsumer);
            errorThread.start();

            int exitCode = p.waitFor();
            errorThread.join();
            outputThread.join();

            if (exitCode != 0) {
                log.warning("Could not probe audio streams for " + filePath
                    + ", falling back to mapping all audio streams: " + errorConsumer.getOutput().trim());
                valid.add("a?");
                return new ArrayList<>(valid);
            }

            for (String line : outputConsumer.getOutput().trim().split("\\R")) {
                if (line.isBlank()) {
                    continue;
                }
                String[] parts = line.split(",");
                if (parts.length != 2) {
                    continue;
                }
                try {
                    String streamIndex = parts[0].trim();
                    int channels = Integer.parseInt(parts[1].trim());
                    if (channels > 0) {
                        valid.add(streamIndex);
                    } else {
                        log.warning("Skipping malformed audio stream " + streamIndex
                            + " (0 channels) in " + filePath);
                    }
                } catch (NumberFormatException e) {
                    log.warning("Could not parse audio stream info line '" + line + "' for " + filePath);
                }
            }
        } catch (IOException | InterruptedException e) {
            if (e instanceof InterruptedException) {
                Thread.currentThread().interrupt();
            }
            log.warning("Failed to probe audio streams for " + filePath
                + ", falling back to mapping all audio streams: " + e);
            valid.add("a?");
        }
        return new ArrayList<>(valid);
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
