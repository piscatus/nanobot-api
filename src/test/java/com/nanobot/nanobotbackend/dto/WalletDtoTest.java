package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import org.junit.jupiter.api.Test;

class WalletDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    WalletDto dto = new WalletDto();
    assertEquals(null, dto.getTicker());
    assertEquals(null, dto.getAddress());
    assertEquals(null, dto.getRaw());
    assertFalse(dto.getRequired());
  }

  @Test
  void tickerRawConstructorSetsFields() {
    WalletDto dto = new WalletDto("BTC", "raw123");
    assertEquals("BTC", dto.getTicker());
    assertEquals("raw123", dto.getRaw());
    assertEquals(null, dto.getAddress());
  }

  @Test
  void tickerAddressRawConstructorSetsFields() {
    WalletDto dto = new WalletDto("ETH", "0x123", "raw456");
    assertEquals("ETH", dto.getTicker());
    assertEquals("0x123", dto.getAddress());
    assertEquals("raw456", dto.getRaw());
  }

  @Test
  void tickerRawRequiredConstructorSetsFields() {
    WalletDto dto = new WalletDto("SOL", "raw789", true);
    assertEquals("SOL", dto.getTicker());
    assertEquals("raw789", dto.getRaw());
    assertTrue(dto.getRequired());
  }

  @Test
  void copyConstructorCopiesAllFields() {
    WalletDto source = new WalletDto("BTC", "addr", "raw");
    source.setId("id-1");
    source.setRequired(true);

    WalletDto dto = new WalletDto(source);

    assertEquals("id-1", dto.getId());
    assertEquals("BTC", dto.getTicker());
    assertEquals("raw", dto.getRaw());
    assertTrue(dto.getRequired());
  }

  @Test
  void settersUpdateFields() {
    WalletDto dto = new WalletDto();
    dto.setTicker("XRP");
    dto.setAddress("addr");
    dto.setRaw("raw");
    dto.setRequired(true);

    assertEquals("XRP", dto.getTicker());
    assertEquals("addr", dto.getAddress());
    assertEquals("raw", dto.getRaw());
    assertTrue(dto.getRequired());
  }
}
