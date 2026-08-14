package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class GuildWalletsEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    GuildWalletsEntity entity = new GuildWalletsEntity();
    assertNull(entity.getGuildId());
    assertNull(entity.getWallets());
  }

  @Test
  void guildIdConstructorCreatesEntityWithEmptyWallets() {
    GuildWalletsEntity entity = new GuildWalletsEntity("guild-123");

    assertEquals("guild-123", entity.getGuildId());
    assertNotNull(entity.getWallets());
    assertTrue(entity.getWallets().isEmpty());
  }

  @Test
  void fromDtoCopiesAllFields() {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setId("gw-1");
    dto.setGuildId("guild-1");
    WalletDto wallet = new WalletDto();
    wallet.setTicker("BAN");
    wallet.setRaw("50");
    dto.setWallets(List.of(wallet));

    GuildWalletsEntity entity = new GuildWalletsEntity(dto);

    assertEquals("gw-1", entity.getId());
    assertEquals("guild-1", entity.getGuildId());
    assertEquals(1, entity.getWallets().size());
    assertEquals("BAN", entity.getWallets().get(0).getTicker());
    assertEquals("50", entity.getWallets().get(0).getRaw());
  }

  @Test
  void settersUpdateFields() {
    GuildWalletsEntity entity = new GuildWalletsEntity();
    entity.setGuildId("guild-2");
    entity.setWallets(Collections.emptyList());

    assertEquals("guild-2", entity.getGuildId());
    assertNotNull(entity.getWallets());
    assertTrue(entity.getWallets().isEmpty());
  }
}
