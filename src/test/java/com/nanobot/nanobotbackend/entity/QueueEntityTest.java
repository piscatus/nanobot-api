package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;

import com.nanobot.nanobotbackend.dto.LevelDto;
import com.nanobot.nanobotbackend.dto.QueueDto;
import java.util.Date;
import org.junit.jupiter.api.Test;

class QueueEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    QueueEntity entity = new QueueEntity();
    assertNull(entity.getUserId());
    assertNull(entity.getSourceAddress());
    assertNull(entity.getTargetAddress());
    assertFalse(entity.getProcessed());
  }

  @Test
  void fromDtoCopiesAllFields() {
    Date ts = new Date();
    LevelDto level = LevelDto.RECEIVE;
    QueueDto dto = new QueueDto("u1", "src", "tgt", level, "hash", "raw", "NANO", true, "seed", ts, "tx-1");
    dto.setId("q-1");

    QueueEntity entity = new QueueEntity(dto);

    assertEquals("q-1", entity.getId());
    assertEquals("u1", entity.getUserId());
    assertEquals("src", entity.getSourceAddress());
    assertEquals("tgt", entity.getTargetAddress());
    assertEquals(level, entity.getLevel());
    assertEquals("hash", entity.getBlockHash());
    assertEquals("raw", entity.getRaw());
    assertEquals("NANO", entity.getTicker());
    assertEquals(true, entity.getProcessed());
    assertEquals("seed", entity.getSeed());
    assertEquals(ts, entity.getTimestamp());
    assertEquals("tx-1", entity.getTransactionId());
  }

  @Test
  void settersUpdateFields() {
    QueueEntity entity = new QueueEntity();
    entity.setUserId("u2");
    entity.setSourceAddress("src2");
    entity.setTargetAddress("tgt2");
    entity.setProcessed(true);
    entity.setTransactionId("tx-2");

    assertEquals("u2", entity.getUserId());
    assertEquals("src2", entity.getSourceAddress());
    assertEquals("tgt2", entity.getTargetAddress());
    assertEquals(true, entity.getProcessed());
    assertEquals("tx-2", entity.getTransactionId());
  }
}
