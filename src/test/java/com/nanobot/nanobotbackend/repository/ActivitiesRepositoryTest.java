package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.ActivityEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class ActivitiesRepositoryTest {

  @Autowired
  private ActivitiesRepository activitiesRepository;

  @BeforeEach
  void setUp() {
    activitiesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoActivities() {
    List<ActivityEntity> result = activitiesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedActivity() {
    ActivityEntity activity = new ActivityEntity();
    activity.setGuildId("g1");
    activity.setChannelId("ch1");
    activity.setUserId("u1");
    activity.setTimestamp(new Date());

    ActivityEntity saved = activitiesRepository.insert(activity);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<ActivityEntity> all = activitiesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("g1", all.get(0).getGuildId());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByGuildIdReturnsMatchingActivities() {
    ActivityEntity a1 = new ActivityEntity();
    a1.setGuildId("g1");
    a1.setUserId("u1");
    activitiesRepository.insert(a1);

    ActivityEntity a2 = new ActivityEntity();
    a2.setGuildId("g1");
    a2.setUserId("u2");
    activitiesRepository.insert(a2);

    ActivityEntity a3 = new ActivityEntity();
    a3.setGuildId("g2");
    a3.setUserId("u1");
    activitiesRepository.insert(a3);

    List<ActivityEntity> byG1 = activitiesRepository.findByGuildId("g1");
    assertEquals(2, byG1.size());
    assertTrue(byG1.stream().allMatch(a -> "g1".equals(a.getGuildId())));
  }

  @Test
  void findByGuildIdWithNullReturnsAll() {
    ActivityEntity a = new ActivityEntity();
    a.setGuildId("g1");
    activitiesRepository.insert(a);

    List<ActivityEntity> result = activitiesRepository.findByGuildId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByGuildIdAndChannelIdReturnsMatchingActivities() {
    ActivityEntity a1 = new ActivityEntity();
    a1.setGuildId("g1");
    a1.setChannelId("ch1");
    a1.setUserId("u1");
    activitiesRepository.insert(a1);

    ActivityEntity a2 = new ActivityEntity();
    a2.setGuildId("g1");
    a2.setChannelId("ch2");
    a2.setUserId("u2");
    activitiesRepository.insert(a2);

    List<ActivityEntity> result =
      activitiesRepository.findByGuildIdAndChannelId("g1", "ch1");
    assertEquals(1, result.size());
    assertEquals("ch1", result.get(0).getChannelId());
  }

  @Test
  void findByGuildIdAndChannelIdAndUserIdReturnsMatchingActivity() {
    ActivityEntity activity = new ActivityEntity();
    activity.setGuildId("g1");
    activity.setChannelId("ch1");
    activity.setUserId("u1");
    activitiesRepository.insert(activity);

    List<ActivityEntity> result =
      activitiesRepository.findByGuildIdAndChannelIdAndUserId("g1", "ch1", "u1");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getUserId());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    ActivityEntity activity = new ActivityEntity();
    activity.setGuildId("g1");
    ActivityEntity saved = activitiesRepository.insert(activity);

    var found = activitiesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("g1", found.get().getGuildId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = activitiesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void deleteByIdRemovesEntity() {
    ActivityEntity activity = new ActivityEntity();
    activity.setGuildId("g1");
    ActivityEntity saved = activitiesRepository.insert(activity);

    activitiesRepository.deleteById(saved.getId());

    var found = activitiesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
