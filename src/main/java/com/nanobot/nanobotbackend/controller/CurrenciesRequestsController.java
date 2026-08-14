package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.CurrenciesResponseDto;
import com.nanobot.nanobotbackend.dto.HelpResponseDto;
import com.nanobot.nanobotbackend.dto.ReceiveResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.CurrenciesServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class CurrenciesRequestsController {

  private final CurrenciesServices currenciesServices;

  public CurrenciesRequestsController(CurrenciesServices currenciesServices) {
    this.currenciesServices = currenciesServices;
  }

  @PostMapping("/currencies")
  public ResponseEntity<CurrenciesResponseDto> currencies(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      currenciesServices.currencies(requestData),
      HttpStatus.OK
    );
  }

  @PostMapping("/help")
  public ResponseEntity<HelpResponseDto> help(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      currenciesServices.help(requestData),
      HttpStatus.OK
    );
  }

  @PostMapping("/receive")
  public ResponseEntity<ReceiveResponseDto> receive(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      currenciesServices.receive(requestData),
      HttpStatus.OK
    );
  }
}
