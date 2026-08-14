package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.CreatureEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class CreaturesRepositoryTest {

  @Autowired
  private CreaturesRepository creaturesRepository;

  @BeforeEach
  void setUp() {
    creaturesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoCreatures() {
    List<CreatureEntity> result = creaturesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedCreature() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("NanoFish");
    creature.setPluralization("NanoFish");
    creature.setCapacity(100);
    creature.setOdds(50);
    creature.setValue("1");
    creature.setTicker("XNO");

    CreatureEntity saved = creaturesRepository.insert(creature);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<CreatureEntity> all = creaturesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("NanoFish", all.get(0).getName());
  }

  @Test
  void findByNameReturnsMatchingCreatures() {
    CreatureEntity c1 = new CreatureEntity();
    c1.setName("NanoFish");
    creaturesRepository.insert(c1);

    CreatureEntity c2 = new CreatureEntity();
    c2.setName("NanoFish");
    creaturesRepository.insert(c2);

    CreatureEntity c3 = new CreatureEntity();
    c3.setName("MegaFish");
    creaturesRepository.insert(c3);

    List<CreatureEntity> byName = creaturesRepository.findByName("NanoFish");
    assertEquals(2, byName.size());
    assertTrue(byName.stream().allMatch(c -> "NanoFish".equals(c.getName())));
  }

  @Test
  void findByNameWithNullReturnsAll() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("NanoFish");
    creaturesRepository.insert(creature);

    List<CreatureEntity> result = creaturesRepository.findByName(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("NanoFish");
    CreatureEntity saved = creaturesRepository.insert(creature);

    var found = creaturesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("NanoFish", found.get().getName());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = creaturesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("NanoFish");
    creature.setValue("1");
    CreatureEntity saved = creaturesRepository.insert(creature);

    saved.setName("MegaFish");
    saved.setValue("10");
    CreatureEntity updated = creaturesRepository.save(saved);

    assertEquals("MegaFish", updated.getName());
    assertEquals("10", updated.getValue());
  }

  @Test
  void deleteByIdRemovesEntity() {
    CreatureEntity creature = new CreatureEntity();
    creature.setName("NanoFish");
    CreatureEntity saved = creaturesRepository.insert(creature);

    creaturesRepository.deleteById(saved.getId());

    var found = creaturesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
