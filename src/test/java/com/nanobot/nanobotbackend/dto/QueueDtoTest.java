package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;

import com.nanobot.nanobotbackend.entity.QueueEntity;
import java.util.Date;
import org.junit.jupiter.api.Test;

class QueueDtoTest {

  @Test
  void fromEntityCopiesAllFields() {
    Date ts = new Date();
    LevelDto level = LevelDto.SEND;
    QueueEntity entity = new QueueEntity();
    entity.setId("q-1");
    entity.setUserId("u1");
    entity.setSourceAddress("src");
    entity.setTargetAddress("tgt");
    entity.setLevel(level);
    entity.setBlockHash("hash");
    entity.setRaw("raw");
    entity.setTicker("NANO");
    entity.setProcessed(true);
    entity.setSeed("seed");
    entity.setIndex(5L);
    entity.setPrivateKey("queue-pk");
    entity.setTimestamp(ts);
    entity.setTransactionId("tx-1");

    QueueDto dto = new QueueDto(entity);

    assertEquals("q-1", dto.getId());
    assertEquals("u1", dto.getUserId());
    assertEquals("src", dto.getSourceAddress());
    assertEquals("tgt", dto.getTargetAddress());
    assertEquals(level, dto.getLevel());
    assertEquals("hash", dto.getBlockHash());
    assertEquals("raw", dto.getRaw());
    assertEquals("NANO", dto.getTicker());
    assertEquals(true, dto.getProcessed());
    assertEquals("seed", dto.getSeed());
    assertEquals(5L, dto.getIndex());
    assertEquals("queue-pk", dto.getPrivateKey());
    assertEquals(ts, dto.getTimestamp());
    assertEquals("tx-1", dto.getTransactionId());
  }

  @Test
  void fullConstructorSetsFields() {
    Date ts = new Date();
    LevelDto level = LevelDto.UPDATE;
    QueueDto dto = new QueueDto("u1", "src", "tgt", level, "hash", "raw", "NANO", false, "seed", ts, "tx-1");

    assertEquals("u1", dto.getUserId());
    assertEquals("src", dto.getSourceAddress());
    assertEquals("tgt", dto.getTargetAddress());
    assertEquals(level, dto.getLevel());
    assertEquals("hash", dto.getBlockHash());
    assertEquals("raw", dto.getRaw());
    assertEquals("NANO", dto.getTicker());
    assertFalse(dto.getProcessed());
    assertEquals("seed", dto.getSeed());
    assertEquals(ts, dto.getTimestamp());
    assertEquals("tx-1", dto.getTransactionId());
  }

  @Test
  void settersUpdateFields() {
    QueueDto dto = new QueueDto("u1", "src", "tgt", LevelDto.RECEIVE, "h", "r", "NANO", false, "s", new Date(), "tx-1");
    dto.setUserId("u2");
    dto.setTransactionId("tx-2");
    dto.setIndex(8L);
    dto.setPrivateKey("pk-2");

    assertEquals("u2", dto.getUserId());
    assertEquals("tx-2", dto.getTransactionId());
    assertEquals(8L, dto.getIndex());
    assertEquals("pk-2", dto.getPrivateKey());
  }
}
