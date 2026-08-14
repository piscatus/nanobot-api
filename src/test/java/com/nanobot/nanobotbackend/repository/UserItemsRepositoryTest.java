package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class UserItemsRepositoryTest {

  @Autowired
  private UserItemsRepository userItemsRepository;

  @BeforeEach
  void setUp() {
    userItemsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoUserItems() {
    List<UserItemsEntity> result = userItemsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedUserItems() {
    UserItemsEntity userItems = new UserItemsEntity("u1");

    UserItemsEntity saved = userItemsRepository.insert(userItems);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<UserItemsEntity> all = userItemsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByUserIdReturnsMatchingUserItems() {
    UserItemsEntity ui1 = new UserItemsEntity("u1");
    userItemsRepository.insert(ui1);

    UserItemsEntity ui2 = new UserItemsEntity("u2");
    userItemsRepository.insert(ui2);

    List<UserItemsEntity> byU1 = userItemsRepository.findByUserId("u1");
    assertEquals(1, byU1.size());
    assertEquals("u1", byU1.get(0).getUserId());
  }

  @Test
  void findByUserIdWithNullReturnsAll() {
    UserItemsEntity userItems = new UserItemsEntity("u1");
    userItemsRepository.insert(userItems);

    List<UserItemsEntity> result = userItemsRepository.findByUserId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    UserItemsEntity userItems = new UserItemsEntity("u1");
    UserItemsEntity saved = userItemsRepository.insert(userItems);

    var found = userItemsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = userItemsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    UserItemsEntity userItems = new UserItemsEntity("u1");
    UserItemsEntity saved = userItemsRepository.insert(userItems);

    saved.setUserId("u2");
    UserItemsEntity updated = userItemsRepository.save(saved);

    assertEquals("u2", updated.getUserId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    UserItemsEntity userItems = new UserItemsEntity("u1");
    UserItemsEntity saved = userItemsRepository.insert(userItems);

    userItemsRepository.deleteById(saved.getId());

    var found = userItemsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
