package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuildWalletsDtoTest {

  @Test
  void defaultConstructorCreatesDtoWithEmptyWallets() {
    GuildWalletsDto dto = new GuildWalletsDto();

    assertNull(dto.getGuildId());
    assertNotNull(dto.getWallets());
    assertTrue(dto.getWallets().isEmpty());
  }

  @Test
  void fromEntityCopiesAllFieldsWithWalletCopy() {
    GuildWalletsEntity entity = new GuildWalletsEntity();
    entity.setId("gw-1");
    entity.setGuildId("guild-1");
    WalletDto wallet = new WalletDto("NANO", "100");
    entity.setWallets(List.of(wallet));

    GuildWalletsDto dto = new GuildWalletsDto(entity);

    assertEquals("gw-1", dto.getId());
    assertEquals("guild-1", dto.getGuildId());
    assertNotNull(dto.getWallets());
    assertEquals(1, dto.getWallets().size());
    assertEquals("NANO", dto.getWallets().get(0).getTicker());
    assertEquals("100", dto.getWallets().get(0).getRaw());
  }

  @Test
  void fromEntityHandlesNullWallets() {
    GuildWalletsEntity entity = new GuildWalletsEntity();
    entity.setId("gw-2");
    entity.setGuildId("guild-2");
    entity.setWallets(null);

    GuildWalletsDto dto = new GuildWalletsDto(entity);

    assertEquals("gw-2", dto.getId());
    assertEquals("guild-2", dto.getGuildId());
    assertNotNull(dto.getWallets());
    assertTrue(dto.getWallets().isEmpty());
  }

  @Test
  void settersUpdateFields() {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("guild-3");
    dto.setWallets(List.of(new WalletDto("BAN", "50")));

    assertEquals("guild-3", dto.getGuildId());
    assertEquals(1, dto.getWallets().size());
    assertEquals("BAN", dto.getWallets().get(0).getTicker());
  }
}
