package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class UserWalletsRepositoryTest {

  @Autowired
  private UserWalletsRepository userWalletsRepository;

  @BeforeEach
  void setUp() {
    userWalletsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoUserWallets() {
    List<UserWalletsEntity> result = userWalletsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedUserWallet() {
    UserWalletsEntity userWallet = new UserWalletsEntity("u1");

    UserWalletsEntity saved = userWalletsRepository.insert(userWallet);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<UserWalletsEntity> all = userWalletsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByUserIdReturnsMatchingUserWallets() {
    UserWalletsEntity uw1 = new UserWalletsEntity("u1");
    userWalletsRepository.insert(uw1);

    UserWalletsEntity uw2 = new UserWalletsEntity("u2");
    userWalletsRepository.insert(uw2);

    List<UserWalletsEntity> byU1 = userWalletsRepository.findByUserId("u1");
    assertEquals(1, byU1.size());
    assertEquals("u1", byU1.get(0).getUserId());
  }

  @Test
  void findByUserIdWithNullReturnsAll() {
    UserWalletsEntity userWallet = new UserWalletsEntity("u1");
    userWalletsRepository.insert(userWallet);

    List<UserWalletsEntity> result = userWalletsRepository.findByUserId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    UserWalletsEntity userWallet = new UserWalletsEntity("u1");
    UserWalletsEntity saved = userWalletsRepository.insert(userWallet);

    var found = userWalletsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = userWalletsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    UserWalletsEntity userWallet = new UserWalletsEntity("u1");
    UserWalletsEntity saved = userWalletsRepository.insert(userWallet);

    saved.setUserId("u2");
    UserWalletsEntity updated = userWalletsRepository.save(saved);

    assertEquals("u2", updated.getUserId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    UserWalletsEntity userWallet = new UserWalletsEntity("u1");
    UserWalletsEntity saved = userWalletsRepository.insert(userWallet);

    userWalletsRepository.deleteById(saved.getId());

    var found = userWalletsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
