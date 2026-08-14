package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.repository.TransactionsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.stereotype.Service;

@Service
public class TransactionsServiceImpl implements TransactionsService {

  private TransactionsRepository transactionsRepository;

  private final FileLogger fileLogger;

  public TransactionsServiceImpl(
    TransactionsRepository transactionsRepository
  ) {
    this.fileLogger = new FileLogger("TransactionsService");
    this.transactionsRepository = transactionsRepository;
  }

  @Override
  public TransactionEntity createTransaction(TransactionDto transactionDto) {
    TransactionEntity transactionEntity = new TransactionEntity(transactionDto);
    ObjectId id = new ObjectId();
    transactionEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating transaction with ID: " + transactionEntity.getId()
    );
    try {
      TransactionEntity createdTransaction = transactionsRepository.insert(
        transactionEntity
      );
      fileLogger.info("Transaction created with ID: " + createdTransaction);
      return createdTransaction;
    } catch (Exception e) {
      fileLogger.error("Error creating transaction: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<TransactionEntity> getTransactions(String userId) {
    try {
      if (userId == null) {
        fileLogger.info("Fetching all transactions by oldest timestamp.");
        return transactionsRepository.findAllByOrderByTimestampDesc();
      }
      fileLogger.info("Fetching transactions by userId: " + userId);
      return transactionsRepository.findByAnyUserId(userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching transactions: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<TransactionEntity> getTransactionById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching transaction with ID: " + id);
        return transactionsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching transaction: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<TransactionEntity> updateTransaction(
    String id,
    TransactionDto transactionDto
  ) {
    if (id != null) {
      try {
        Optional<TransactionEntity> transactionsOptional =
          transactionsRepository.findById(id);
        if (transactionsOptional.isPresent()) {
          TransactionEntity transactions = transactionsOptional.get();
          transactions.setCompletedPrimaryTransfers(
            transactionDto.getCompletedPrimaryTransfers()
          );
          transactions.setCompletedSecondaryTransfers(
            transactionDto.getCompletedSecondaryTransfers()
          );
          transactions.setCompletedPrimaryGuildTransfers(
            transactionDto.getCompletedPrimaryGuildTransfers()
          );
          transactions.setCompletedSecondaryGuildTransfers(
            transactionDto.getCompletedSecondaryGuildTransfers()
          );
          transactions.setGuildWalletQuantities(
            transactionDto.getGuildWalletQuantities()
          );
          transactions.setInput(transactionDto.getInput());
          transactions.setPrimaryTransfer(transactionDto.getPrimaryTransfer());
          transactions.setSecondaryTransfer(
            transactionDto.getSecondaryTransfer()
          );
          transactions.setUserWalletQuantities(
            transactionDto.getUserWalletQuantities()
          );
          transactions.setUserItemQuantities(
            transactionDto.getUserItemQuantities()
          );
          transactions.setTimestamp(transactionDto.getTimestamp());

          TransactionEntity updatedTransaction = transactionsRepository.save(
            transactions
          );
          fileLogger.info("Transaction updated with ID: " + id);
          return Optional.of(updatedTransaction);
        } else {
          fileLogger.warn("Transaction not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating transaction: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<TransactionEntity> updateTransactionBlockHash(
    String id,
    String blockHash
  ) {
    if (id != null) {
      try {
        Optional<TransactionEntity> transactionsOptional =
          transactionsRepository.findById(id);
        if (transactionsOptional.isPresent()) {
          TransactionEntity transactions = transactionsOptional.get();
          transactions.setBlockHash(blockHash);

          TransactionEntity updatedTransaction = transactionsRepository.save(
            transactions
          );
          fileLogger.info("Transaction block hash updated with ID: " + id);
          return Optional.of(updatedTransaction);
        } else {
          fileLogger.warn("Transaction not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error updating transaction block hash: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<TransactionEntity> deleteTransaction(String id) {
    if (id != null) {
      try {
        Optional<TransactionEntity> transactionsOptional =
          transactionsRepository.findById(id);
        if (transactionsOptional.isPresent()) {
          TransactionEntity transactions = transactionsOptional.get();
          transactionsRepository.deleteById(id);
          fileLogger.info("Transaction deleted with ID: " + id);
          return Optional.of(transactions);
        } else {
          fileLogger.warn("Transaction not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting transaction: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public void deleteTransactionsOlderThan30Days() {
    try {
      Date thirtyDaysAgo = new Date(
        System.currentTimeMillis() - 30L * 24 * 60 * 60 * 1000
      );
      fileLogger.info("Deleting transactions older than: " + thirtyDaysAgo);

      long deletedCount = transactionsRepository.deleteByTimestampBefore(
        thirtyDaysAgo
      );
      fileLogger.info(
        "Deleted " + deletedCount + " transactions older than 30 days."
      );
    } catch (Exception e) {
      fileLogger.error("Error deleting old transactions: " + e.getMessage());
      throw e;
    }
  }
}
