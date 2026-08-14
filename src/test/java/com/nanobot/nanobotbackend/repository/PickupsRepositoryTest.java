package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.Date;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class PickupsRepositoryTest {

  @Autowired
  private PickupsRepository pickupsRepository;

  @BeforeEach
  void setUp() {
    pickupsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoPickups() {
    List<PickupEntity> result = pickupsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedPickup() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    pickup.setTimestamp(new Date());

    PickupEntity saved = pickupsRepository.insert(pickup);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<PickupEntity> all = pickupsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("drop1", all.get(0).getDropId());
    assertEquals("u1", all.get(0).getUserId());
  }

  @Test
  void findByDropIdReturnsMatchingPickups() {
    PickupEntity p1 = new PickupEntity();
    p1.setDropId("drop1");
    p1.setUserId("u1");
    pickupsRepository.insert(p1);

    PickupEntity p2 = new PickupEntity();
    p2.setDropId("drop1");
    p2.setUserId("u2");
    pickupsRepository.insert(p2);

    PickupEntity p3 = new PickupEntity();
    p3.setDropId("drop2");
    p3.setUserId("u1");
    pickupsRepository.insert(p3);

    List<PickupEntity> byDrop1 = pickupsRepository.findByDropId("drop1");
    assertEquals(2, byDrop1.size());
    assertTrue(byDrop1.stream().allMatch(p -> "drop1".equals(p.getDropId())));
  }

  @Test
  void findByDropIdWithNullReturnsAll() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    pickupsRepository.insert(pickup);

    List<PickupEntity> result = pickupsRepository.findByDropId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByDropIdAndUserIdReturnsMatchingPickup() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    pickupsRepository.insert(pickup);

    List<PickupEntity> result =
      pickupsRepository.findByDropIdAndUserId("drop1", "u1");
    assertEquals(1, result.size());
    assertEquals("drop1", result.get(0).getDropId());
    assertEquals("u1", result.get(0).getUserId());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    PickupEntity saved = pickupsRepository.insert(pickup);

    var found = pickupsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("drop1", found.get().getDropId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = pickupsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    PickupEntity saved = pickupsRepository.insert(pickup);

    saved.setUserId("u2");
    PickupEntity updated = pickupsRepository.save(saved);

    assertEquals("u2", updated.getUserId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    PickupEntity pickup = new PickupEntity();
    pickup.setDropId("drop1");
    pickup.setUserId("u1");
    PickupEntity saved = pickupsRepository.insert(pickup);

    pickupsRepository.deleteById(saved.getId());

    var found = pickupsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
