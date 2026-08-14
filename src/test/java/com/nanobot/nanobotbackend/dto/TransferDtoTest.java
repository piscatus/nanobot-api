package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.*;

import java.util.ArrayList;
import java.util.List;
import org.junit.jupiter.api.Test;

class TransferDtoTest {

  @Test
  void defaultConstructorInitializesEmptyLists() {
    TransferDto dto = new TransferDto();

    assertNotNull(dto.getWallets());
    assertNotNull(dto.getItems());
    assertTrue(dto.getWallets().isEmpty());
    assertTrue(dto.getItems().isEmpty());
    assertFalse(dto.getPricey());
  }

  @Test
  void constructorWithListsSetsWalletsAndItems() {
    List<WalletDto> wallets = List.of(new WalletDto("XNO", "100"));
    List<ItemDto> items = List.of(new ItemDto("SHRIMP", 5));

    TransferDto dto = new TransferDto(wallets, items);

    assertEquals(wallets, dto.getWallets());
    assertEquals(items, dto.getItems());
    assertFalse(dto.getPricey());
  }

  @Test
  void settersUpdateFields() {
    TransferDto dto = new TransferDto();
    dto.setPricey(true);
    dto.setWallets(new ArrayList<>(List.of(new WalletDto("BAN", "50"))));
    dto.setItems(new ArrayList<>(List.of(new ItemDto("CRAB", 2))));

    assertTrue(dto.getPricey());
    assertEquals(1, dto.getWallets().size());
    assertEquals("BAN", dto.getWallets().get(0).getTicker());
    assertEquals(1, dto.getItems().size());
    assertEquals("CRAB", dto.getItems().get(0).getName());
  }
}
