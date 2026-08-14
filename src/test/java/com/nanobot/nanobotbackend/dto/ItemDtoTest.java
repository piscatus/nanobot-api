package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.Date;
import org.junit.jupiter.api.Test;

class ItemDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    ItemDto dto = new ItemDto();
    assertNull(dto.getName());
    assertEquals(0, dto.getQuantity());
    assertFalse(dto.getRequired());
    assertNull(dto.getTimestamp());
  }

  @Test
  void nameQuantityConstructorSetsFields() {
    ItemDto dto = new ItemDto("FISH", 5);

    assertEquals("FISH", dto.getName());
    assertEquals(5, dto.getQuantity());
    assertFalse(dto.getRequired());
  }

  @Test
  void nameQuantityRequiredConstructorSetsFields() {
    ItemDto dto = new ItemDto("NANO", 10, true);

    assertEquals("NANO", dto.getName());
    assertEquals(10, dto.getQuantity());
    assertTrue(dto.getRequired());
  }

  @Test
  void copyConstructorCopiesFields() {
    ItemDto source = new ItemDto("BAN", 3, true);
    source.setId("item-1");
    source.setTimestamp(new Date());

    ItemDto dto = new ItemDto(source);

    assertEquals("item-1", dto.getId());
    assertEquals("BAN", dto.getName());
    assertEquals(3, dto.getQuantity());
    assertTrue(dto.getRequired());
  }

  @Test
  void settersUpdateFields() {
    ItemDto dto = new ItemDto();
    Date ts = new Date();
    dto.setName("CRAB");
    dto.setQuantity(7);
    dto.setRequired(true);
    dto.setTimestamp(ts);

    assertEquals("CRAB", dto.getName());
    assertEquals(7, dto.getQuantity());
    assertTrue(dto.getRequired());
    assertEquals(ts, dto.getTimestamp());
  }
}
