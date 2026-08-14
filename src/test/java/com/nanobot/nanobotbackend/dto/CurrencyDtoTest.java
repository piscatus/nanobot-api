package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import org.junit.jupiter.api.Test;

class CurrencyDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    CurrencyDto dto = new CurrencyDto();
    assertNull(dto.getTicker());
  }

  @Test
  void fromEntityCopiesAllFields() {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setId("c-1");
    entity.setTicker("NANO");
    entity.setName("Nano");
    entity.setEnabled(true);
    entity.setEmoji("🪙");
    entity.setPrecision("30");

    CurrencyDto dto = new CurrencyDto(entity);

    assertEquals("c-1", dto.getId());
    assertEquals("NANO", dto.getTicker());
    assertEquals("Nano", dto.getName());
    assertTrue(dto.getEnabled());
    assertEquals("🪙", dto.getEmoji());
    assertEquals("30", dto.getPrecision());
  }

  @Test
  void threeArgConstructorSetsCoreFields() {
    CurrencyDto dto = new CurrencyDto("XNO", "Nano", true);

    assertEquals("XNO", dto.getTicker());
    assertEquals("Nano", dto.getName());
    assertTrue(dto.getEnabled());
  }

  @Test
  void fiveArgConstructorSetsCoreFields() {
    CurrencyDto dto = new CurrencyDto("XNO", "Nano", true, "🪙", "30");

    assertEquals("XNO", dto.getTicker());
    assertEquals("Nano", dto.getName());
    assertTrue(dto.getEnabled());
    assertEquals("🪙", dto.getEmoji());
    assertEquals("30", dto.getPrecision());
  }

  @Test
  void settersUpdateFields() {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker("BAN");
    dto.setName("Banano");
    dto.setEnabled(false);
    dto.setMinimumDrop("1");

    assertEquals("BAN", dto.getTicker());
    assertEquals("Banano", dto.getName());
    assertFalse(dto.getEnabled());
    assertEquals("1", dto.getMinimumDrop());
  }
}
