package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;

import java.util.Collections;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransactionsResponseDtoTest {

  @Test
  void defaultConstructorCreatesEmptyDto() {
    TransactionsResponseDto dto = new TransactionsResponseDto();
    assertNull(dto.getBonuses());
    assertNull(dto.getCurrencies());
    assertNull(dto.getCreatures());
    assertNull(dto.getTransactions());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    TransactionsResponseDto dto = new TransactionsResponseDto("Transactions failed");
    assertEquals("Transactions failed", dto.getErrorMessage());
  }

  @Test
  void settersUpdateFields() {
    TransactionsResponseDto dto = new TransactionsResponseDto();
    List<ItemDto> bonuses = Collections.singletonList(new ItemDto());
    List<CurrencyDto> currencies = Collections.singletonList(new CurrencyDto());
    List<CreatureDto> creatures = Collections.singletonList(new CreatureDto());
    List<TransactionDto> transactions = Collections.singletonList(new TransactionDto());

    dto.setBonuses(bonuses);
    dto.setCurrencies(currencies);
    dto.setCreatures(creatures);
    dto.setTransactions(transactions);

    assertEquals(bonuses, dto.getBonuses());
    assertEquals(currencies, dto.getCurrencies());
    assertEquals(creatures, dto.getCreatures());
    assertEquals(transactions, dto.getTransactions());
  }
}
