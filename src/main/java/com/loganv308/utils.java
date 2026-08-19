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
        DbConfig dbConfig = buildDbConfig(dotenv);
        long scanIntervalMillis = parseScanIntervalMillis(dotenv);

        return new AppConfig(pathConfig, dbConfig, scanIntervalMillis);
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

        return new PathConfig(
            Path.of(dotenv.get("NAS_ROOT")),
            Path.of(dotenv.get("TEMP_DIR")),
            dotenv.get("FFMPEG_BIN"),
            dotenv.get("FFPROBE_BIN"),
            isWindows
        );
    }

    private DbConfig buildDbConfig(Dotenv dotenv) {
        return new DbConfig(
            dotenv.get("DB_HOST"),
            Integer.parseInt(dotenv.get("DB_PORT")),
            dotenv.get("DB_NAME"),
            dotenv.get("DB_USER"),
            dotenv.get("DB_PASSWORD")
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
