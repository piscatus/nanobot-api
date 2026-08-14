package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class RequestDtoTest {

  @Test
  void constructorSetsAllFields() {
    RequestDto dto = new RequestDto(
      "addr1",
      "ch1",
      true,
      "drop1",
      60,
      false,
      "guild1",
      "role1",
      "1 nano",
      "msgData",
      10,
      List.of("r1", "r2"),
      "user1",
      List.of("ur1"),
      List.of("role1"),
      5
    );

    assertEquals("addr1", dto.getAddress());
    assertEquals("ch1", dto.getChannelId());
    assertTrue(dto.getConfirmation());
    assertEquals("drop1", dto.getDropId());
    assertEquals(60, dto.getDuration());
    assertFalse(dto.getGlobal());
    assertEquals("guild1", dto.getGuildId());
    assertEquals("role1", dto.getRoleId());
    assertEquals("1 nano", dto.getInput());
    assertEquals("msgData", dto.getMessageData());
    assertEquals(10, dto.getRandom());
    assertEquals(List.of("r1", "r2"), dto.getReceiverIds());
    assertEquals("user1", dto.getUserId());
    assertEquals(List.of("ur1"), dto.getUserIdsWithRole());
    assertEquals(List.of("role1"), dto.getUserRoles());
    assertEquals(5, dto.getUsers());
  }

  @Test
  void settersUpdateFields() {
    RequestDto dto = new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );

    dto.setAddress("addr2");
    dto.setChannelId("ch2");
    dto.setConfirmation(true);
    dto.setDropId("drop2");
    dto.setDuration(30);
    dto.setGlobal(true);
    dto.setGuildId("guild2");
    dto.setRoleId("role2");
    dto.setInput("2 ban");
    dto.setMessageData("data2");
    dto.setRandom(20);
    dto.setReceiverIds(List.of("r3"));
    dto.setUserId("user2");
    dto.setUserIdsWithRole(List.of("ur2"));
    dto.setUserRoles(List.of("role2"));
    dto.setUsers(10);

    assertEquals("addr2", dto.getAddress());
    assertEquals("ch2", dto.getChannelId());
    assertTrue(dto.getConfirmation());
    assertEquals("drop2", dto.getDropId());
    assertEquals(30, dto.getDuration());
    assertTrue(dto.getGlobal());
    assertEquals("guild2", dto.getGuildId());
    assertEquals("role2", dto.getRoleId());
    assertEquals("2 ban", dto.getInput());
    assertEquals("data2", dto.getMessageData());
    assertEquals(20, dto.getRandom());
    assertEquals(List.of("r3"), dto.getReceiverIds());
    assertEquals("user2", dto.getUserId());
    assertEquals(List.of("ur2"), dto.getUserIdsWithRole());
    assertEquals(List.of("role2"), dto.getUserRoles());
    assertEquals(10, dto.getUsers());
  }

  @Test
  void inheritsBaseDtoId() {
    RequestDto dto = new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null,
      null
    );
    assertNull(dto.getId());
    dto.setId("id-123");
    assertEquals("id-123", dto.getId());
  }
}
