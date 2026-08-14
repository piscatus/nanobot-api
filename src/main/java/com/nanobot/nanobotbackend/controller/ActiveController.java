package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.ActiveServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/active")
public class ActiveController {

  private final ActiveServices activeServices;

  public ActiveController(
      ActiveServices activeServices
  ) {
    this.activeServices = activeServices;
  }

  @PostMapping
  public ResponseEntity<TransferResponseDto> active(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
        activeServices.active(requestData), 
        HttpStatus.OK
    );
  }
}