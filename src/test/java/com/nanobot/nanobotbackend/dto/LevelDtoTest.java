package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

class LevelDtoTest {

  @Test
  void enumValuesExist() {
    assertEquals(3, LevelDto.values().length);
    assertNotNull(LevelDto.valueOf("UPDATE"));
    assertNotNull(LevelDto.valueOf("RECEIVE"));
    assertNotNull(LevelDto.valueOf("SEND"));
  }

  @Test
  void valueOfReturnsCorrectEnum() {
    assertEquals(LevelDto.UPDATE, LevelDto.valueOf("UPDATE"));
    assertEquals(LevelDto.RECEIVE, LevelDto.valueOf("RECEIVE"));
    assertEquals(LevelDto.SEND, LevelDto.valueOf("SEND"));
  }
}
