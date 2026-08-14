package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.FishServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/fish")
public class FishController {

  private final FishServices fishServices;

  public FishController(FishServices fishServices) {
    this.fishServices = fishServices;
  }

  @PutMapping
  public ResponseEntity<TransferResponseDto> fish(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(fishServices.fish(requestData), HttpStatus.OK);
  }
}
