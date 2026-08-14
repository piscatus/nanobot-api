package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.repository.TransactionsRepository;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.stubbing.Answer;

@ExtendWith(MockitoExtension.class)
class TransactionsServiceImplTest {

  @Mock
  private TransactionsRepository transactionsRepository;

  private TransactionsServiceImpl transactionsService;

  @BeforeEach
  void setUp() {
    transactionsService = new TransactionsServiceImpl(transactionsRepository);
  }

  @Test
  void createTransactionShouldReturnCreatedEntity() {
    TransactionEntity entity = new TransactionEntity();
    entity.setCommand("send");
    TransactionDto dto = new TransactionDto(entity);
    when(transactionsRepository.insert(any(TransactionEntity.class)))
      .thenAnswer((Answer<TransactionEntity>) inv -> {
        TransactionEntity e = inv.getArgument(0);
        e.setId("tx-new");
        return e;
      });

    TransactionEntity result = transactionsService.createTransaction(dto);

    assertEquals("tx-new", result.getId());
    verify(transactionsRepository).insert(any(TransactionEntity.class));
  }

  @Test
  void createTransactionShouldPropagateException() {
    TransactionEntity entity = new TransactionEntity();
    entity.setCommand("send");
    TransactionDto dto = new TransactionDto(entity);
    when(transactionsRepository.insert(any(TransactionEntity.class)))
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(
      RuntimeException.class,
      () -> transactionsService.createTransaction(dto)
    );
    verify(transactionsRepository).insert(any(TransactionEntity.class));
  }

  @Test
  void getTransactionsShouldReturnAllWhenUserIdNull() {
    List<TransactionEntity> entities = List.of(new TransactionEntity());
    when(transactionsRepository.findAllByOrderByTimestampDesc())
      .thenReturn(entities);

    List<TransactionEntity> result =
      transactionsService.getTransactions(null);

    assertEquals(1, result.size());
    verify(transactionsRepository).findAllByOrderByTimestampDesc();
  }

  @Test
  void getTransactionsShouldReturnByUserIdWhenProvided() {
    List<TransactionEntity> entities = List.of(new TransactionEntity());
    when(transactionsRepository.findByAnyUserId("user1"))
      .thenReturn(entities);

    List<TransactionEntity> result =
      transactionsService.getTransactions("user1");

    assertEquals(1, result.size());
    verify(transactionsRepository).findByAnyUserId("user1");
  }

  @Test
  void getTransactionsShouldPropagateException() {
    when(transactionsRepository.findAllByOrderByTimestampDesc())
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(
      RuntimeException.class,
      () -> transactionsService.getTransactions(null)
    );
  }

  @Test
  void getTransactionByIdShouldReturnEmptyWhenIdNull() {
    Optional<TransactionEntity> result =
      transactionsService.getTransactionById(null);

    assertFalse(result.isPresent());
  }

  @Test
  void getTransactionByIdShouldReturnEntityWhenFound() {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("tx1");
    entity.setCommand("send");
    when(transactionsRepository.findById("tx1"))
      .thenReturn(Optional.of(entity));

    Optional<TransactionEntity> result =
      transactionsService.getTransactionById("tx1");

    assertTrue(result.isPresent());
    assertEquals("tx1", result.get().getId());
    verify(transactionsRepository).findById("tx1");
  }

  @Test
  void getTransactionByIdShouldReturnEmptyWhenNotFound() {
    when(transactionsRepository.findById("missing"))
      .thenReturn(Optional.empty());

    Optional<TransactionEntity> result =
      transactionsService.getTransactionById("missing");

    assertFalse(result.isPresent());
  }

  @Test
  void updateTransactionShouldReturnEmptyWhenIdNull() {
    TransactionEntity entity = new TransactionEntity();
    TransactionDto dto = new TransactionDto(entity);

    Optional<TransactionEntity> result =
      transactionsService.updateTransaction(null, dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateTransactionShouldReturnUpdatedEntityWhenFound() {
    TransactionEntity existing = new TransactionEntity();
    existing.setId("tx1");
    existing.setCommand("send");
    existing.setInput("old");
    TransactionDto dto = new TransactionDto(existing);
    dto.setInput("updated-input");
    when(transactionsRepository.findById("tx1"))
      .thenReturn(Optional.of(existing));
    when(transactionsRepository.save(any()))
      .thenAnswer(inv -> inv.getArgument(0));

    Optional<TransactionEntity> result =
      transactionsService.updateTransaction("tx1", dto);

    assertTrue(result.isPresent());
    assertEquals("updated-input", result.get().getInput());
    verify(transactionsRepository).findById("tx1");
    verify(transactionsRepository).save(any());
  }

  @Test
  void updateTransactionShouldReturnEmptyWhenNotFound() {
    TransactionEntity entity = new TransactionEntity();
    TransactionDto dto = new TransactionDto(entity);
    when(transactionsRepository.findById("missing"))
      .thenReturn(Optional.empty());

    Optional<TransactionEntity> result =
      transactionsService.updateTransaction("missing", dto);

    assertFalse(result.isPresent());
  }

  @Test
  void updateTransactionBlockHashShouldReturnUpdatedEntityWhenFound() {
    TransactionEntity existing = new TransactionEntity();
    existing.setId("tx1");
    when(transactionsRepository.findById("tx1"))
      .thenReturn(Optional.of(existing));
    when(transactionsRepository.save(any()))
      .thenAnswer(inv -> inv.getArgument(0));

    Optional<TransactionEntity> result =
      transactionsService.updateTransactionBlockHash("tx1", "block123");

    assertTrue(result.isPresent());
    assertEquals("block123", result.get().getBlockHash());
    verify(transactionsRepository).save(any());
  }

  @Test
  void updateTransactionBlockHashShouldReturnEmptyWhenIdNull() {
    Optional<TransactionEntity> result =
      transactionsService.updateTransactionBlockHash(null, "block123");

    assertFalse(result.isPresent());
  }

  @Test
  void deleteTransactionShouldReturnDeletedEntityWhenFound() {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("tx1");
    when(transactionsRepository.findById("tx1"))
      .thenReturn(Optional.of(entity));

    Optional<TransactionEntity> result =
      transactionsService.deleteTransaction("tx1");

    assertTrue(result.isPresent());
    assertEquals("tx1", result.get().getId());
    verify(transactionsRepository).findById("tx1");
    verify(transactionsRepository).deleteById("tx1");
  }

  @Test
  void deleteTransactionShouldReturnEmptyWhenNotFound() {
    when(transactionsRepository.findById("missing"))
      .thenReturn(Optional.empty());

    Optional<TransactionEntity> result =
      transactionsService.deleteTransaction("missing");

    assertFalse(result.isPresent());
  }

  @Test
  void deleteTransactionShouldReturnEmptyWhenIdNull() {
    Optional<TransactionEntity> result =
      transactionsService.deleteTransaction(null);

    assertFalse(result.isPresent());
  }

  @Test
  void deleteTransactionsOlderThan30DaysShouldCallRepository() {
    when(transactionsRepository.deleteByTimestampBefore(any(Date.class)))
      .thenReturn(5L);

    transactionsService.deleteTransactionsOlderThan30Days();

    verify(transactionsRepository).deleteByTimestampBefore(any(Date.class));
  }

  @Test
  void deleteTransactionsOlderThan30DaysShouldPropagateException() {
    when(transactionsRepository.deleteByTimestampBefore(any(Date.class)))
      .thenThrow(new RuntimeException("DB error"));

    assertThrows(
      RuntimeException.class,
      () -> transactionsService.deleteTransactionsOlderThan30Days()
    );
    verify(transactionsRepository).deleteByTimestampBefore(any(Date.class));
  }
}
