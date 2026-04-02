package com.loganv308;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.time.format.DateTimeFormatter;

public class Utils {
    // Initialize custom LoggerFactory 
    private static Logger log = LoggerFactory.initLogger(Utils.class);

    // Retrieves todays date
    public LocalDate getDate() {
        LocalDate today = LocalDate.now();

        // Returns todays date
        return today;
    }

    public String getFormattedTime() {
        // Will format time like so: "01:26:52 pm"
        DateTimeFormatter dtf = DateTimeFormatter.ofPattern("HH:mm:ss a");

        // Returns formatted time
        return LocalDateTime.now().format(dtf);
    }

    // Configures the paths based on OS, used in many other classes throughout this program. 
    public PathConfig configurePaths() {
        String os = System.getProperty("os.name").toLowerCase();

        if (os.contains("win")) {
            return new PathConfig(
                Path.of("Y:\\TestData\\Test - IOExceptions"),
                Path.of("C:\\Temp\\nascopiestest\\"),
                ".\\ffmpeg\\ffmpeg.exe",
                ".\\ffmpeg\\ffprobe.exe",
                true
            );
        } else if (os.contains("nix") || os.contains("nux") || os.contains("mac")) {
            return new PathConfig(
                Path.of("/mnt/NASMedia/TestData/Test - IOExceptions"),
                Path.of("/tmp/nascopiestest/"),
                "ffmpeg",
                "ffprobe",
                false
            );
        } else {
            log.severe("Unsupported OS: " + os);
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }
    }
}
