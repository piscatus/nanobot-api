package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.AnglerEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class AnglersRepositoryTest {

  @Autowired
  private AnglersRepository anglersRepository;

  @BeforeEach
  void setUp() {
    anglersRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoAnglers() {
    List<AnglerEntity> result = anglersRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedAngler() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    angler.setResting(false);
    angler.setTimestamp(new Date());

    AnglerEntity saved = anglersRepository.insert(angler);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<AnglerEntity> all = anglersRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("g1", all.get(0).getGuildId());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findAllRestingAnglersReturnsOnlyResting() {
    AnglerEntity resting = new AnglerEntity();
    resting.setGuildId("g1");
    resting.setUserId("u1");
    resting.setResting(true);
    anglersRepository.insert(resting);

    AnglerEntity active = new AnglerEntity();
    active.setGuildId("g1");
    active.setUserId("u2");
    active.setResting(false);
    anglersRepository.insert(active);

    List<AnglerEntity> restingList = anglersRepository.findAllRestingAnglers();
    assertEquals(1, restingList.size());
    assertTrue(restingList.get(0).getResting());
  }

  @Test
  void findByGuildIdAndUserIdReturnsMatchingAngler() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    angler.setResting(true);
    anglersRepository.insert(angler);

    List<AnglerEntity> result = anglersRepository.findByGuildIdAndUserId("g1", "u1");
    assertEquals(1, result.size());
    assertEquals("u1", result.get(0).getUserId());
    assertEquals("g1", result.get(0).getGuildId());
  }

  @Test
  void findByGuildIdAndUserIdWithNullGuildIdReturnsAllForUser() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    anglersRepository.insert(angler);

    List<AnglerEntity> result = anglersRepository.findByGuildIdAndUserId(null, "u1");
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    AnglerEntity saved = anglersRepository.insert(angler);

    var found = anglersRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("u1", found.get().getUserId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = anglersRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    angler.setResting(false);
    AnglerEntity saved = anglersRepository.insert(angler);

    saved.setResting(true);
    AnglerEntity updated = anglersRepository.save(saved);

    assertTrue(updated.getResting());
  }

  @Test
  void deleteByIdRemovesEntity() {
    AnglerEntity angler = new AnglerEntity();
    angler.setGuildId("g1");
    angler.setUserId("u1");
    AnglerEntity saved = anglersRepository.insert(angler);

    anglersRepository.deleteById(saved.getId());

    var found = anglersRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
