package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.BonusesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.CoreServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class CoreController {

  private final CoreServices coreServices;

  public CoreController(CoreServices coreServices) {
    this.coreServices = coreServices;
  }

  @PostMapping("/bonuses")
  public ResponseEntity<BonusesResponseDto> bonuses(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      coreServices.bonuses(requestData),
      HttpStatus.OK
    );
  }

  @PostMapping("/cute")
  public ResponseEntity<BaseResponseDto> cute(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(coreServices.cute(requestData), HttpStatus.OK);
  }

  @PostMapping("/roles")
  public ResponseEntity<BaseResponseDto> roles(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(coreServices.roles(requestData), HttpStatus.OK);
  }

  @PostMapping("/rules")
  public ResponseEntity<BaseResponseDto> rules(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(coreServices.rules(requestData), HttpStatus.OK);
  }
}
