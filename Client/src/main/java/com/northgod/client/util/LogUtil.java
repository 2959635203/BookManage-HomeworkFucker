package com.northgod.client.util;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.PrintStream;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;

public class LogUtil {
    private static final PrintStream consoleOut;
    private static final PrintStream consoleErr;
    private static PrintStream fileOut;
    private static PrintStream fileErr;
    private static final DateTimeFormatter formatter =
            DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss.SSS");
    
    private static final String LOG_DIR = "logs";
    private static final String LOG_FILE = "bookstore-client.log";
    private static final String ERROR_LOG_FILE = "bookstore-client-error.log";
    
    static {
        // 初始化控制台输出流
        try {
            consoleOut = new PrintStream(System.out, true, StandardCharsets.UTF_8);
            consoleErr = new PrintStream(System.err, true, StandardCharsets.UTF_8);
        } catch (Exception e) {
            throw new RuntimeException("初始化LogUtil失败", e);
        }
        
        // 初始化文件输出流
        try {
            // 创建日志目录
            File logDir = new File(LOG_DIR);
            if (!logDir.exists()) {
                logDir.mkdirs();
            }
            
            // 创建日志文件输出流（追加模式）
            File logFile = new File(logDir, LOG_FILE);
            File errorLogFile = new File(logDir, ERROR_LOG_FILE);
            
            fileOut = new PrintStream(
                    new FileOutputStream(logFile, true), 
                    true, 
                    StandardCharsets.UTF_8);
            fileErr = new PrintStream(
                    new FileOutputStream(errorLogFile, true), 
                    true, 
                    StandardCharsets.UTF_8);
            
            consoleOut.println("[LOG] 日志文件已初始化: " + logFile.getAbsolutePath());
        } catch (IOException e) {
            consoleErr.println("[ERROR] 无法创建日志文件: " + e.getMessage());
            // 如果文件日志失败，使用控制台日志
            fileOut = consoleOut;
            fileErr = consoleErr;
        }
    }
    
    /**
     * 同时输出到控制台和文件
     */
    private static void log(String level, String message, boolean isError) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "[" + level + "] " + timestamp + " - " + message;
        
        // 输出到控制台
        if (isError) {
            consoleErr.println(logMessage);
        } else {
            consoleOut.println(logMessage);
        }
        
        // 输出到文件
        if (fileOut != null && fileErr != null) {
            try {
                if (isError) {
                    fileErr.println(logMessage);
                    fileErr.flush();
                } else {
                    fileOut.println(logMessage);
                    fileOut.flush();
                }
            } catch (Exception e) {
                // 忽略文件写入错误，避免影响程序运行
            }
        }
    }
    
    public static void info(String message) {
        log("INFO", message, false);
    }

    public static void warn(String message) {
        log("WARN", message, true);
    }

    public static void error(String message) {
        log("ERROR", message, true);
    }

    public static void error(String message, Throwable throwable) {
        String timestamp = LocalDateTime.now().format(formatter);
        String logMessage = "[ERROR] " + timestamp + " - " + message;
        
        // 输出到控制台
        consoleErr.println(logMessage);
        throwable.printStackTrace(consoleErr);
        
        // 输出到文件
        if (fileErr != null) {
            try {
                fileErr.println(logMessage);
                throwable.printStackTrace(fileErr);
                fileErr.flush();
            } catch (Exception e) {
                // 忽略文件写入错误
            }
        }
    }

    public static void debug(String message) {
        log("DEBUG", message, false);
    }

    public static void cache(String message) {
        log("CACHE", message, false);
    }

    public static void performance(String message) {
        log("PERF", message, false);
    }

    // 直接输出原始信息（无格式）
    public static void print(String message) {
        consoleOut.print(message);
        if (fileOut != null) {
            try {
                fileOut.print(message);
            } catch (Exception e) {
                // 忽略
            }
        }
    }

    public static void println(String message) {
        consoleOut.println(message);
        if (fileOut != null) {
            try {
                fileOut.println(message);
            } catch (Exception e) {
                // 忽略
            }
        }
    }
    
    /**
     * 关闭日志文件流（程序退出时调用）
     */
    public static void close() {
        try {
            if (fileOut != null && fileOut != consoleOut) {
                fileOut.close();
            }
            if (fileErr != null && fileErr != consoleErr) {
                fileErr.close();
            }
        } catch (Exception e) {
            // 忽略关闭错误
        }
    }
}
