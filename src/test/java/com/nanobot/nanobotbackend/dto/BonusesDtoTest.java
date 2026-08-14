package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class BonusesDtoTest {

  @Test
  void defaultConstructorInitializesDefaultBonuses() {
    BonusesDto dto = new BonusesDto();
    List<ItemDto> bonuses = dto.getBonuses();

    assertNotNull(bonuses);
    assertEquals(4, bonuses.size());
    assertEquals("1.25", bonuses.get(0).getName());
    assertEquals(10, bonuses.get(0).getQuantity());
    assertEquals("2.00", bonuses.get(3).getName());
    assertEquals(100, bonuses.get(3).getQuantity());
  }

  @Test
  void setBonusesReplacesList() {
    BonusesDto dto = new BonusesDto();
    List<ItemDto> custom = new ArrayList<>();
    custom.add(new ItemDto("3.00", 200));

    dto.setBonuses(custom);

    assertEquals(1, dto.getBonuses().size());
    assertEquals("3.00", dto.getBonuses().get(0).getName());
    assertEquals(200, dto.getBonuses().get(0).getQuantity());
  }
}
