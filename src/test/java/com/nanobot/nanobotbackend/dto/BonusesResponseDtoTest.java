package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class BonusesResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    BonusesResponseDto dto = new BonusesResponseDto();
    assertNull(dto.getBonuses());
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    BonusesResponseDto dto = new BonusesResponseDto("Bonuses failed");
    assertEquals("Bonuses failed", dto.getErrorMessage());
  }

  @Test
  void setBonusesUpdatesField() {
    BonusesResponseDto dto = new BonusesResponseDto();
    List<ItemDto> bonuses = Collections.singletonList(new ItemDto("1.5", 25));

    dto.setBonuses(bonuses);

    assertEquals(bonuses, dto.getBonuses());
    assertEquals(1, dto.getBonuses().size());
  }
}
