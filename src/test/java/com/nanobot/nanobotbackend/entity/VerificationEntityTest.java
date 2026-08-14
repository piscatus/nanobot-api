package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.VerificationDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class VerificationEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    VerificationEntity entity = new VerificationEntity();
    assertNull(entity.getUserId());
    assertNull(entity.getTimestamp());
  }

  @Test
  void twoArgConstructorSetsFields() {
    Date ts = new Date();
    VerificationEntity entity = new VerificationEntity("u1", ts);

    assertEquals("u1", entity.getUserId());
    assertEquals(ts, entity.getTimestamp());
  }

  @Test
  void fromDtoCopiesFields() {
    VerificationDto dto = new VerificationDto();
    dto.setId("v-1");
    dto.setUserId("u1");

    VerificationEntity entity = new VerificationEntity(dto);

    assertEquals("v-1", entity.getId());
    assertEquals("u1", entity.getUserId());
    assertNotNull(entity.getTimestamp());
  }

  @Test
  void settersUpdateFields() {
    VerificationEntity entity = new VerificationEntity();
    entity.setUserId("u2");
    Date ts = new Date();
    entity.setTimestamp(ts);

    assertEquals("u2", entity.getUserId());
    assertEquals(ts, entity.getTimestamp());
  }
}
