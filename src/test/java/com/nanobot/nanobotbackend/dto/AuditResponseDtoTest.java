package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class AuditResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    AuditResponseDto dto = new AuditResponseDto();
    assertNull(dto.getBonuses());
    assertNull(dto.getCurrencies());
    assertNull(dto.getCreatures());
    assertNull(dto.getDrops());
    assertNull(dto.getGuildsWallets());
    assertNull(dto.getUsersItems());
    assertNull(dto.getUsersWallets());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    AuditResponseDto dto = new AuditResponseDto("Audit failed");
    assertEquals("Audit failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    AuditResponseDto dto = new AuditResponseDto();
    List<ItemDto> bonuses = Collections.singletonList(new ItemDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());
    List<DropDto> drops = Collections.singletonList(new DropDto());
    List<GuildWalletsDto> guilds = Collections.singletonList(new GuildWalletsDto());
    List<UserItemsDto> userItems = Collections.singletonList(new UserItemsDto());
    List<UserWalletsDto> userWallets = Collections.singletonList(new UserWalletsDto());

    dto.setBonuses(bonuses);
    dto.setCurrencies(currencies);
    dto.setCreatures(creatures);
    dto.setDrops(drops);
    dto.setGuildsWallets(guilds);
    dto.setUsersItems(userItems);
    dto.setUsersWallets(userWallets);

    assertEquals(bonuses, dto.getBonuses());
    assertEquals(currencies, dto.getCurrencies());
    assertEquals(creatures, dto.getCreatures());
    assertEquals(drops, dto.getDrops());
    assertEquals(guilds, dto.getGuildsWallets());
    assertEquals(userItems, dto.getUsersItems());
    assertEquals(userWallets, dto.getUsersWallets());
  }
}
