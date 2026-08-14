package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.AliasEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class AliasesRepositoryTest {

  @Autowired
  private AliasesRepository aliasesRepository;

  @BeforeEach
  void setUp() {
    aliasesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoAliases() {
    List<AliasEntity> result = aliasesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedAlias() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("big");
    alias.setPlural("bigs");
    alias.setTicker("XNO");
    alias.setValue("100");
    alias.setEmoji(":big:");

    AliasEntity saved = aliasesRepository.insert(alias);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<AliasEntity> all = aliasesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("big", all.get(0).getSingular());
    assertEquals("g1", all.get(0).getGuildId());
  }

  @Test
  void findByGuildIdReturnsMatchingAliases() {
    AliasEntity a1 = new AliasEntity();
    a1.setGuildId("g1");
    a1.setSingular("big");
    aliasesRepository.insert(a1);

    AliasEntity a2 = new AliasEntity();
    a2.setGuildId("g1");
    a2.setSingular("small");
    aliasesRepository.insert(a2);

    AliasEntity a3 = new AliasEntity();
    a3.setGuildId("g2");
    a3.setSingular("big");
    aliasesRepository.insert(a3);

    List<AliasEntity> byG1 = aliasesRepository.findByGuildId("g1");
    assertEquals(2, byG1.size());
    assertTrue(byG1.stream().allMatch(a -> "g1".equals(a.getGuildId())));
  }

  @Test
  void findByGuildIdAndSingularReturnsMatchingAlias() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("big");
    alias.setPlural("bigs");
    aliasesRepository.insert(alias);

    List<AliasEntity> result = aliasesRepository.findByGuildIdAndSingular("g1", "big");
    assertEquals(1, result.size());
    assertEquals("big", result.get(0).getSingular());
    assertEquals("g1", result.get(0).getGuildId());
  }

  @Test
  void findByGuildIdAndSingularIsCaseInsensitive() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("Big");
    alias.setPlural("Bigs");
    aliasesRepository.insert(alias);

    List<AliasEntity> result = aliasesRepository.findByGuildIdAndSingular("g1", "BIG");
    assertEquals(1, result.size());
    assertEquals("Big", result.get(0).getSingular());
  }

  @Test
  void findByEitherGuildIdReturnsAliasesFromEitherGuild() {
    AliasEntity a1 = new AliasEntity();
    a1.setGuildId("g1");
    a1.setSingular("big");
    aliasesRepository.insert(a1);

    AliasEntity a2 = new AliasEntity();
    a2.setGuildId("GLOBAL");
    a2.setSingular("nano");
    aliasesRepository.insert(a2);

    AliasEntity a3 = new AliasEntity();
    a3.setGuildId("g2");
    a3.setSingular("small");
    aliasesRepository.insert(a3);

    List<AliasEntity> result = aliasesRepository.findByEitherGuildId("g1", "GLOBAL");
    assertEquals(2, result.size());
    assertTrue(
      result.stream()
        .anyMatch(a -> "g1".equals(a.getGuildId()))
    );
    assertTrue(
      result.stream()
        .anyMatch(a -> "GLOBAL".equals(a.getGuildId()))
    );
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("big");
    AliasEntity saved = aliasesRepository.insert(alias);

    var found = aliasesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("big", found.get().getSingular());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = aliasesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingAlias() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("big");
    alias.setValue("100");
    AliasEntity saved = aliasesRepository.insert(alias);

    saved.setValue("200");
    saved.setSingular("bigger");
    AliasEntity updated = aliasesRepository.save(saved);

    assertEquals("200", updated.getValue());
    assertEquals("bigger", updated.getSingular());
  }

  @Test
  void deleteByIdRemovesEntity() {
    AliasEntity alias = new AliasEntity();
    alias.setGuildId("g1");
    alias.setSingular("big");
    AliasEntity saved = aliasesRepository.insert(alias);

    aliasesRepository.deleteById(saved.getId());

    var found = aliasesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
