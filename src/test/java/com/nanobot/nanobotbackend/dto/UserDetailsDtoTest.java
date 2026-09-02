package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import org.junit.jupiter.api.Test;

class UserDetailsDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    UserDetailsDto dto = new UserDetailsDto();
    assertNull(dto.getUserId());
    assertNull(dto.getSubordinateUserId());
    assertNull(dto.getStatus());
    assertNull(dto.getSeed());
    assertNull(dto.getIndex());
    assertNull(dto.getPrivateKey());
  }

  @Test
  void fullConstructorSetsAllFields() {
    StatusDto status = StatusDto.ACTIVE;
    UserDetailsDto dto = new UserDetailsDto("u1", "sub1", status, "seed123");

    assertEquals("u1", dto.getUserId());
    assertEquals("sub1", dto.getSubordinateUserId());
    assertEquals(status, dto.getStatus());
    assertEquals("seed123", dto.getSeed());
  }

  @Test
  void fromEntityCopiesAllFields() {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setId("e-1");
    entity.setUserId("u1");
    entity.setSubordinateUserId("sub1");
    entity.setSeed("secret");
    entity.setIndex(7L);
    entity.setPrivateKey("pk-hex");
    StatusDto status = StatusDto.ACTIVE;
    entity.setStatus(status);

    UserDetailsDto dto = new UserDetailsDto(entity);

    assertEquals("e-1", dto.getId());
    assertEquals("u1", dto.getUserId());
    assertEquals("sub1", dto.getSubordinateUserId());
    assertEquals("secret", dto.getSeed());
    assertEquals(7L, dto.getIndex());
    assertEquals("pk-hex", dto.getPrivateKey());
    assertEquals(status, dto.getStatus());
  }

  @Test
  void removeSensitiveDataClearsSeedAndPrivateKey() {
    UserDetailsDto dto = new UserDetailsDto("u1", "sub1", StatusDto.ACTIVE, "seed");
    dto.setIndex(3L);
    dto.setPrivateKey("pk-hex");
    assertEquals("seed", dto.getSeed());
    assertEquals("pk-hex", dto.getPrivateKey());

    dto.removeSensitiveData();
    assertNull(dto.getSeed());
    assertNull(dto.getPrivateKey());
    assertEquals(3L, dto.getIndex());
  }

  @Test
  void settersUpdateFields() {
    UserDetailsDto dto = new UserDetailsDto();
    StatusDto status = StatusDto.ACTIVE;

    dto.setUserId("u2");
    dto.setSubordinateUserId("sub2");
    dto.setStatus(status);
    dto.setSeed("new-seed");
    dto.setIndex(9L);
    dto.setPrivateKey("new-pk");

    assertEquals("u2", dto.getUserId());
    assertEquals("sub2", dto.getSubordinateUserId());
    assertEquals(status, dto.getStatus());
    assertEquals("new-seed", dto.getSeed());
    assertEquals(9L, dto.getIndex());
    assertEquals("new-pk", dto.getPrivateKey());
  }
}
