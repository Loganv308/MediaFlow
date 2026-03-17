package com.loganv308;

import java.util.logging.Logger;

// Logger Factory class
public class LoggerFactory {
    
    public static Logger initLogger() {
        Logger LOGGER = Logger.getLogger(LoggerFactory.class.getName());

        return LOGGER;
    }

}
