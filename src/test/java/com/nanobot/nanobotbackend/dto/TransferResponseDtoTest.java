package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransferResponseDtoTest {

  @Test
  void defaultConstructorSetsNullErrorMessage() {
    TransferResponseDto dto = new TransferResponseDto();
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    TransferResponseDto dto = new TransferResponseDto("Transfer failed");
    assertEquals("Transfer failed", dto.getErrorMessage());
  }

  @Test
  void settersAndGettersForTransferFields() {
    TransferResponseDto dto = new TransferResponseDto();
    TransferDto primary = new TransferDto(List.of(), List.of());
    TransferDto secondary = new TransferDto(List.of(), List.of());

    dto.setPrimaryTransfer(primary);
    dto.setSecondaryTransfer(secondary);
    dto.setBlockHash("block123");
    dto.setTransactionId("tx-456");
    dto.setInput("1 nano");
    dto.setConfirmation(true);

    assertEquals(primary, dto.getPrimaryTransfer());
    assertEquals(secondary, dto.getSecondaryTransfer());
    assertEquals("block123", dto.getBlockHash());
    assertEquals("tx-456", dto.getTransactionId());
    assertEquals("1 nano", dto.getInput());
    assertTrue(dto.getConfirmation());
  }

  @Test
  void completedTransfersOptionalFields() {
    TransferResponseDto dto = new TransferResponseDto();
    Map<String, TransferDto> primaryMap = Map.of("user1", new TransferDto(List.of(), List.of()));
    Map<String, TransferDto> secondaryMap = Map.of("user2", new TransferDto(List.of(), List.of()));

    dto.setCompletedPrimaryTransfers(Optional.of(primaryMap));
    dto.setCompletedSecondaryTransfers(Optional.of(secondaryMap));

    assertTrue(dto.getCompletedPrimaryTransfers().isPresent());
    assertEquals(1, dto.getCompletedPrimaryTransfers().get().size());
    assertTrue(dto.getCompletedSecondaryTransfers().isPresent());
    assertEquals(1, dto.getCompletedSecondaryTransfers().get().size());
  }

  @Test
  void userAndGuildQuantitiesSetters() {
    TransferResponseDto dto = new TransferResponseDto();
    Set<UserWalletsEntity> userWallets = Set.of(new UserWalletsEntity("u1"));
    Set<UserItemsEntity> userItems = Set.of(new UserItemsEntity("u1"));
    Set<GuildWalletsEntity> guildWallets = Set.of(new GuildWalletsEntity("g1"));

    dto.setUserWalletQuantities(userWallets);
    dto.setUserItemQuantities(userItems);
    dto.setGuildWalletQuantities(guildWallets);

    assertEquals(userWallets, dto.getUserWalletQuantities());
    assertEquals(userItems, dto.getUserItemQuantities());
    assertEquals(guildWallets, dto.getGuildWalletQuantities());
  }
}
