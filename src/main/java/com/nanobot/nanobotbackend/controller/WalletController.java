package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;
import com.nanobot.nanobotbackend.service.WalletServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/wallet")
public class WalletController {

  private final WalletServices walletServices;

  public WalletController(WalletServices walletServices) {
    this.walletServices = walletServices;
  }

  @PostMapping
  public ResponseEntity<UserWalletsResponseDto> wallet(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      walletServices.wallet(requestData),
      HttpStatus.OK
    );
  }
}
