package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuildConfigurationsEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getStatus());
    assertNull(entity.getFishingChannelId());
  }

  @Test
  void fromDtoCopiesAllFields() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setId("gc-1");
    dto.setGuildId("guild-1");
    dto.setStatus(StatusDto.ACTIVE);
    dto.setFishingLoggingChannelId("ch-log");
    dto.setTransferLoggingChannelId("ch-transfer");
    dto.setFishingChannelId("ch-fish");
    dto.setFishingRole("fisher");
    dto.setFishingBypassRoles(List.of("admin"));
    dto.setFishingError("No fish!");
    dto.setFishingFrequency(60);
    dto.setMaximumActiveUsers(100);
    dto.setMaximumMinutesActive(30);

    GuildConfigurationsEntity entity = new GuildConfigurationsEntity(dto);

    assertEquals("gc-1", entity.getId());
    assertEquals("guild-1", entity.getGuildId());
    assertEquals(StatusDto.ACTIVE, entity.getStatus());
    assertEquals("ch-log", entity.getFishingLoggingChannelId());
    assertEquals("ch-transfer", entity.getTransferLoggingChannelId());
    assertEquals("ch-fish", entity.getFishingChannelId());
    assertEquals("fisher", entity.getFishingRole());
    assertEquals(List.of("admin"), entity.getFishingBypassRoles());
    assertEquals("No fish!", entity.getFishingError());
    assertEquals(60, entity.getFishingFrequency());
    assertEquals(100, entity.getMaximumActiveUsers());
    assertEquals(30, entity.getMaximumMinutesActive());
  }

  @Test
  void settersUpdateFields() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setGuildId("guild-2");
    entity.setStatus(StatusDto.LOCKED);
    entity.setFishingChannelId("ch-2");
    entity.setFishingFrequency(120);

    assertEquals("guild-2", entity.getGuildId());
    assertEquals(StatusDto.LOCKED, entity.getStatus());
    assertEquals("ch-2", entity.getFishingChannelId());
    assertEquals(120, entity.getFishingFrequency());
  }
}
