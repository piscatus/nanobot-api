package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.MessageEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class MessagesRepositoryTest {

  @Autowired
  private MessagesRepository messagesRepository;

  @BeforeEach
  void setUp() {
    messagesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoMessages() {
    List<MessageEntity> result = messagesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedMessage() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    message.setTimestamp(new Date());

    MessageEntity saved = messagesRepository.insert(message);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<MessageEntity> all = messagesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
    assertEquals("msg1", all.get(0).getMessageId());
  }

  @Test
  void findAllSortedByOldestTimestampReturnsOrderedByTimestampAsc() {
    MessageEntity m1 = new MessageEntity();
    m1.setUserId("u1");
    m1.setTimestamp(new Date(3000));
    messagesRepository.insert(m1);

    MessageEntity m2 = new MessageEntity();
    m2.setUserId("u2");
    m2.setTimestamp(new Date(1000));
    messagesRepository.insert(m2);

    MessageEntity m3 = new MessageEntity();
    m3.setUserId("u3");
    m3.setTimestamp(new Date(2000));
    messagesRepository.insert(m3);

    List<MessageEntity> result =
      messagesRepository.findAllSortedByOldestTimestamp();
    assertEquals(3, result.size());
    assertEquals(1000L, result.get(0).getTimestamp().getTime());
    assertEquals(2000L, result.get(1).getTimestamp().getTime());
    assertEquals(3000L, result.get(2).getTimestamp().getTime());
  }

  @Test
  void findByParamsReturnsMatchingMessages() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    messagesRepository.insert(message);

    List<MessageEntity> result =
      messagesRepository.findByParams("u1", "msg1");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getUserId());
    assertEquals("msg1", result.get(0).getMessageId());
  }

  @Test
  void findByParamsWithNullUserIdReturnsAllForMessageId() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    messagesRepository.insert(message);

    List<MessageEntity> result = messagesRepository.findByParams(null, "msg1");
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    MessageEntity saved = messagesRepository.insert(message);

    var found = messagesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("msg1", found.get().getMessageId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = messagesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    message.setContent("original");
    MessageEntity saved = messagesRepository.insert(message);

    saved.setContent("updated");
    MessageEntity updated = messagesRepository.save(saved);

    assertEquals("updated", updated.getContent());
  }

  @Test
  void deleteByIdRemovesEntity() {
    MessageEntity message = new MessageEntity();
    message.setUserId("u1");
    message.setMessageId("msg1");
    MessageEntity saved = messagesRepository.insert(message);

    messagesRepository.deleteById(saved.getId());

    var found = messagesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
