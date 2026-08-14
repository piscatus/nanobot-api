package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class ProfanitiesRepositoryTest {

  @Autowired
  private ProfanitiesRepository profanitiesRepository;

  @BeforeEach
  void setUp() {
    profanitiesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoProfanities() {
    List<ProfanityEntity> result = profanitiesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedProfanity() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("badword");
    profanity.setPlural("badwords");
    profanity.setContains(false);

    ProfanityEntity saved = profanitiesRepository.insert(profanity);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<ProfanityEntity> all = profanitiesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("badword", all.get(0).getSingular());
  }

  @Test
  void findBySingularReturnsMatchingProfanityCaseInsensitive() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("BadWord");
    profanity.setPlural("badwords");
    profanitiesRepository.insert(profanity);

    List<ProfanityEntity> result =
      profanitiesRepository.findBySingular("badword");
    assertEquals(1, result.size());
    assertEquals("BadWord", result.get(0).getSingular());
  }

  @Test
  void findBySingularWithNullReturnsAll() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("badword");
    profanity.setPlural("badwords");
    profanitiesRepository.insert(profanity);

    List<ProfanityEntity> result = profanitiesRepository.findBySingular(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("badword");
    profanity.setPlural("badwords");
    ProfanityEntity saved = profanitiesRepository.insert(profanity);

    var found = profanitiesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("badword", found.get().getSingular());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = profanitiesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("badword");
    profanity.setPlural("badwords");
    profanity.setContains(false);
    ProfanityEntity saved = profanitiesRepository.insert(profanity);

    saved.setContains(true);
    ProfanityEntity updated = profanitiesRepository.save(saved);

    assertTrue(updated.getContains());
  }

  @Test
  void deleteByIdRemovesEntity() {
    ProfanityEntity profanity = new ProfanityEntity();
    profanity.setSingular("badword");
    profanity.setPlural("badwords");
    ProfanityEntity saved = profanitiesRepository.insert(profanity);

    profanitiesRepository.deleteById(saved.getId());

    var found = profanitiesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
