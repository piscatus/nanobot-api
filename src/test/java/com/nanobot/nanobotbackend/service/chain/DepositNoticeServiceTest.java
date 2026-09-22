package com.nanobot.nanobotbackend.service.chain;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.entity.DepositNoticeEntity;
import com.nanobot.nanobotbackend.repository.DepositNoticesRepository;
import java.util.Date;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.data.mongodb.core.MongoTemplate;

class DepositNoticeServiceTest {

  private DepositNoticesRepository repository;
  private DepositNoticeService service;

  @BeforeEach
  void setUp() {
    repository = mock(DepositNoticesRepository.class);
    service = new DepositNoticeService(repository, mock(MongoTemplate.class));
  }

  @Test
  void firstSightingIsTrueOnlyOnce() {
    when(
      repository.existsByTickerAndTxidAndAddressIndex("XMR", "tx", 3L)
    )
      .thenReturn(false, true);

    assertTrue(service.recordFirstSighting("XMR", "tx", 3L, "u1", "1"));
    assertFalse(service.recordFirstSighting("XMR", "tx", 3L, "u1", "1"));
    verify(repository).insert(any(DepositNoticeEntity.class));
  }

  @Test
  void confirmSentMarksAnExistingDiscoveryOnce() {
    DepositNoticeEntity notice = new DepositNoticeEntity(
      "XMR",
      "tx",
      3L,
      "u1",
      "1"
    );
    when(
      repository.findByTickerAndTxidAndAddressIndex("XMR", "tx", 3L)
    )
      .thenReturn(Optional.of(notice));

    assertTrue(service.recordConfirmSent("XMR", "tx", 3L, "u1", "1"));
    assertFalse(service.recordConfirmSent("XMR", "tx", 3L, "u1", "1"));
    verify(repository).save(notice);
  }

  @Test
  void confirmSentInsertsWhenDiscoveryNeverRan() {
    when(
      repository.findByTickerAndTxidAndAddressIndex("XMR", "tx", 3L)
    )
      .thenReturn(Optional.empty());

    assertTrue(service.recordConfirmSent("XMR", "tx", 3L, "u1", "1"));
    verify(repository).insert(any(DepositNoticeEntity.class));
  }

  @Test
  void confirmSentDoesNotRepeatAfterARacingInsert() {
    DepositNoticeEntity notice = new DepositNoticeEntity(
      "XMR",
      "tx",
      3L,
      "u1",
      "1"
    );
    notice.setConfirmedAt(new Date());
    when(
      repository.findByTickerAndTxidAndAddressIndex("XMR", "tx", 3L)
    )
      .thenReturn(Optional.empty(), Optional.of(notice));
    when(repository.insert(any(DepositNoticeEntity.class)))
      .thenThrow(new DuplicateKeyException("dup"));

    assertFalse(service.recordConfirmSent("XMR", "tx", 3L, "u1", "1"));
    verify(repository, never()).save(any());
  }

  @Test
  void unconfirmedReturnsRowsStillOwingAConfirm() {
    DepositNoticeEntity notice = new DepositNoticeEntity(
      "XMR",
      "tx",
      3L,
      "u1",
      "1"
    );
    when(repository.findByTickerAndConfirmedAtIsNull("XMR"))
      .thenReturn(java.util.List.of(notice));

    assertEquals(1, service.unconfirmed("XMR").size());
    assertTrue(service.unconfirmed(null).isEmpty());
  }
}
