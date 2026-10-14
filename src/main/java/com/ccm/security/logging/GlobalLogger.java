package com.ccm.security.logging;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * GlobalLogger - Centralized logger to write messages to global log file.
 */
@Component
public class GlobalLogger {

    private static final Logger logger = LoggerFactory.getLogger("com.ccm.security.logging");

    public void info(String message) {
        logger.info(message);
    }

    public void debug(String message) {
        logger.debug(message);
    }

    public void warn(String message) {
        logger.warn(message);
    }

    public void error(String message) {
        logger.error(message);
    }

    public void error(String message, Throwable throwable) {
        logger.error(message, throwable);
    }

    public void trace(String message) {
        logger.trace(message);
    }
}


