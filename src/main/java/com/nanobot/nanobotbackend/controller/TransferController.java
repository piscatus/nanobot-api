package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.TransferServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class TransferController {

  private final TransferServices transferServices;

  public TransferController(TransferServices transferServices) {
    this.transferServices = transferServices;
  }

  @PutMapping("/drop")
  public ResponseEntity<TransferResponseDto> drop(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transferServices.drop(requestData),
      HttpStatus.OK
    );
  }

  @PutMapping("/gift")
  public ResponseEntity<TransferResponseDto> gift(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transferServices.gift(requestData),
      HttpStatus.OK
    );
  }

  @PutMapping("/merge")
  public ResponseEntity<TransferResponseDto> merge(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transferServices.merge(requestData),
      HttpStatus.OK
    );
  }

  @PutMapping("/rain")
  public ResponseEntity<TransferResponseDto> rain(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transferServices.rain(requestData),
      HttpStatus.OK
    );
  }

  @PutMapping("/sell")
  public ResponseEntity<TransferResponseDto> sell(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      transferServices.sell(requestData),
      HttpStatus.OK
    );
  }
}
