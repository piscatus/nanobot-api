package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.service.TransactionsService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("transactions")
public class TransactionsController {

  private TransactionsService transactionsService;

  public TransactionsController(TransactionsService transactionsService) {
    this.transactionsService = transactionsService;
  }

  @PostMapping
  public ResponseEntity<TransactionEntity> createTransaction(
    @RequestBody TransactionDto newTransactionDto
  ) {
    return new ResponseEntity<>(
      transactionsService.createTransaction(newTransactionDto),
      HttpStatus.CREATED
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getTransactionById(
    @PathVariable String id
  ) {
    Optional<TransactionEntity> sransactions =
      transactionsService.getTransactionById(id);

    if (sransactions.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Transaction ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(sransactions), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<List<TransactionEntity>> getTransactions(
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      transactionsService.getTransactions(userId),
      HttpStatus.OK
    );
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateTransaction(
    @PathVariable String id,
    @RequestBody TransactionDto updatedTransactionDto
  ) {
    Optional<TransactionEntity> sransactions =
      transactionsService.updateTransaction(id, updatedTransactionDto);

    if (sransactions.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Transaction ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(sransactions), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteTransaction(
    @PathVariable String id
  ) {
    Optional<TransactionEntity> sransactions =
      transactionsService.deleteTransaction(id);

    if (sransactions.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(
          new ErrorDto(new Date(), 404, "Transaction id not found", "")
        ),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
