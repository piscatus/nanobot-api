package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.TransactionEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class TransactionsRepositoryTest {

  @Autowired
  private TransactionsRepository transactionsRepository;

  @BeforeEach
  void setUp() {
    transactionsRepository.deleteAll();
  }

  @Test
  void findAllByOrderByTimestampDescReturnsEmptyWhenNoTransactions() {
    List<TransactionEntity> result = transactionsRepository.findAllByOrderByTimestampDesc();
    assertTrue(result.isEmpty());
  }

  @Test
  void findAllByOrderByTimestampDescReturnsOrderedByTimestampDesc() {
    TransactionEntity t1 = new TransactionEntity();
    t1.setPrimaryUserId("u1");
    t1.setTimestamp(new Date(1000));
    transactionsRepository.insert(t1);

    TransactionEntity t2 = new TransactionEntity();
    t2.setPrimaryUserId("u2");
    t2.setTimestamp(new Date(3000));
    transactionsRepository.insert(t2);

    TransactionEntity t3 = new TransactionEntity();
    t3.setPrimaryUserId("u3");
    t3.setTimestamp(new Date(2000));
    transactionsRepository.insert(t3);

    List<TransactionEntity> result = transactionsRepository.findAllByOrderByTimestampDesc();
    assertEquals(3, result.size());
    assertEquals(3000L, result.get(0).getTimestamp().getTime());
    assertEquals(2000L, result.get(1).getTimestamp().getTime());
    assertEquals(1000L, result.get(2).getTimestamp().getTime());
  }

  @Test
  void findByAnyUserIdReturnsByPrimaryUserId() {
    TransactionEntity t = new TransactionEntity();
    t.setPrimaryUserId("u1");
    t.setTimestamp(new Date());
    transactionsRepository.insert(t);

    List<TransactionEntity> result = transactionsRepository.findByAnyUserId("u1");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getPrimaryUserId());
  }

  @Test
  void findByAnyUserIdReturnsBySecondaryUserId() {
    TransactionEntity t = new TransactionEntity();
    t.setPrimaryUserId("u0");
    t.setSecondaryUserId("u1");
    t.setTimestamp(new Date());
    transactionsRepository.insert(t);

    List<TransactionEntity> result = transactionsRepository.findByAnyUserId("u1");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getSecondaryUserId());
  }

  @Test
  void findByAnyUserIdReturnsEmptyWhenNoMatch() {
    TransactionEntity t = new TransactionEntity();
    t.setPrimaryUserId("u1");
    t.setTimestamp(new Date());
    transactionsRepository.insert(t);

    List<TransactionEntity> result = transactionsRepository.findByAnyUserId("u99");
    assertTrue(result.isEmpty());
  }

  @Test
  void deleteByTimestampBeforeRemovesOlderTransactions() {
    TransactionEntity old = new TransactionEntity();
    old.setPrimaryUserId("u1");
    old.setTimestamp(new Date(1000));
    transactionsRepository.insert(old);

    TransactionEntity recent = new TransactionEntity();
    recent.setPrimaryUserId("u2");
    recent.setTimestamp(new Date(5000));
    transactionsRepository.insert(recent);

    long deleted = transactionsRepository.deleteByTimestampBefore(new Date(3000));
    assertEquals(1, deleted);

    List<TransactionEntity> remaining = transactionsRepository.findAll();
    assertEquals(1, remaining.size());
    assertEquals("u2", remaining.get(0).getPrimaryUserId());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    TransactionEntity t = new TransactionEntity();
    t.setPrimaryUserId("u1");
    t.setTimestamp(new Date());
    TransactionEntity saved = transactionsRepository.insert(t);

    var found = transactionsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getPrimaryUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = transactionsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }
}
