package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import java.util.Collections;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.dao.OptimisticLockingFailureException;

/**
 * Tests retry/recover behavior when {@link OptimisticLockingFailureException}
 * occurs during executeTransfer (e.g. concurrent updates during /fish, transfers, drop cleanups).
 *
 * <p>Recover behavior is tested by calling {@code recoverOptimisticLock} directly.
 * With the full Spring context, {@code @Retryable} on {@code executeTransfer} performs
 * up to 3 attempts with backoff before invoking this recover method.
 */
class TransferExecutorServiceImplTest {

  @Mock
  private TransferService transferService;

  @Mock
  private GuildWalletsService guildWalletsService;

  @Mock
  private UserItemsService userItemsService;

  @Mock
  private UserWalletsService userWalletsService;

  @Mock
  private TransactionsService transactionsService;

  @InjectMocks
  private TransferExecutorServiceImpl transferExecutorService;

  @BeforeEach
  void setUp() {
    MockitoAnnotations.openMocks(this);
  }

  @Test
  void recoverOptimisticLock_returnsFriendlyErrorMessage() {
    OptimisticLockingFailureException ex =
      new OptimisticLockingFailureException("version conflict");
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    TransferResponseDto result = transferExecutorService.recoverOptimisticLock(
      ex,
      "fish",
      "guild1",
      "channel1",
      "user1",
      null,
      List.of("user2"),
      null,
      request
    );

    assertNotNull(result);
    assertNotNull(result.getErrorMessage());
    assertTrue(
      result.getErrorMessage().contains("temporary conflict"),
      "Message should mention conflict: " + result.getErrorMessage()
    );
    assertTrue(
      result.getErrorMessage().contains("please try again"),
      "Message should ask to try again: " + result.getErrorMessage()
    );
  }

  @Test
  void executeTransfer_throwsOptimisticLockingFailureWhenSaveFails() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    TransferResponseDto successResponse = new TransferResponseDto();
    successResponse.setPrimaryTransfer(request.getPrimaryTransfer());
    successResponse.setUserWalletQuantities(walletSet);
    successResponse.setUserItemQuantities(itemSet);
    successResponse.setGuildWalletQuantities(null);

    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenReturn(successResponse);
    when(userWalletsService.getUsersWallets(any()))
      .thenReturn(List.of(new UserWalletsEntity("user1")));
    when(userItemsService.getUsersItems(any()))
      .thenReturn(List.of(new UserItemsEntity("user1")));
    doThrow(new OptimisticLockingFailureException("version conflict"))
      .when(userWalletsService).saveWallet(any(UserWalletsEntity.class));

    // Without Spring retry proxy, the exception propagates. With @Retryable in production,
    // the call is retried up to 3 times before recoverOptimisticLock is used.
    assertThrows(
      OptimisticLockingFailureException.class,
      () ->
        transferExecutorService.executeTransfer(
          "fish",
          "guild1",
          "channel1",
          "user1",
          null,
          List.of("user2"),
          null,
          request
        )
    );
    verify(userWalletsService).saveWallet(any(UserWalletsEntity.class));
  }

  @Test
  void executeTransfer_succeedsWhenSavesDoNotThrow() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    TransferResponseDto successResponse = new TransferResponseDto();
    successResponse.setPrimaryTransfer(request.getPrimaryTransfer());
    successResponse.setUserWalletQuantities(walletSet);
    successResponse.setUserItemQuantities(itemSet);
    successResponse.setGuildWalletQuantities(null);

    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenReturn(successResponse);
    when(userWalletsService.getUsersWallets(any()))
      .thenReturn(List.of(new UserWalletsEntity("user1")));
    when(userItemsService.getUsersItems(any()))
      .thenReturn(List.of(new UserItemsEntity("user1")));
    TransactionEntity txEntity = new TransactionEntity();
    txEntity.setId("tx-1");
    when(transactionsService.createTransaction(any())).thenReturn(txEntity);

    TransferResponseDto result = transferExecutorService.executeTransfer(
      "fish",
      "guild1",
      "channel1",
      "user1",
      null,
      List.of("user2"),
      null,
      request
    );

    assertNull(result.getErrorMessage());
    assertEquals("tx-1", result.getTransactionId());
    verify(userWalletsService).saveWallet(any(UserWalletsEntity.class));
  }

  @Test
  void executeTransferReturnsEarlyWhenPrimaryTransferFails() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    TransferResponseDto failedPrimary = new TransferResponseDto();
    failedPrimary.setErrorMessage("insufficient balance");

    when(userWalletsService.getUsersWallets(any()))
      .thenReturn(List.of(new UserWalletsEntity("user1")));
    when(userItemsService.getUsersItems(any()))
      .thenReturn(List.of(new UserItemsEntity("user1")));
    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenReturn(failedPrimary);

    TransferResponseDto result = transferExecutorService.executeTransfer(
      "gift",
      "guild1",
      "channel1",
      "user1",
      null,
      List.of("user2"),
      null,
      request
    );

    assertEquals("insufficient balance", result.getErrorMessage());
    verify(userWalletsService, never()).saveWallet(any(UserWalletsEntity.class));
    verify(userItemsService, never()).saveItem(any(UserItemsEntity.class));
    verify(transactionsService, never()).createTransaction(any());
  }

  @Test
  void executeTransferRunsSecondaryFlowAndStoresSecondaryCompletions() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));
    request.setSecondaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    walletSet.add(new UserWalletsEntity("user2"));
    walletSet.add(new UserWalletsEntity("user3"));
    walletSet.add(new UserWalletsEntity("user4"));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    itemSet.add(new UserItemsEntity("user2"));
    itemSet.add(new UserItemsEntity("user3"));
    itemSet.add(new UserItemsEntity("user4"));

    TransferResponseDto primarySuccess = new TransferResponseDto();
    primarySuccess.setUserWalletQuantities(walletSet);
    primarySuccess.setUserItemQuantities(itemSet);
    primarySuccess.setCompletedPrimaryTransfers(Optional.of(Map.of("user2", new TransferDto())));

    TransferResponseDto secondarySuccess = new TransferResponseDto();
    secondarySuccess.setUserWalletQuantities(walletSet);
    secondarySuccess.setUserItemQuantities(itemSet);
    secondarySuccess.setCompletedSecondaryTransfers(
      Optional.of(Map.of("user4", new TransferDto()))
    );

    when(userWalletsService.getUsersWallets(any())).thenReturn(Collections.emptyList());
    when(userItemsService.getUsersItems(any())).thenReturn(Collections.emptyList());
    when(transferService.transfer(any(), any(), any(), any(), eq(true)))
      .thenReturn(primarySuccess);
    when(transferService.transfer(any(), any(), any(), any(), eq(false)))
      .thenReturn(secondarySuccess);
    TransactionEntity txEntity = new TransactionEntity();
    txEntity.setId("tx-secondary");
    when(transactionsService.createTransaction(any())).thenReturn(txEntity);

    TransferResponseDto result = transferExecutorService.executeTransfer(
      "trade",
      "guild1",
      "channel1",
      "user1",
      "user3",
      List.of("user2"),
      List.of("user4"),
      request
    );

    assertNull(result.getErrorMessage());
    assertTrue(result.getCompletedSecondaryTransfers().isPresent());
    assertTrue(result.getCompletedSecondaryTransfers().get().containsKey("user4"));
    verify(transferService).transfer(any(), eq("user1"), any(), any(), eq(true));
    verify(transferService).transfer(any(), eq("user3"), any(), any(), eq(false));
  }

  @Test
  void executeTransferReturnsEarlyWhenSecondaryTransferFails() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));
    request.setSecondaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    walletSet.add(new UserWalletsEntity("user2"));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    itemSet.add(new UserItemsEntity("user2"));

    TransferResponseDto primarySuccess = new TransferResponseDto();
    primarySuccess.setUserWalletQuantities(walletSet);
    primarySuccess.setUserItemQuantities(itemSet);
    primarySuccess.setGuildWalletQuantities(new HashSet<GuildWalletsEntity>());

    TransferResponseDto failedSecondary = new TransferResponseDto();
    failedSecondary.setErrorMessage("secondary transfer failed");

    when(userWalletsService.getUsersWallets(any())).thenReturn(Collections.emptyList());
    when(userItemsService.getUsersItems(any())).thenReturn(Collections.emptyList());
    when(guildWalletsService.getGuildsWallets(any())).thenReturn(Collections.emptyList());
    when(transferService.transfer(any(), any(), any(), any(), eq(true)))
      .thenReturn(primarySuccess);
    when(transferService.transfer(any(), any(), any(), any(), eq(false)))
      .thenReturn(failedSecondary);

    TransferResponseDto result = transferExecutorService.executeTransfer(
      "trade",
      "guild1",
      "channel1",
      "user1",
      "user3",
      List.of("user2"),
      List.of("user4"),
      request
    );

    assertEquals("secondary transfer failed", result.getErrorMessage());
    verify(userWalletsService, never()).saveWallet(any(UserWalletsEntity.class));
    verify(userItemsService, never()).saveItem(any(UserItemsEntity.class));
    verify(guildWalletsService, never()).saveWallet(any(GuildWalletsEntity.class));
    verify(transactionsService, never()).createTransaction(any());
  }

  @Test
  void executeTransferCreatesDefaultWalletAndItemEntitiesWhenMissing() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    when(userWalletsService.getUsersWallets(any())).thenReturn(Collections.emptyList());
    when(userItemsService.getUsersItems(any())).thenReturn(Collections.emptyList());
    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenAnswer(invocation -> {
        TransferResponseDto dto = invocation.getArgument(3);
        dto.setCompletedPrimaryTransfers(Optional.of(Collections.emptyMap()));
        return dto;
      });
    TransactionEntity txEntity = new TransactionEntity();
    txEntity.setId("tx-created-defaults");
    when(transactionsService.createTransaction(any())).thenReturn(txEntity);

    transferExecutorService.executeTransfer(
      "rain",
      "guild1",
      "channel1",
      "userA",
      null,
      List.of("userB"),
      null,
      request
    );

    ArgumentCaptor<UserWalletsEntity> walletCaptor = ArgumentCaptor.forClass(
      UserWalletsEntity.class
    );
    ArgumentCaptor<UserItemsEntity> itemCaptor = ArgumentCaptor.forClass(
      UserItemsEntity.class
    );
    verify(userWalletsService, atLeastOnce()).saveWallet(walletCaptor.capture());
    verify(userItemsService, atLeastOnce()).saveItem(itemCaptor.capture());

    Set<String> savedWalletUsers = new HashSet<>();
    for (UserWalletsEntity entity : walletCaptor.getAllValues()) {
      savedWalletUsers.add(entity.getUserId());
    }
    Set<String> savedItemUsers = new HashSet<>();
    for (UserItemsEntity entity : itemCaptor.getAllValues()) {
      savedItemUsers.add(entity.getUserId());
    }

    assertTrue(savedWalletUsers.contains("userA"));
    assertTrue(savedWalletUsers.contains("userB"));
    assertTrue(savedItemUsers.contains("userA"));
    assertTrue(savedItemUsers.contains("userB"));
  }

  @Test
  void executeTransferBuildsTransactionWithCommandAndParticipants() {
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    walletSet.add(new UserWalletsEntity("user2"));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    itemSet.add(new UserItemsEntity("user2"));
    TransferResponseDto successResponse = new TransferResponseDto();
    successResponse.setPrimaryTransfer(request.getPrimaryTransfer());
    successResponse.setUserWalletQuantities(walletSet);
    successResponse.setUserItemQuantities(itemSet);
    successResponse.setGuildWalletQuantities(null);

    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenReturn(successResponse);
    when(userWalletsService.getUsersWallets(any()))
      .thenReturn(List.of(new UserWalletsEntity("user1")));
    when(userItemsService.getUsersItems(any()))
      .thenReturn(List.of(new UserItemsEntity("user1")));
    TransactionEntity txEntity = new TransactionEntity();
    txEntity.setId("tx-captured");
    when(transactionsService.createTransaction(any())).thenReturn(txEntity);

    transferExecutorService.executeTransfer(
      "gift",
      "guild-123",
      "channel-999",
      "user-1",
      null,
      List.of("user-2"),
      null,
      request
    );

    ArgumentCaptor<TransactionDto> transactionCaptor =
      ArgumentCaptor.forClass(TransactionDto.class);
    verify(transactionsService).createTransaction(transactionCaptor.capture());
    assertEquals("gift", transactionCaptor.getValue().getCommand());
    assertEquals("guild-123", transactionCaptor.getValue().getGuildId());
    assertEquals("channel-999", transactionCaptor.getValue().getChannelId());
    assertEquals("user-1", transactionCaptor.getValue().getPrimaryUserId());
    assertEquals(List.of("user-2"), transactionCaptor.getValue().getPrimaryReceiverIds());
  }

  /**
   * TRANSFERS.md: Executor passes all entities to saveWallet/saveItem.
   * UserWalletsServiceImpl/UserItemsServiceImpl filter out "0" and BOT_USER_ID
   * and do not persist them. This test verifies the executor invokes save
   * for every entity (filtering happens in the service layer).
   */
  @Test
  void executeTransferInvokesSaveWalletForAllEntitiesIncludingSystemUserAndBot() {
    String botUserId = System.getenv("BOT_USER_ID");
    if (botUserId == null) {
      botUserId = "test-bot-user-id";
    }
    TransferResponseDto request = new TransferResponseDto();
    request.setPrimaryTransfer(new TransferDto(Collections.emptyList(), Collections.emptyList()));

    Set<UserWalletsEntity> walletSet = new HashSet<>();
    walletSet.add(new UserWalletsEntity("user1"));
    walletSet.add(new UserWalletsEntity("0"));
    walletSet.add(new UserWalletsEntity(botUserId));
    Set<UserItemsEntity> itemSet = new HashSet<>();
    itemSet.add(new UserItemsEntity("user1"));
    itemSet.add(new UserItemsEntity("0"));
    itemSet.add(new UserItemsEntity(botUserId));
    TransferResponseDto successResponse = new TransferResponseDto();
    successResponse.setPrimaryTransfer(request.getPrimaryTransfer());
    successResponse.setUserWalletQuantities(walletSet);
    successResponse.setUserItemQuantities(itemSet);
    successResponse.setGuildWalletQuantities(null);

    when(transferService.transfer(any(), any(), any(), any(), anyBoolean()))
      .thenReturn(successResponse);
    when(userWalletsService.getUsersWallets(any())).thenReturn(Collections.emptyList());
    when(userItemsService.getUsersItems(any())).thenReturn(Collections.emptyList());
    TransactionEntity txEntity = new TransactionEntity();
    txEntity.setId("tx-special-users");
    when(transactionsService.createTransaction(any())).thenReturn(txEntity);

    transferExecutorService.executeTransfer(
      "gift",
      "guild1",
      "channel1",
      "user1",
      null,
      List.of("0", botUserId),
      null,
      request
    );

    ArgumentCaptor<UserWalletsEntity> walletCaptor = ArgumentCaptor.forClass(UserWalletsEntity.class);
    ArgumentCaptor<UserItemsEntity> itemCaptor = ArgumentCaptor.forClass(UserItemsEntity.class);
    verify(userWalletsService, atLeastOnce()).saveWallet(walletCaptor.capture());
    verify(userItemsService, atLeastOnce()).saveItem(itemCaptor.capture());

    Set<String> walletUserIds = new HashSet<>();
    for (UserWalletsEntity e : walletCaptor.getAllValues()) {
      walletUserIds.add(e.getUserId());
    }
    Set<String> itemUserIds = new HashSet<>();
    for (UserItemsEntity e : itemCaptor.getAllValues()) {
      itemUserIds.add(e.getUserId());
    }
    assertTrue(walletUserIds.contains("user1"));
    assertTrue(walletUserIds.contains("0"));
    assertTrue(walletUserIds.contains(botUserId));
    assertTrue(itemUserIds.contains("user1"));
    assertTrue(itemUserIds.contains("0"));
    assertTrue(itemUserIds.contains(botUserId));
  }
}
