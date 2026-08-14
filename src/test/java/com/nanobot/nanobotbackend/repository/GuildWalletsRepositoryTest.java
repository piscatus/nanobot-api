package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class GuildWalletsRepositoryTest {

  @Autowired
  private GuildWalletsRepository guildWalletsRepository;

  @BeforeEach
  void setUp() {
    guildWalletsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoWallets() {
    List<GuildWalletsEntity> result = guildWalletsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedWallet() {
    GuildWalletsEntity wallet = new GuildWalletsEntity("g1");

    GuildWalletsEntity saved = guildWalletsRepository.insert(wallet);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<GuildWalletsEntity> all = guildWalletsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("g1", all.get(0).getGuildId());
  }

  @Test
  void findByGuildIdReturnsMatchingWallets() {
    GuildWalletsEntity w1 = new GuildWalletsEntity("g1");
    guildWalletsRepository.insert(w1);

    GuildWalletsEntity w2 = new GuildWalletsEntity("g2");
    guildWalletsRepository.insert(w2);

    List<GuildWalletsEntity> byG1 = guildWalletsRepository.findByGuildId("g1");
    assertEquals(1, byG1.size());
    assertEquals("g1", byG1.get(0).getGuildId());
  }

  @Test
  void findByGuildIdWithNullReturnsAll() {
    GuildWalletsEntity wallet = new GuildWalletsEntity("g1");
    guildWalletsRepository.insert(wallet);

    List<GuildWalletsEntity> result = guildWalletsRepository.findByGuildId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    GuildWalletsEntity wallet = new GuildWalletsEntity("g1");
    GuildWalletsEntity saved = guildWalletsRepository.insert(wallet);

    var found = guildWalletsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("g1", found.get().getGuildId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = guildWalletsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    GuildWalletsEntity wallet = new GuildWalletsEntity("g1");
    GuildWalletsEntity saved = guildWalletsRepository.insert(wallet);

    saved.setGuildId("g2");
    GuildWalletsEntity updated = guildWalletsRepository.save(saved);

    assertEquals("g2", updated.getGuildId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    GuildWalletsEntity wallet = new GuildWalletsEntity("g1");
    GuildWalletsEntity saved = guildWalletsRepository.insert(wallet);

    guildWalletsRepository.deleteById(saved.getId());

    var found = guildWalletsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
