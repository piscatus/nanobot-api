package com.nanobot.nanobotbackend.task;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import com.nanobot.nanobotbackend.service.DropService;
import com.nanobot.nanobotbackend.service.NodesService;
import com.nanobot.nanobotbackend.service.PriceService;
import com.nanobot.nanobotbackend.service.TransactionsService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class CronJobsTest {

  @Mock
  private DropService dropService;

  @Mock
  private NodesService nodesService;

  @Mock
  private PriceService priceService;

  @Mock
  private TransactionsService transactionsService;

  private CronJobs cronJobs;

  @BeforeEach
  void setUp() {
    cronJobs =
      new CronJobs(dropService, priceService, nodesService, transactionsService);
  }

  @Test
  void checkPricesDelegatesToPriceService() {
    cronJobs.checkPrices();
    verify(priceService).checkPrices();
  }

  @Test
  void checkDropsByPickupSizeDelegatesToDropService() {
    cronJobs.checkDropsByPickupSize();
    verify(dropService).checkDropsByPickupSize();
  }

  @Test
  void checkActivityDelegatesToNodesService() {
    cronJobs.checkActivity();
    verify(nodesService).checkActivity();
  }

  @Test
  void checkDropsByTimeDelegatesToDropService() {
    cronJobs.checkDropsByTime();
    verify(dropService).checkDropsByTime();
  }

  @Test
  void deleteOldTransactionsDelegatesToTransactionsService() {
    cronJobs.deleteOldTransactions();
    verify(transactionsService).deleteTransactionsOlderThan30Days();
  }

  @Test
  void checkPricesPropagatesExceptionWhenServiceThrows() {
    doThrow(new RuntimeException("Price service unavailable"))
      .when(priceService)
      .checkPrices();

    assertThrows(RuntimeException.class, () -> cronJobs.checkPrices());
    verify(priceService).checkPrices();
  }

  @Test
  void checkActivityPropagatesExceptionWhenServiceThrows() {
    doThrow(new RuntimeException("Nodes service unavailable"))
      .when(nodesService)
      .checkActivity();

    assertThrows(RuntimeException.class, () -> cronJobs.checkActivity());
    verify(nodesService).checkActivity();
  }

  @Test
  void checkDropsByPickupSizePropagatesExceptionWhenServiceThrows() {
    doThrow(new RuntimeException("Drop service unavailable"))
      .when(dropService)
      .checkDropsByPickupSize();

    assertThrows(RuntimeException.class, () -> cronJobs.checkDropsByPickupSize());
    verify(dropService).checkDropsByPickupSize();
  }

  @Test
  void checkDropsByTimePropagatesExceptionWhenServiceThrows() {
    doThrow(new RuntimeException("Drop service unavailable"))
      .when(dropService)
      .checkDropsByTime();

    assertThrows(RuntimeException.class, () -> cronJobs.checkDropsByTime());
    verify(dropService).checkDropsByTime();
  }

  @Test
  void deleteOldTransactionsDoesNotPropagateWhenServiceThrows() {
    doThrow(new RuntimeException("DB connection failed"))
      .when(transactionsService)
      .deleteTransactionsOlderThan30Days();

    cronJobs.deleteOldTransactions();

    verify(transactionsService).deleteTransactionsOlderThan30Days();
  }
}
