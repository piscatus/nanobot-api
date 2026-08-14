package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.ProfanityDto;
import org.junit.jupiter.api.Test;

class ProfanityEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    ProfanityEntity entity = new ProfanityEntity();
    assertNull(entity.getSingular());
    assertNull(entity.getPlural());
    assertFalse(entity.getContains());
  }

  @Test
  void fromDtoCopiesAllFields() {
    ProfanityDto dto = new ProfanityDto();
    dto.setId("prof-1");
    dto.setSingular("badword");
    dto.setPlural("badwords");
    dto.setContains(true);

    ProfanityEntity entity = new ProfanityEntity(dto);

    assertEquals("prof-1", entity.getId());
    assertEquals("badword", entity.getSingular());
    assertEquals("badwords", entity.getPlural());
    assertTrue(entity.getContains());
  }

  @Test
  void settersUpdateFields() {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setSingular("foo");
    entity.setPlural("foos");
    entity.setContains(true);

    assertEquals("foo", entity.getSingular());
    assertEquals("foos", entity.getPlural());
    assertTrue(entity.getContains());
  }
}
