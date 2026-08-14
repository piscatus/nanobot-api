package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuildConfigurationsDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    assertNull(dto.getGuildId());
    assertNull(dto.getStatus());
    assertNull(dto.getFishingChannelId());
  }

  @Test
  void fromEntityCopiesAllFields() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc-1");
    entity.setGuildId("guild-1");
    entity.setStatus(StatusDto.ACTIVE);
    entity.setFishingLoggingChannelId("ch-log");
    entity.setTransferLoggingChannelId("ch-transfer");
    entity.setFishingChannelId("ch-fish");
    entity.setFishingRole("fisher");
    entity.setFishingBypassRoles(List.of("admin"));
    entity.setFishingError("No fish!");
    entity.setFishingFrequency(60);
    entity.setMaximumActiveUsers(100);
    entity.setMaximumMinutesActive(30);

    GuildConfigurationsDto dto = new GuildConfigurationsDto(entity);

    assertEquals("gc-1", dto.getId());
    assertEquals("guild-1", dto.getGuildId());
    assertEquals(StatusDto.ACTIVE, dto.getStatus());
    assertEquals("ch-log", dto.getFishingLoggingChannelId());
    assertEquals("ch-transfer", dto.getTransferLoggingChannelId());
    assertEquals("ch-fish", dto.getFishingChannelId());
    assertEquals("fisher", dto.getFishingRole());
    assertEquals(List.of("admin"), dto.getFishingBypassRoles());
    assertEquals("No fish!", dto.getFishingError());
    assertEquals(60, dto.getFishingFrequency());
    assertEquals(100, dto.getMaximumActiveUsers());
    assertEquals(30, dto.getMaximumMinutesActive());
  }

  @Test
  void settersUpdateFields() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setGuildId("guild-2");
    dto.setStatus(StatusDto.LOCKED);
    dto.setFishingChannelId("ch-2");
    dto.setFishingFrequency(120);

    assertEquals("guild-2", dto.getGuildId());
    assertEquals(StatusDto.LOCKED, dto.getStatus());
    assertEquals("ch-2", dto.getFishingChannelId());
    assertEquals(120, dto.getFishingFrequency());
  }
}
