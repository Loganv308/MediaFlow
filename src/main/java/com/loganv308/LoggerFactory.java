package com.loganv308;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Logger Factory class
public class LoggerFactory {

    // Used throughout the class.
    private static final Logger LOGGER = Logger.getLogger(LoggerFactory.class.getName());

    private static String DEFAULT_LOG_FILE = "runner.log";
    
    // Initialize logger method
    public static Logger initLogger() {
        // StackWalker walks the call stack and finds the caller's class name
        String callerClassName = StackWalker
                .getInstance(StackWalker.Option.RETAIN_CLASS_REFERENCE)
                .walk(frames -> frames
                        .skip(1) // Skip initLogger itself
                        .findFirst()
                        .map(f -> f.getClassName())
                        .orElse("UnknownClass"));

        return buildLogger(callerClassName, callerClassName + ".log");
    }

    // Builds and configures the logger with a FileHandler
    private static Logger buildLogger(String loggerName, String logFileName) {
        Logger logger = Logger.getLogger(loggerName);

        // Avoid adding duplicate handlers if logger was already configured
        if (logger.getHandlers().length > 0) {
            return logger;
        }
        // Suppress console output from root logger
        logger.setUseParentHandlers(false); 

        try {
            // true = append mode
            FileHandler fileHandler = new FileHandler(logFileName, true);
            fileHandler.setFormatter(new SimpleFormatter());
            logger.addHandler(fileHandler);
        } catch (IOException e) {
            Logger.getLogger(LoggerFactory.class.getName())
                .severe("Failed to create log file: " + logFileName + " — " + e.getMessage());
        }

        return logger;
    }
    
    // ERROR logging, used for anything error related.
    public static String logError(String errorMsg) {
        LOGGER.severe(errorMsg);

        return "ERROR: " + errorMsg;
    }

    // INFO logging, most commonly used for basic logs.
    public static String logInfo(String logMsg) {
        LOGGER.info(logMsg);

        return "INFO: " + logMsg;
    }
}
