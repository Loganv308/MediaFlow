package com.loganv308;

import java.io.IOException;
import java.util.logging.FileHandler;
import java.util.logging.Logger;
import java.util.logging.SimpleFormatter;

// Logger Factory class
public class LoggerFactory {

    private static String DEFAULT_LOG_FILE = "runner.log";
    
    // Initialize logger method
    public static Logger initLogger(Class<?> className) {
        Logger logger = Logger.getLogger(className.getName());

        if (logger.getHandlers().length == 0) {
            logger.setUseParentHandlers(false);

            try {
                FileHandler fileHandler = new FileHandler("Logs/" + className.getSimpleName() + ".log", true);
                fileHandler.setFormatter(new SimpleFormatter());
                logger.addHandler(fileHandler);
            } catch (IOException e) {
                e.printStackTrace();
            }
        }

        return buildLogger(className.getName(), "Logs/" + className.getSimpleName() + ".log");
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
}
