package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.DropEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class DropsRepositoryTest {

  @Autowired
  private DropsRepository dropsRepository;

  @BeforeEach
  void setUp() {
    dropsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoDrops() {
    List<DropEntity> result = dropsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedDrop() {
    DropEntity drop = new DropEntity();
    drop.setMessageId("msg1");
    drop.setGuildId("g1");
    drop.setChannelId("ch1");
    drop.setUserId("u1");
    drop.setInput("100");
    drop.setMessageData("NANO");

    DropEntity saved = dropsRepository.insert(drop);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<DropEntity> all = dropsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("msg1", all.get(0).getMessageId());
    assertEquals("g1", all.get(0).getGuildId());
  }

  @Test
  void findByMessageIdReturnsMatchingDrops() {
    DropEntity drop1 = new DropEntity();
    drop1.setMessageId("msg1");
    drop1.setGuildId("g1");
    dropsRepository.insert(drop1);

    DropEntity drop2 = new DropEntity();
    drop2.setMessageId("msg1");
    drop2.setGuildId("g1");
    dropsRepository.insert(drop2);

    DropEntity drop3 = new DropEntity();
    drop3.setMessageId("msg2");
    drop3.setGuildId("g1");
    dropsRepository.insert(drop3);

    List<DropEntity> byMsg1 = dropsRepository.findByMessageId("msg1");
    assertEquals(2, byMsg1.size());
    assertTrue(byMsg1.stream().allMatch(d -> "msg1".equals(d.getMessageId())));

    List<DropEntity> byMsg2 = dropsRepository.findByMessageId("msg2");
    assertEquals(1, byMsg2.size());
    assertEquals("msg2", byMsg2.get(0).getMessageId());
  }

  @Test
  void findByMessageIdWithNullUsesQueryWildcard() {
    DropEntity drop = new DropEntity();
    drop.setMessageId("msg1");
    dropsRepository.insert(drop);

    List<DropEntity> result = dropsRepository.findByMessageId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    DropEntity drop = new DropEntity();
    drop.setMessageId("msg1");
    DropEntity saved = dropsRepository.insert(drop);

    var found = dropsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals(saved.getId(), found.get().getId());
    assertEquals("msg1", found.get().getMessageId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = dropsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    DropEntity drop = new DropEntity();
    drop.setMessageId("msg1");
    drop.setGuildId("g1");
    DropEntity saved = dropsRepository.insert(drop);

    saved.setMessageId("msg2");
    saved.setInput("200");
    DropEntity updated = dropsRepository.save(saved);

    assertEquals("msg2", updated.getMessageId());
    assertEquals("200", updated.getInput());

    var found = dropsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("msg2", found.get().getMessageId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    DropEntity drop = new DropEntity();
    drop.setMessageId("msg1");
    DropEntity saved = dropsRepository.insert(drop);

    dropsRepository.deleteById(saved.getId());

    var found = dropsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
    assertTrue(dropsRepository.findAll().isEmpty());
  }
}
