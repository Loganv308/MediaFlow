package com.loganv308;

import java.nio.file.Path;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.logging.Logger;
import java.time.format.DateTimeFormatter;

import io.github.cdimascio.dotenv.Dotenv;

public class Utils {
    // Initialize custom LoggerFactory
    private static Logger log = LoggerFactory.initLogger(Utils.class);

    private static final long DEFAULT_SCAN_INTERVAL_MINUTES = 10;

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

    // Loads application configuration from .env, falling back to OS-detected
    // defaults for anything not set.
    public AppConfig loadConfig() {
        Dotenv dotenv = Dotenv.configure().ignoreIfMissing().load();

        PathConfig pathConfig = buildPathConfig(dotenv);
        long scanIntervalMillis = parseScanIntervalMillis(dotenv);

        return new AppConfig(pathConfig, scanIntervalMillis);
    }

    // Configures the paths based on OS, with .env overrides layered on top.
    private PathConfig buildPathConfig(Dotenv dotenv) {
        String os = System.getProperty("os.name").toLowerCase();

        boolean isWindows = os.contains("win");
        boolean isUnixLike = os.contains("nix") || os.contains("nux") || os.contains("mac");

        if (!isWindows && !isUnixLike) {
            log.severe("Unsupported OS: " + os);
            throw new UnsupportedOperationException("Unsupported OS: " + os);
        }

        String defaultNasRoot = isWindows ? "W:\\TestData" : "/mnt/NASMedia/TestData";
        String defaultTempDir = isWindows ? "C:\\Temp\\nas\\" : "/tmp/nas/";
        String defaultFfmpeg  = isWindows ? ".\\ffmpeg\\ffmpeg.exe" : "ffmpeg";
        String defaultFfprobe = isWindows ? ".\\ffmpeg\\ffprobe.exe" : "ffprobe";

        return new PathConfig(
            Path.of(dotenv.get("NAS_ROOT", defaultNasRoot)),
            Path.of(dotenv.get("TEMP_DIR", defaultTempDir)),
            dotenv.get("FFMPEG_BIN", defaultFfmpeg),
            dotenv.get("FFPROBE_BIN", defaultFfprobe),
            isWindows
        );
    }

    private long parseScanIntervalMillis(Dotenv dotenv) {
        String raw = dotenv.get("SCAN_INTERVAL_MINUTES", String.valueOf(DEFAULT_SCAN_INTERVAL_MINUTES));
        try {
            return Long.parseLong(raw.trim()) * 60_000L;
        } catch (NumberFormatException e) {
            log.warning("Invalid SCAN_INTERVAL_MINUTES='" + raw + "', defaulting to "
                + DEFAULT_SCAN_INTERVAL_MINUTES + " minutes");
            return DEFAULT_SCAN_INTERVAL_MINUTES * 60_000L;
        }
    }
}
