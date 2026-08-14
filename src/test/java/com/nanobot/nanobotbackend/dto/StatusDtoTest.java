package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class StatusDtoTest {

  @Test
  void enumValuesExist() {
    assertEquals(3, StatusDto.values().length);
    assertNotNull(StatusDto.valueOf("ACTIVE"));
    assertNotNull(StatusDto.valueOf("LOCKED"));
    assertNotNull(StatusDto.valueOf("BANNED"));
  }

  @Test
  void valueOfReturnsCorrectEnum() {
    assertEquals(StatusDto.ACTIVE, StatusDto.valueOf("ACTIVE"));
    assertEquals(StatusDto.LOCKED, StatusDto.valueOf("LOCKED"));
    assertEquals(StatusDto.BANNED, StatusDto.valueOf("BANNED"));
  }
}
