package com.nanobot.nanobotbackend.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import org.junit.jupiter.api.Test;

class CurrencyEntityTest {

  @Test
  void defaultConstructorCreatesEmptyEntity() {
    CurrencyEntity entity = new CurrencyEntity();
    assertEquals(false, entity.getEnabled());
    assertEquals(false, entity.getProcessDeposits());
  }

  @Test
  void fromDtoCopiesAllFields() {
    CurrencyDto dto = new CurrencyDto();
    dto.setId("c-1");
    dto.setTicker("NANO");
    dto.setName("Nano");
    dto.setAddress("addr");
    dto.setNodeUrl("http://node");
    dto.setEnabled(true);
    dto.setProcessDeposits(true);
    dto.setProcessWithdrawals(false);
    dto.setEmoji("🌿");
    dto.setColor("#4A90E2");
    dto.setPrecision("30");
    dto.setMinimumDrop("0.01");
    dto.setMinimumGift("0.1");
    dto.setMinimumRain("1");

    CurrencyEntity entity = new CurrencyEntity(dto);

    assertEquals("c-1", entity.getId());
    assertEquals("NANO", entity.getTicker());
    assertEquals("Nano", entity.getName());
    assertEquals("addr", entity.getAddress());
    assertEquals("http://node", entity.getNodeUrl());
    assertTrue(entity.getEnabled());
    assertTrue(entity.getProcessDeposits());
    assertFalse(entity.getProcessWithdrawals());
    assertEquals("🌿", entity.getEmoji());
    assertEquals("#4A90E2", entity.getColor());
    assertEquals("30", entity.getPrecision());
    assertEquals("0.01", entity.getMinimumDrop());
    assertEquals("0.1", entity.getMinimumGift());
    assertEquals("1", entity.getMinimumRain());
  }

  @Test
  void settersUpdateFields() {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("BAN");
    entity.setName("Banano");
    entity.setEnabled(true);

    assertEquals("BAN", entity.getTicker());
    assertEquals("Banano", entity.getName());
    assertTrue(entity.getEnabled());
  }
}
