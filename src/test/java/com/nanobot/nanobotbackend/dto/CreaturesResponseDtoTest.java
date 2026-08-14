package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class CreaturesResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    CreaturesResponseDto dto = new CreaturesResponseDto();
    assertNull(dto.getBonuses());
    assertNull(dto.getCurrencies());
    assertNull(dto.getCreatures());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    CreaturesResponseDto dto = new CreaturesResponseDto("Creatures failed");
    assertEquals("Creatures failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    CreaturesResponseDto dto = new CreaturesResponseDto();
    List<ItemDto> bonuses = Collections.singletonList(new ItemDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());

    dto.setBonuses(bonuses);
    dto.setCurrencies(currencies);
    dto.setCreatures(creatures);

    assertEquals(bonuses, dto.getBonuses());
    assertEquals(currencies, dto.getCurrencies());
    assertEquals(creatures, dto.getCreatures());
  }
}
