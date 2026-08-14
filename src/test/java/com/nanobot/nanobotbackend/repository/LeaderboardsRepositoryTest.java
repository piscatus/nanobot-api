package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class LeaderboardsRepositoryTest {

  @Autowired
  private LeaderboardsRepository leaderboardsRepository;

  @BeforeEach
  void setUp() {
    leaderboardsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoLeaderboards() {
    List<LeaderboardEntity> result = leaderboardsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedLeaderboard() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");

    LeaderboardEntity saved = leaderboardsRepository.insert(leaderboard);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<LeaderboardEntity> all = leaderboardsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("g1", all.get(0).getGuildId());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByGuildIdReturnsMatchingLeaderboards() {
    LeaderboardEntity l1 = new LeaderboardEntity();
    l1.setGuildId("g1");
    l1.setUserId("u1");
    leaderboardsRepository.insert(l1);

    LeaderboardEntity l2 = new LeaderboardEntity();
    l2.setGuildId("g1");
    l2.setUserId("u2");
    leaderboardsRepository.insert(l2);

    LeaderboardEntity l3 = new LeaderboardEntity();
    l3.setGuildId("g2");
    l3.setUserId("u1");
    leaderboardsRepository.insert(l3);

    List<LeaderboardEntity> byG1 = leaderboardsRepository.findByGuildId("g1");
    assertEquals(2, byG1.size());
    assertTrue(byG1.stream().allMatch(l -> "g1".equals(l.getGuildId())));
  }

  @Test
  void findByGuildIdWithNullReturnsAll() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");
    leaderboardsRepository.insert(leaderboard);

    List<LeaderboardEntity> result = leaderboardsRepository.findByGuildId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByGuildIdAndUserIdReturnsMatchingLeaderboard() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");
    leaderboardsRepository.insert(leaderboard);

    List<LeaderboardEntity> result =
      leaderboardsRepository.findByGuildIdAndUserId("g1", "u1");
    assertEquals(1, result.size());
    assertEquals("g1", result.get(0).getGuildId());
    assertEquals("u1", result.get(0).getUserId());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");
    LeaderboardEntity saved = leaderboardsRepository.insert(leaderboard);

    var found = leaderboardsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = leaderboardsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");
    LeaderboardEntity saved = leaderboardsRepository.insert(leaderboard);

    saved.setUserId("u2");
    LeaderboardEntity updated = leaderboardsRepository.save(saved);

    assertEquals("u2", updated.getUserId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    LeaderboardEntity leaderboard = new LeaderboardEntity();
    leaderboard.setGuildId("g1");
    leaderboard.setUserId("u1");
    LeaderboardEntity saved = leaderboardsRepository.insert(leaderboard);

    leaderboardsRepository.deleteById(saved.getId());

    var found = leaderboardsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
