package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.entity.DropEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DropDtoTest {

  @Test
  void constructorSetsAllFields() {
    Date endTime = new Date();
    Date startTime = new Date();
    TransferDto transfer = new TransferDto();

    DropDto dto = new DropDto(
      "ch1",
      60,
      endTime,
      "guild1",
      "10 xno",
      "5",
      "msg",
      2,
      "role1",
      startTime,
      transfer,
      "user1"
    );

    assertEquals("ch1", dto.getChannelId());
    assertEquals(60, dto.getDuration());
    assertEquals(endTime, dto.getEndTime());
    assertEquals("guild1", dto.getGuildId());
    assertEquals("10 xno", dto.getInput());
    assertEquals("5", dto.getMaximumEntries());
    assertEquals("msg", dto.getMessageData());
    assertEquals(2, dto.getNumberWinners());
    assertEquals("role1", dto.getRequiredRole());
    assertEquals(startTime, dto.getStartTime());
    assertEquals(transfer, dto.getTransfer());
    assertEquals("user1", dto.getUserId());
  }

  @Test
  void fromEntityCopiesAllFields() {
    DropEntity entity = new DropEntity();
    entity.setId("drop-1");
    entity.setChannelId("ch1");
    entity.setDuration(30);
    entity.setGuildId("g1");
    entity.setInput("5 ban");
    entity.setMaximumEntries("10");
    entity.setMessageData("BAN");
    entity.setMessageId("msg-1");
    entity.setNumberWinners(1);
    entity.setRequiredRole("role1");
    entity.setUserId("u1");

    DropDto dto = new DropDto(entity);

    assertEquals("drop-1", dto.getId());
    assertEquals("ch1", dto.getChannelId());
    assertEquals(30, dto.getDuration());
    assertEquals("g1", dto.getGuildId());
    assertEquals("5 ban", dto.getInput());
    assertEquals("10", dto.getMaximumEntries());
    assertEquals("BAN", dto.getMessageData());
    assertEquals("msg-1", dto.getMessageId());
    assertEquals(1, dto.getNumberWinners());
    assertEquals("role1", dto.getRequiredRole());
    assertEquals("u1", dto.getUserId());
  }

  @Test
  void settersUpdateFields() {
    DropDto dto = new DropDto();
    dto.setChannelId("ch2");
    dto.setDuration(90);
    dto.setGuildId("g2");
    dto.setInput("20 xno");
    dto.setMaximumEntries("15");
    dto.setMessageId("msg-2");
    dto.setUserId("u2");

    assertEquals("ch2", dto.getChannelId());
    assertEquals(90, dto.getDuration());
    assertEquals("g2", dto.getGuildId());
    assertEquals("20 xno", dto.getInput());
    assertEquals("15", dto.getMaximumEntries());
    assertEquals("msg-2", dto.getMessageId());
    assertEquals("u2", dto.getUserId());
  }

  @Test
  void defaultConstructorCreatesEmptyDto() {
    DropDto dto = new DropDto();
    assertNull(dto.getChannelId());
    assertNull(dto.getGuildId());
    assertNull(dto.getUserId());
  }
}
