package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class DropEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    DropEntity entity = new DropEntity();
    assertNull(entity.getChannelId());
    assertNull(entity.getGuildId());
    assertNull(entity.getUserId());
  }

  @Test
  void fromDtoCopiesAllFields() {
    DropDto dto = new DropDto();
    dto.setId("drop-1");
    dto.setChannelId("ch1");
    dto.setDuration(60);
    Date endTime = new Date();
    dto.setEndTime(endTime);
    dto.setGuildId("g1");
    dto.setInput("100");
    dto.setMaximumEntries("5");
    dto.setMessageData("NANO");
    dto.setMessageId("msg1");
    dto.setNumberWinners(2);
    dto.setRequiredRole("role1");
    Date startTime = new Date();
    dto.setStartTime(startTime);
    dto.setTransfer(new TransferDto());
    dto.setUserId("u1");

    DropEntity entity = new DropEntity(dto);

    assertEquals("drop-1", entity.getId());
    assertEquals("ch1", entity.getChannelId());
    assertEquals(60, entity.getDuration());
    assertEquals(endTime, entity.getEndTime());
    assertEquals("g1", entity.getGuildId());
    assertEquals("100", entity.getInput());
    assertEquals("5", entity.getMaximumEntries());
    assertEquals("NANO", entity.getMessageData());
    assertEquals("msg1", entity.getMessageId());
    assertEquals(2, entity.getNumberWinners());
    assertEquals("role1", entity.getRequiredRole());
    assertEquals(startTime, entity.getStartTime());
    assertEquals("u1", entity.getUserId());
  }

  @Test
  void settersUpdateFields() {
    DropEntity entity = new DropEntity();
    entity.setChannelId("ch2");
    entity.setDuration(30);
    entity.setGuildId("g2");
    entity.setInput("50");
    entity.setUserId("u2");

    assertEquals("ch2", entity.getChannelId());
    assertEquals(30, entity.getDuration());
    assertEquals("g2", entity.getGuildId());
    assertEquals("50", entity.getInput());
    assertEquals("u2", entity.getUserId());
  }
}
