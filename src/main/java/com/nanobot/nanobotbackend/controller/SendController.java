package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.SendServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class SendController {

  private final SendServices sendServices;

  public SendController(SendServices sendServices) {
    this.sendServices = sendServices;
  }

  @PutMapping("/update")
  public ResponseEntity<TransferResponseDto> update(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      sendServices.update(requestData),
      HttpStatus.OK
    );
  }

  @PutMapping("/send")
  public ResponseEntity<TransferResponseDto> send(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(sendServices.send(requestData), HttpStatus.OK);
  }
}
