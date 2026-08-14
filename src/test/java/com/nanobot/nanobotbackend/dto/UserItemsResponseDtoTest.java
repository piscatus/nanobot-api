package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class UserItemsResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    UserItemsResponseDto dto = new UserItemsResponseDto();
    assertNull(dto.getBonuses());
    assertNull(dto.getUserItems());
    assertNull(dto.getSubordinateItems());
    assertNull(dto.getCurrencies());
    assertNull(dto.getCreatures());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    UserItemsResponseDto dto = new UserItemsResponseDto("User items failed");
    assertEquals("User items failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    UserItemsResponseDto dto = new UserItemsResponseDto();
    List<ItemDto> bonuses = Collections.singletonList(new ItemDto());
    List<ItemDto> userItems = Collections.singletonList(new ItemDto());
    List<ItemDto> subordinateItems = Collections.singletonList(new ItemDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());

    dto.setBonuses(bonuses);
    dto.setUserItems(userItems);
    dto.setSubordinateItems(subordinateItems);
    dto.setCurrencies(currencies);
    dto.setCreatures(creatures);

    assertEquals(bonuses, dto.getBonuses());
    assertEquals(userItems, dto.getUserItems());
    assertEquals(subordinateItems, dto.getSubordinateItems());
    assertEquals(currencies, dto.getCurrencies());
    assertEquals(creatures, dto.getCreatures());
  }
}
