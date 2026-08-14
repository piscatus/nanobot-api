package com.nanobot.nanobotbackend.task;

import java.io.*;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.atomic.AtomicLong;

public class FileLogger {

  private String logFilePath;
  private String logFileName;
  private String logFileExtension;
  private AtomicLong fileSizeThreshold;
  private long retentionPeriodMillis = 30L * 24 * 60 * 60 * 1000; // 30 days

  public FileLogger(String logFileName) {
    this("log", logFileName);
  }

  /**
   * Package-private constructor for testing with a custom base path.
   */
  FileLogger(String basePath, String logFileName) {
    this.logFilePath = basePath + "/" + logFileName + "/";
    logFileName = logFileName + ".log";
    int dotIndex = logFileName.lastIndexOf('.');
    if (dotIndex > 0) {
      this.logFileName = logFileName.substring(0, dotIndex);
      this.logFileExtension = logFileName.substring(dotIndex);
    } else {
      this.logFileName = logFileName;
      this.logFileExtension = "";
    }
    this.fileSizeThreshold = new AtomicLong(10_000_000); // 10 megabytes
    checkAndRotateLogFileOnStartup();
  }

  public synchronized void error(String message) {
    log("ERROR", message);
  }

  public synchronized void info(String message) {
    log("INFO", message);
  }

  public synchronized void warn(String message) {
    log("WARN", message);
  }

  private void log(String level, String message) {
    try (
      PrintWriter writer = new PrintWriter(
        new FileWriter(getCurrentLogFilePath(), true)
      )
    ) {
      String timestamp = java.time.LocalDateTime.now().toString();
      writer.println(timestamp + " " + level + " | " + message);
      writer.flush();

      // Check and perform rotation after writing each log entry
      if (
        new File(getCurrentLogFilePath()).length() > fileSizeThreshold.get()
      ) {
        rotateLogFile();
      }
    } catch (IOException e) {
      // If the log file doesn't exist, create it and retry
      if (!new File(getCurrentLogFilePath()).exists()) {
        createLogFile();
        log(level, message); // Retry logging the message
      } else {
        e.printStackTrace();
      }
    }
  }

  private String getCurrentLogFilePath() {
    return logFilePath + logFileName + logFileExtension;
  }

  private void checkAndRotateLogFileOnStartup() {
    if (new File(getCurrentLogFilePath()).length() > fileSizeThreshold.get()) {
      rotateLogFile();
    }
  }

  private void deleteOldLogFiles() {
    File dir = new File(logFilePath);
    if (!dir.exists() || !dir.isDirectory()) return;

    File[] files = dir.listFiles((d, name) -> name.startsWith(logFileName + ".")
    );
    if (files == null) return;

    long now = System.currentTimeMillis();
    for (File file : files) {
      if (now - file.lastModified() > retentionPeriodMillis) {
        if (!file.delete()) {
          System.err.println(
            "Failed to delete old log file: " + file.getName()
          );
        }
      }
    }
  }

  private void rotateLogFile() {
    String timestamp = new SimpleDateFormat("yyyyMMddHHmmss").format(
      new Date()
    );
    String rotatedFilePath =
      logFilePath + logFileName + "." + timestamp + logFileExtension;

    File currentLogFile = new File(getCurrentLogFilePath());
    File rotatedLogFile = new File(rotatedFilePath);

    if (currentLogFile.renameTo(rotatedLogFile)) {
      createLogFile();
      deleteOldLogFiles(); // <--- delete old logs after rotation
    } else {
      System.err.println("Failed to rotate log file.");
    }
  }

  private void createLogFile() {
    try {
      File logFile = new File(getCurrentLogFilePath());
      if (logFile.getParentFile() != null) {
        logFile.getParentFile().mkdirs(); // Create parent directories if they don't exist
      }
      logFile.createNewFile(); // Create the log file
    } catch (IOException e) {
      e.printStackTrace();
    }
  }
}
