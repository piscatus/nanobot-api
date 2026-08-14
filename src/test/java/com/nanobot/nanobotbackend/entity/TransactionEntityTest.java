package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Date;
import org.junit.jupiter.api.Test;

class TransactionEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    TransactionEntity entity = new TransactionEntity();
    assertNull(entity.getCommand());
    assertNull(entity.getGuildId());
  }

  @Test
  void settersUpdateFields() {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("t-1");
    entity.setBlockHash("hash123");
    entity.setPrimaryUserId("u1");
    entity.setGuildId("g1");
    entity.setChannelId("ch1");
    entity.setCommand("drop");
    entity.setInput("100");
    entity.setTimestamp(new Date());

    assertEquals("t-1", entity.getId());
    assertEquals("hash123", entity.getBlockHash());
    assertEquals("u1", entity.getPrimaryUserId());
    assertEquals("g1", entity.getGuildId());
    assertEquals("ch1", entity.getChannelId());
    assertEquals("drop", entity.getCommand());
    assertEquals("100", entity.getInput());
  }
}
