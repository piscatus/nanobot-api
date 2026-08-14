package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import java.util.List;
import java.util.Optional;

public interface TransactionsService {
  TransactionEntity createTransaction(TransactionDto transactionDto);

  List<TransactionEntity> getTransactions(String userId);

  Optional<TransactionEntity> getTransactionById(String id);

  Optional<TransactionEntity> updateTransaction(
    String id,
    TransactionDto transactionDto
  );

  Optional<TransactionEntity> updateTransactionBlockHash(
    String id,
    String blockHash
  );

  Optional<TransactionEntity> deleteTransaction(String id);

  void deleteTransactionsOlderThan30Days();
}
