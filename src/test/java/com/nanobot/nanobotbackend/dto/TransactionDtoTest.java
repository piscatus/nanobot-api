package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.TransactionEntity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.junit.jupiter.api.Test;

class TransactionDtoTest {

  @Test
  void constructorFromTransferResponseDtoSetsCommandAndDefaults() {
    TransferResponseDto transfer = new TransferResponseDto();
    transfer.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));
    transfer.setUserWalletQuantities(new HashSet<>());
    transfer.setUserItemQuantities(new HashSet<>());

    TransactionDto dto = new TransactionDto("gift", transfer);

    assertEquals("gift", dto.getCommand());
    assertNotNull(dto.getCompletedPrimaryTransfers());
    assertTrue(dto.getCompletedPrimaryTransfers().isEmpty());
    assertNotNull(dto.getCompletedSecondaryTransfers());
    assertTrue(dto.getCompletedSecondaryTransfers().isEmpty());
    assertNotNull(dto.getCompletedPrimaryGuildTransfers());
    assertNotNull(dto.getCompletedSecondaryGuildTransfers());
    assertNotNull(dto.getGuildWalletHistories());
    assertNotNull(dto.getUserWalletHistories());
    assertNotNull(dto.getUserItemHistories());
    assertNotNull(dto.getUserWalletQuantities());
    assertNotNull(dto.getUserItemQuantities());
    assertNotNull(dto.getTimestamp());
  }

  @Test
  void constructorFromTransferResponseDtoPreservesCompletedTransfers() {
    TransferResponseDto transfer = new TransferResponseDto();
    transfer.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));
    transfer.setCompletedPrimaryTransfers(
      java.util.Optional.of(Map.of("r1", new TransferDto()))
    );
    transfer.setCompletedSecondaryTransfers(
      java.util.Optional.of(Map.of("r2", new TransferDto()))
    );
    transfer.setUserWalletQuantities(new HashSet<>());
    transfer.setUserItemQuantities(new HashSet<>());

    TransactionDto dto = new TransactionDto("gift", transfer);

    assertEquals(1, dto.getCompletedPrimaryTransfers().size());
    assertTrue(dto.getCompletedPrimaryTransfers().containsKey("r1"));
    assertEquals(1, dto.getCompletedSecondaryTransfers().size());
    assertTrue(dto.getCompletedSecondaryTransfers().containsKey("r2"));
  }

  @Test
  void constructorFromTransactionEntityCopiesFields() {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("tx-1");
    entity.setBlockHash("block123");
    entity.setGuildId("guild1");
    entity.setChannelId("ch1");
    entity.setPrimaryUserId("user1");
    entity.setPrimaryReceiverIds(List.of("r1"));
    entity.setCommand("fish");

    TransactionDto dto = new TransactionDto(entity);

    assertEquals("tx-1", dto.getId());
    assertEquals("block123", dto.getBlockHash());
    assertEquals("guild1", dto.getGuildId());
    assertEquals("ch1", dto.getChannelId());
    assertEquals("user1", dto.getPrimaryUserId());
    assertEquals(List.of("r1"), dto.getPrimaryReceiverIds());
    assertEquals("fish", dto.getCommand());
  }

  @Test
  void settersUpdateFields() {
    TransactionDto dto = new TransactionDto();

    dto.setBlockHash("hash2");
    dto.setGuildId("guild2");
    dto.setChannelId("ch2");
    dto.setPrimaryUserId("user2");
    dto.setSecondaryUserId("user3");
    dto.setPrimaryReceiverIds(List.of("r1", "r2"));
    dto.setSecondaryReceiverIds(List.of("r3"));
    dto.setCommand("rain");
    dto.setInput("1 nano");

    assertEquals("hash2", dto.getBlockHash());
    assertEquals("guild2", dto.getGuildId());
    assertEquals("ch2", dto.getChannelId());
    assertEquals("user2", dto.getPrimaryUserId());
    assertEquals("user3", dto.getSecondaryUserId());
    assertEquals(List.of("r1", "r2"), dto.getPrimaryReceiverIds());
    assertEquals(List.of("r3"), dto.getSecondaryReceiverIds());
    assertEquals("rain", dto.getCommand());
    assertEquals("1 nano", dto.getInput());
  }
}
