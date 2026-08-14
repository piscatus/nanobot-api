package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class GuildConfigurationsRepositoryTest {

  @Autowired
  private GuildConfigurationsRepository guildConfigurationsRepository;

  @BeforeEach
  void setUp() {
    guildConfigurationsRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoConfigurations() {
    List<GuildConfigurationsEntity> result = guildConfigurationsRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedConfiguration() {
    GuildConfigurationsEntity config = new GuildConfigurationsEntity();
    config.setGuildId("g1");
    config.setFishingChannelId("ch1");

    GuildConfigurationsEntity saved = guildConfigurationsRepository.insert(config);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<GuildConfigurationsEntity> all = guildConfigurationsRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("g1", all.get(0).getGuildId());
  }

  @Test
  void findByGuildIdReturnsMatchingConfigurations() {
    GuildConfigurationsEntity c1 = new GuildConfigurationsEntity();
    c1.setGuildId("g1");
    guildConfigurationsRepository.insert(c1);

    GuildConfigurationsEntity c2 = new GuildConfigurationsEntity();
    c2.setGuildId("g2");
    guildConfigurationsRepository.insert(c2);

    List<GuildConfigurationsEntity> byG1 =
      guildConfigurationsRepository.findByGuildId("g1");
    assertEquals(1, byG1.size());
    assertEquals("g1", byG1.get(0).getGuildId());
  }

  @Test
  void findByGuildIdWithNullReturnsAll() {
    GuildConfigurationsEntity config = new GuildConfigurationsEntity();
    config.setGuildId("g1");
    guildConfigurationsRepository.insert(config);

    List<GuildConfigurationsEntity> result =
      guildConfigurationsRepository.findByGuildId(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    GuildConfigurationsEntity config = new GuildConfigurationsEntity();
    config.setGuildId("g1");
    GuildConfigurationsEntity saved = guildConfigurationsRepository.insert(config);

    var found = guildConfigurationsRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("g1", found.get().getGuildId());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = guildConfigurationsRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    GuildConfigurationsEntity config = new GuildConfigurationsEntity();
    config.setGuildId("g1");
    config.setFishingChannelId("ch1");
    GuildConfigurationsEntity saved = guildConfigurationsRepository.insert(config);

    saved.setFishingChannelId("ch2");
    GuildConfigurationsEntity updated = guildConfigurationsRepository.save(saved);

    assertEquals("ch2", updated.getFishingChannelId());
  }

  @Test
  void deleteByIdRemovesEntity() {
    GuildConfigurationsEntity config = new GuildConfigurationsEntity();
    config.setGuildId("g1");
    GuildConfigurationsEntity saved = guildConfigurationsRepository.insert(config);

    guildConfigurationsRepository.deleteById(saved.getId());

    var found = guildConfigurationsRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
