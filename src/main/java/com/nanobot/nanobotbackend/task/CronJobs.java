package com.nanobot.nanobotbackend.task;

import com.nanobot.nanobotbackend.service.DropService;
import com.nanobot.nanobotbackend.service.NodesService;
import com.nanobot.nanobotbackend.service.PriceService;
import com.nanobot.nanobotbackend.service.TransactionsService;
import java.util.Date;
import java.util.concurrent.locks.ReentrantLock;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Component
public class CronJobs {

  private final DropService dropService;
  private final NodesService nodesService;
  private final PriceService priceService;
  private final TransactionsService transactionsService;
  private final FileLogger fileLogger;

  // Lock to prevent overlap between checkDrops and runEveryMinuteJob
  private final ReentrantLock dropsLock = new ReentrantLock();

  @Autowired
  public CronJobs(
    DropService dropService,
    PriceService priceService,
    NodesService nodesService,
    TransactionsService transactionsService
  ) {
    this.dropService = dropService;
    this.nodesService = nodesService;
    this.priceService = priceService;
    this.transactionsService = transactionsService;
    this.fileLogger = new FileLogger("CronJobs");
  }

  // Runs every 5 minutes
  @Scheduled(fixedRate = 300000)
  public void checkPrices() {
    priceService.checkPrices();
  }

  // New Job: Runs every 1 minute
  @Scheduled(cron = "0 * * * * *") // Every minute on the 0th second
  public void checkDropsByPickupSize() {
    if (dropsLock.tryLock()) {
      try {
        dropService.checkDropsByPickupSize();
      } finally {
        dropsLock.unlock();
      }
    } else {
      fileLogger.info(
        "Cron Jobs : Skipped checkDropsByPickupSize() due to lock."
      );
    }
  }

  @Scheduled(fixedDelay = 1000)
  public void checkActivity() {
    nodesService.checkActivity();
  }

  @Scheduled(fixedDelay = 1000)
  public void checkDropsByTime() {
    if (dropsLock.tryLock()) {
      try {
        dropService.checkDropsByTime();
      } finally {
        dropsLock.unlock();
      }
    } else {
      fileLogger.info("Cron Jobs : Skipped checkDropsByTime() due to lock.");
    }
  }

  // Runs daily at 00:20 UTC = 7:20 PM EST
  @Scheduled(cron = "0 20 0 * * ?", zone = "UTC")
  public void deleteOldTransactions() {
    try {
      fileLogger.info("Starting cleanup of transactions older than 30 days.");
      transactionsService.deleteTransactionsOlderThan30Days();
      fileLogger.info("Finished cleanup of old transactions.");
    } catch (Exception e) {
      fileLogger.error(
        "Error during daily transaction cleanup: " + e.getMessage()
      );
    }
  }
}
