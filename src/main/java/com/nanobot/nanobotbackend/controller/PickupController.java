package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.PickupServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/pickup")
public class PickupController {

  private final PickupServices pickupServices;

  public PickupController(PickupServices pickupServices) {
    this.pickupServices = pickupServices;
  }

  @PostMapping
  public ResponseEntity<BaseResponseDto> pickup(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      pickupServices.pickup(requestData),
      HttpStatus.ACCEPTED
    );
  }
}
