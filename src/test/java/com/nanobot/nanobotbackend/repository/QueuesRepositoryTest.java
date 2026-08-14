package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class QueuesRepositoryTest {

  @Autowired
  private QueuesRepository queuesRepository;

  @BeforeEach
  void setUp() {
    queuesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoQueues() {
    List<QueueEntity> result = queuesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedQueue() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    queue.setTimestamp(new Date());
    queue.setProcessed(false);

    QueueEntity saved = queuesRepository.insert(queue);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<QueueEntity> all = queuesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
    assertEquals("XNO", all.get(0).getTicker());
  }

  @Test
  void findAllSortedByOldestTimestampReturnsOrderedByTimestampAsc() {
    QueueEntity q1 = new QueueEntity();
    q1.setUserId("u1");
    q1.setTicker("XNO");
    q1.setTimestamp(new Date(3000));
    queuesRepository.insert(q1);

    QueueEntity q2 = new QueueEntity();
    q2.setUserId("u2");
    q2.setTicker("XNO");
    q2.setTimestamp(new Date(1000));
    queuesRepository.insert(q2);

    QueueEntity q3 = new QueueEntity();
    q3.setUserId("u3");
    q3.setTicker("XNO");
    q3.setTimestamp(new Date(2000));
    queuesRepository.insert(q3);

    List<QueueEntity> result =
      queuesRepository.findAllSortedByOldestTimestamp();
    assertEquals(3, result.size());
    assertEquals(1000L, result.get(0).getTimestamp().getTime());
    assertEquals(2000L, result.get(1).getTimestamp().getTime());
    assertEquals(3000L, result.get(2).getTimestamp().getTime());
  }

  @Test
  void findByParamsReturnsMatchingQueues() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    queue.setTimestamp(new Date());
    queuesRepository.insert(queue);

    List<QueueEntity> result = queuesRepository.findByParams("u1", "XNO");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getUserId());
    assertEquals("XNO", result.get(0).getTicker());
  }

  @Test
  void findByParamsWithNullUserIdReturnsAllForTicker() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    queuesRepository.insert(queue);

    List<QueueEntity> result = queuesRepository.findByParams(null, "XNO");
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    QueueEntity saved = queuesRepository.insert(queue);

    var found = queuesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("XNO", found.get().getTicker());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = queuesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    queue.setProcessed(false);
    QueueEntity saved = queuesRepository.insert(queue);

    saved.setProcessed(true);
    QueueEntity updated = queuesRepository.save(saved);

    assertTrue(updated.getProcessed());
  }

  @Test
  void deleteByIdRemovesEntity() {
    QueueEntity queue = new QueueEntity();
    queue.setUserId("u1");
    queue.setTicker("XNO");
    QueueEntity saved = queuesRepository.insert(queue);

    queuesRepository.deleteById(saved.getId());

    var found = queuesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
