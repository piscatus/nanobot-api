package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.VerificationEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class VerificationsRepositoryTest {

  @Autowired
  private VerificationsRepository verificationsRepository;

  @BeforeEach
  void setUp() {
    verificationsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoVerifications() {
    List<VerificationEntity> result = verificationsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedVerification() {
    VerificationEntity verification = new VerificationEntity();
    verification.setUserId("u1");

    VerificationEntity saved = verificationsRepository.insert(verification);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<VerificationEntity> all = verificationsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByUserIdReturnsMatchingVerifications() {
    VerificationEntity v1 = new VerificationEntity();
    v1.setUserId("u1");
    verificationsRepository.insert(v1);

    VerificationEntity v2 = new VerificationEntity();
    v2.setUserId("u1");
    verificationsRepository.insert(v2);

    VerificationEntity v3 = new VerificationEntity();
    v3.setUserId("u2");
    verificationsRepository.insert(v3);

    List<VerificationEntity> byU1 = verificationsRepository.findByUserId("u1");
    assertEquals(2, byU1.size());
    assertTrue(byU1.stream().allMatch(v -> "u1".equals(v.getUserId())));

    List<VerificationEntity> byU2 = verificationsRepository.findByUserId("u2");
    assertEquals(1, byU2.size());
    assertEquals("u2", byU2.get(0).getUserId());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    VerificationEntity verification = new VerificationEntity();
    verification.setUserId("u1");
    VerificationEntity saved = verificationsRepository.insert(verification);

    var found = verificationsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = verificationsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    VerificationEntity verification = new VerificationEntity();
    verification.setUserId("u1");
    VerificationEntity saved = verificationsRepository.insert(verification);

    saved.setUserId("u2");
    VerificationEntity updated = verificationsRepository.save(saved);

    assertEquals("u2", updated.getUserId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    VerificationEntity verification = new VerificationEntity();
    verification.setUserId("u1");
    VerificationEntity saved = verificationsRepository.insert(verification);

    verificationsRepository.deleteById(saved.getId());

    var found = verificationsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
