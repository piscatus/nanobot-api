package com.nanobot.nanobotbackend;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

/**
 * Unit tests for NanobotBackendApplication.
 * Full context load (contextLoads) is disabled as it requires MongoDB.
 */
class NanobotBackendApplicationTests {

  @Test
  void applicationClassHasRequiredAnnotations() {
    Class<?> appClass = NanobotBackendApplication.class;

    assertNotNull(appClass.getAnnotation(SpringBootApplication.class));
    assertNotNull(appClass.getAnnotation(EnableMongoRepositories.class));
    assertNotNull(appClass.getAnnotation(EnableRetry.class));
    assertNotNull(appClass.getAnnotation(EnableScheduling.class));
    assertNotNull(appClass.getAnnotation(EnableTransactionManagement.class));
  }

  @Test
  void applicationHasMainMethod() {
    assertDoesNotThrow(() -> {
      var main = NanobotBackendApplication.class.getMethod("main", String[].class);
      assertNotNull(main);
    });
  }
}
