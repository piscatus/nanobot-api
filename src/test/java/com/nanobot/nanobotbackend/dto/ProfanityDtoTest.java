package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import org.junit.jupiter.api.Test;

class ProfanityDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    ProfanityDto dto = new ProfanityDto();
    assertNull(dto.getSingular());
    assertNull(dto.getPlural());
    assertFalse(dto.getContains());
  }

  @Test
  void fromEntityCopiesAllFields() {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("prof-1");
    entity.setSingular("badword");
    entity.setPlural("badwords");
    entity.setContains(true);

    ProfanityDto dto = new ProfanityDto(entity);

    assertEquals("prof-1", dto.getId());
    assertEquals("badword", dto.getSingular());
    assertEquals("badwords", dto.getPlural());
    assertTrue(dto.getContains());
  }

  @Test
  void settersUpdateFields() {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("foo");
    dto.setPlural("foos");
    dto.setContains(true);

    assertEquals("foo", dto.getSingular());
    assertEquals("foos", dto.getPlural());
    assertTrue(dto.getContains());
  }
}
