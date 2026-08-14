package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.dto.TransferDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Recover;
import org.springframework.retry.annotation.Retryable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class TransferExecutorServiceImpl implements TransferExecutorService {

  private final TransferService transferService;

  private final GuildWalletsService guildWalletsService;

  private final UserItemsService userItemsService;

  private final UserWalletsService userWalletsService;

  private final TransactionsService transactionsService;

  @Autowired
  public TransferExecutorServiceImpl(
    GuildWalletsService guildWalletsService,
    UserItemsService userItemsService,
    UserWalletsService userWalletsService,
    TransferService transferService,
    TransactionsService transactionsService
  ) {
    this.guildWalletsService = guildWalletsService;
    this.userItemsService = userItemsService;
    this.userWalletsService = userWalletsService;
    this.transferService = transferService;
    this.transactionsService = transactionsService;
  }

  private <T> void processEntities(
    Set<T> entities,
    Consumer<T> saveFunction,
    Consumer<Set<T>> logFunction
  ) {
    if (entities != null && !entities.isEmpty()) {
      for (T entity : entities) {
        saveFunction.accept(entity);
      }
      logFunction.accept(entities);
    }
  }

  private Set<GuildWalletsEntity> getGuildWallets(String guildId) {
    List<GuildWalletsEntity> guildWalletsList =
      guildWalletsService.getGuildsWallets(guildId);
    if (guildWalletsList.isEmpty()) {
      return new HashSet<>();
    }
    return new HashSet<>(guildWalletsList);
  }

  private Set<UserItemsEntity> getUserItems(List<String> userIds) {
    Set<UserItemsEntity> result = new HashSet<>();
    for (String userId : userIds) {
      List<UserItemsEntity> items = userItemsService.getUsersItems(userId);
      if (items.isEmpty()) {
        result.add(new UserItemsEntity(userId));
      } else {
        result.add(items.get(0));
      }
    }
    return result;
  }

  private Set<UserWalletsEntity> getUserWallets(List<String> userIds) {
    Set<UserWalletsEntity> result = new HashSet<>();
    for (String userId : userIds) {
      List<UserWalletsEntity> wallets = userWalletsService.getUsersWallets(
        userId
      );
      if (wallets.isEmpty()) {
        result.add(new UserWalletsEntity(userId));
      } else {
        result.add(wallets.get(0));
      }
    }
    return result;
  }

  private void logQuantities(
    boolean botInvolved,
    TransferResponseDto transferResponseDto
  ) {
    if (
      !transferResponseDto.getPrimaryTransfer().getWallets().isEmpty() ||
      (transferResponseDto.getSecondaryTransfer() != null &&
        !transferResponseDto.getSecondaryTransfer().getWallets().isEmpty())
    ) {
      LoggingUtil.logCurrentWallets(
        transferResponseDto.getUserWalletQuantities()
      );
    }
    if (
      !transferResponseDto.getPrimaryTransfer().getItems().isEmpty() ||
      (transferResponseDto.getSecondaryTransfer() != null &&
        !transferResponseDto.getSecondaryTransfer().getItems().isEmpty())
    ) {
      LoggingUtil.logCurrentItems(transferResponseDto.getUserItemQuantities());
    }
    if (botInvolved) LoggingUtil.logCurrentGuildWallets(
      transferResponseDto.getGuildWalletQuantities()
    );
  }

  @Retryable(
    retryFor = OptimisticLockingFailureException.class,
    maxAttempts = 3,
    backoff = @Backoff(delay = 100)
  )
  @Transactional(rollbackFor = OptimisticLockingFailureException.class)
  public TransferResponseDto executeTransfer(
    String command,
    String guildId,
    String channelId,
    String primaryUserId,
    String secondaryUserId,
    List<String> primaryReceiverIds,
    List<String> secondaryReceiverIds,
    TransferResponseDto transferResponseDto
  ) {
    try {
      Set<String> allUserIds = new HashSet<>();
      allUserIds.add(primaryUserId);
      allUserIds.addAll(primaryReceiverIds);

      TransferDto secondary = transferResponseDto.getSecondaryTransfer();
      if (secondary != null && secondaryUserId != null) {
        allUserIds.add(secondaryUserId);
        allUserIds.addAll(secondaryReceiverIds);
      }

      Set<UserWalletsEntity> allUserWallets = getUserWallets(
        new ArrayList<>(allUserIds)
      );
      Set<UserItemsEntity> allUserItems = getUserItems(
        new ArrayList<>(allUserIds)
      );

      String nanobotUserId = System.getenv("BOT_USER_ID");
      boolean botInvolved =
        nanobotUserId != null &&
        (primaryUserId.equals(nanobotUserId) ||
          primaryReceiverIds.contains(nanobotUserId) ||
          (secondaryUserId != null &&
            (secondaryUserId.equals(nanobotUserId) ||
              secondaryReceiverIds.contains(nanobotUserId))));

      Set<GuildWalletsEntity> guildWalletQuantities = botInvolved
        ? getGuildWallets(guildId)
        : null;

      if (allUserWallets != null) {
        transferResponseDto.setUserWalletHistories(
          allUserWallets
            .stream()
            .map(UserWalletsDto::new)
            .collect(Collectors.toSet())
        );
      }

      if (allUserItems != null) {
        transferResponseDto.setUserItemHistories(
          allUserItems
            .stream()
            .map(UserItemsDto::new)
            .collect(Collectors.toSet())
        );
      }

      if (guildWalletQuantities != null) {
        transferResponseDto.setGuildWalletHistories(
          guildWalletQuantities
            .stream()
            .map(GuildWalletsDto::new)
            .collect(Collectors.toSet())
        );
      }

      transferResponseDto.setUserWalletQuantities(allUserWallets);
      transferResponseDto.setUserItemQuantities(allUserItems);
      transferResponseDto.setGuildWalletQuantities(guildWalletQuantities);

      logQuantities(botInvolved, transferResponseDto);

      TransferResponseDto primaryResult = transferService.transfer(
        guildId,
        primaryUserId,
        primaryReceiverIds,
        transferResponseDto,
        true
      );

      if (primaryResult.getErrorMessage() != null) {
        transferResponseDto.setErrorMessage(primaryResult.getErrorMessage());
        return transferResponseDto;
      }

      transferResponseDto.setUserWalletQuantities(
        primaryResult.getUserWalletQuantities()
      );
      transferResponseDto.setUserItemQuantities(
        primaryResult.getUserItemQuantities()
      );
      transferResponseDto.setGuildWalletQuantities(
        primaryResult.getGuildWalletQuantities()
      );

      transferResponseDto.setCompletedPrimaryTransfers(
        primaryResult.getCompletedPrimaryTransfers()
      );

      logQuantities(botInvolved, transferResponseDto);

      if (secondary != null && secondaryUserId != null) {
        TransferResponseDto secondaryResult = transferService.transfer(
          guildId,
          secondaryUserId,
          secondaryReceiverIds,
          transferResponseDto,
          false
        );

        if (secondaryResult.getErrorMessage() != null) {
          transferResponseDto.setErrorMessage(
            secondaryResult.getErrorMessage()
          );
          return transferResponseDto;
        }

        transferResponseDto.setUserWalletQuantities(
          secondaryResult.getUserWalletQuantities()
        );
        transferResponseDto.setUserItemQuantities(
          secondaryResult.getUserItemQuantities()
        );
        transferResponseDto.setGuildWalletQuantities(
          secondaryResult.getGuildWalletQuantities()
        );

        transferResponseDto.setCompletedSecondaryTransfers(
          secondaryResult.getCompletedSecondaryTransfers()
        );

        logQuantities(botInvolved, transferResponseDto);
      }

      processEntities(
        transferResponseDto.getUserWalletQuantities(),
        userWalletsService::saveWallet,
        LoggingUtil::logCurrentWallets
      );
      processEntities(
        transferResponseDto.getUserItemQuantities(),
        userItemsService::saveItem,
        LoggingUtil::logCurrentItems
      );
      processEntities(
        transferResponseDto.getGuildWalletQuantities(),
        guildWalletsService::saveWallet,
        LoggingUtil::logCurrentGuildWallets
      );

      TransactionDto transaction = new TransactionDto(
        command,
        transferResponseDto
      );
      transaction.setGuildId(guildId);
      transaction.setChannelId(channelId);
      transaction.setPrimaryUserId(primaryUserId);
      transaction.setSecondaryUserId(secondaryUserId);
      transaction.setPrimaryReceiverIds(primaryReceiverIds);
      transaction.setSecondaryReceiverIds(secondaryReceiverIds);

      TransactionEntity newTransaction = transactionsService.createTransaction(
        transaction
      );

      transferResponseDto.setTransactionId(newTransaction.getId());

      return transferResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging("TRANSFER EXECUTOR", e);
      throw e;
    }
  }

  @Recover
  public TransferResponseDto recoverOptimisticLock(
    OptimisticLockingFailureException e,
    String command,
    String guildId,
    String channelId,
    String primaryUserId,
    String secondaryUserId,
    List<String> primaryReceiverIds,
    List<String> secondaryReceiverIds,
    TransferResponseDto transferResponseDto
  ) {
    LoggingUtil.errorLogging("TRANSFER EXECUTOR (retries exhausted)", e);
    transferResponseDto.setErrorMessage(
      "A temporary conflict occurred while processing; please try again."
    );
    return transferResponseDto;
  }
}
