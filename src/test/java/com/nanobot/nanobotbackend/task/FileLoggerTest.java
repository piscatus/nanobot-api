package com.nanobot.nanobotbackend.task;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

class FileLoggerTest {

  @TempDir
  Path tempDir;

  private String logBasePath;

  @BeforeEach
  void setUp() {
    logBasePath = tempDir.toAbsolutePath().toString();
  }

  @Test
  void infoWritesToLogFile() throws IOException {
    String logName = "test-info";
    FileLogger logger = new FileLogger(logBasePath, logName);

    assertDoesNotThrow(() -> logger.info("Test info message"));

    Path logFile = tempDir.resolve(logName).resolve(logName + ".log");
    assertTrue(Files.exists(logFile), "Log file should exist");
    String content = Files.readString(logFile);
    assertTrue(content.contains("INFO"), "Content should contain INFO level");
    assertTrue(content.contains("Test info message"), "Content should contain message");
  }

  @Test
  void errorWritesToLogFile() throws IOException {
    String logName = "test-error";
    FileLogger logger = new FileLogger(logBasePath, logName);

    assertDoesNotThrow(() -> logger.error("Test error message"));

    Path logFile = tempDir.resolve(logName).resolve(logName + ".log");
    assertTrue(Files.exists(logFile), "Log file should exist");
    String content = Files.readString(logFile);
    assertTrue(content.contains("ERROR"), "Content should contain ERROR level");
    assertTrue(content.contains("Test error message"), "Content should contain message");
  }

  @Test
  void warnWritesToLogFile() throws IOException {
    String logName = "test-warn";
    FileLogger logger = new FileLogger(logBasePath, logName);

    assertDoesNotThrow(() -> logger.warn("Test warn message"));

    Path logFile = tempDir.resolve(logName).resolve(logName + ".log");
    assertTrue(Files.exists(logFile), "Log file should exist");
    String content = Files.readString(logFile);
    assertTrue(content.contains("WARN"), "Content should contain WARN level");
    assertTrue(content.contains("Test warn message"), "Content should contain message");
  }

  @Test
  void multipleLogCallsAppendToFile() throws IOException {
    String logName = "test-append";
    FileLogger logger = new FileLogger(logBasePath, logName);

    logger.info("First message");
    logger.info("Second message");

    Path logFile = tempDir.resolve(logName).resolve(logName + ".log");
    String content = Files.readString(logFile);
    assertTrue(content.contains("First message"), "Should contain first message");
    assertTrue(content.contains("Second message"), "Should contain second message");
  }

  @Test
  void publicConstructorCreatesLoggerWithDefaultPath() {
    assertDoesNotThrow(() -> {
      FileLogger logger = new FileLogger("test-default-path");
      logger.info("Smoke test");
    });
  }
}
