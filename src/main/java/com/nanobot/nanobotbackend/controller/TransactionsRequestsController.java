package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransactionsResponseDto;
import com.nanobot.nanobotbackend.service.TransactionsServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/transactions")
public class TransactionsRequestsController {

  private final TransactionsServices transactionsServices;

  public TransactionsRequestsController(
    TransactionsServices transactionsServices
  ) {
    this.transactionsServices = transactionsServices;
  }

  @PostMapping
  public ResponseEntity<TransactionsResponseDto> transactions(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transactionsServices.transactions(requestData),
      HttpStatus.OK
    );
  }
}
