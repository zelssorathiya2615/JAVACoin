package com.javacoin.util;

import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Logger - Simple file-based logging utility
 */
public class Logger {
    
    private String logFile;
    private boolean consoleOutput;
    private SimpleDateFormat dateFormat;
    
    /**
     * Creates a logger
     * @param logFile Path to log file
     * @param consoleOutput Whether to also print to console
     */
    public Logger(String logFile, boolean consoleOutput) {
        this.logFile = logFile;
        this.consoleOutput = consoleOutput;
        this.dateFormat = new SimpleDateFormat("yyyy-MM-dd HH:mm:ss");
    }
    
    /**
     * Logs an info message
     * @param message Message to log
     */
    public void info(String message) {
        log("INFO", message);
    }
    
    /**
     * Logs a warning message
     * @param message Message to log
     */
    public void warn(String message) {
        log("WARN", message);
    }
    
    /**
     * Logs an error message
     * @param message Message to log
     */
    public void error(String message) {
        log("ERROR", message);
    }
    
    /**
     * Logs a debug message
     * @param message Message to log
     */
    public void debug(String message) {
        if (Constants.ENABLE_DEBUG_LOGGING) {
            log("DEBUG", message);
        }
    }
    
    /**
     * Internal logging method
     * @param level Log level
     * @param message Message
     */
    private void log(String level, String message) {
        String timestamp = dateFormat.format(new Date());
        String logEntry = String.format("[%s] [%s] %s", timestamp, level, message);
        
        // Console output
        if (consoleOutput) {
            System.out.println(logEntry);
        }
        
        // File output
        try (PrintWriter writer = new PrintWriter(new FileWriter(logFile, true))) {
            writer.println(logEntry);
        } catch (IOException e) {
            System.err.println("Failed to write to log file: " + e.getMessage());
        }
    }
    
    /**
     * Creates a logger for a specific node
     * @param nodeId Node identifier
     * @return Logger instance
     */
    public static Logger forNode(String nodeId) {
        String logFile = "logs/node-" + nodeId + ".log";
        return new Logger(logFile, true);
    }
}


// Copyright (c) 2025 Zels Sorathiya. All rights reserved.
// Unauthorized copying of this file, via any medium is strictly prohibited.