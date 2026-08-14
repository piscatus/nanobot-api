package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.VerificationEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

class VerificationDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    VerificationDto dto = new VerificationDto();
    assertNull(dto.getUserId());
    assertNull(dto.getTimestamp());
  }

  @Test
  void fromEntityCopiesAllFields() {
    VerificationEntity entity = new VerificationEntity();
    entity.setId("v-1");
    entity.setUserId("u1");
    Date ts = new Date();
    entity.setTimestamp(ts);

    VerificationDto dto = new VerificationDto(entity);

    assertEquals("v-1", dto.getId());
    assertEquals("u1", dto.getUserId());
    assertEquals(ts, dto.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("u2");
    Date ts = new Date();
    dto.setTimestamp(ts);

    assertEquals("u2", dto.getUserId());
    assertEquals(ts, dto.getTimestamp());
  }
}
