package com.nanobot.nanobotbackend;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.data.mongodb.repository.config.EnableMongoRepositories;
import org.springframework.retry.annotation.EnableRetry;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.transaction.annotation.EnableTransactionManagement;

@EnableMongoRepositories(basePackages = "com.nanobot.nanobotbackend.repository")
@EnableRetry
@EnableScheduling
@EnableTransactionManagement
@SpringBootApplication(scanBasePackages = "com.nanobot.nanobotbackend")
public class NanobotBackendApplication {

  public static void main(String[] args) {
    SpringApplication.run(NanobotBackendApplication.class, args);
  }
}
