package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class BaseDtoTest {

  @Test
  void defaultConstructorSetsNullId() {
    BaseDto dto = new BaseDto();
    assertNull(dto.getId());
  }

  @Test
  void stringConstructorSetsId() {
    BaseDto dto = new BaseDto("id-1");
    assertEquals("id-1", dto.getId());
  }

  @Test
  void setIdUpdatesValue() {
    BaseDto dto = new BaseDto();
    dto.setId("id-2");
    assertEquals("id-2", dto.getId());
  }

  @Test
  void toJsonExcludesNullsWhenIncludeNullsFalse() {
    BaseDto dto = new BaseDto("id-1");
    String json = dto.toJson(false);
    assertNotNull(json);
    assertTrue(json.contains("id-1"));
  }

  @Test
  void toJsonIncludesNullsWhenIncludeNullsTrue() {
    BaseDto dto = new BaseDto();
    String json = dto.toJson(true);
    assertNotNull(json);
    assertTrue(json.contains("null"));
  }
}
