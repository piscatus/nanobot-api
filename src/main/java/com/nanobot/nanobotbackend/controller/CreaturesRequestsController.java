package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.CreaturesServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/creatures")
public class CreaturesRequestsController {

  private final CreaturesServices creaturesServices;

  public CreaturesRequestsController(CreaturesServices creaturesServices) {
    this.creaturesServices = creaturesServices;
  }

  @PostMapping
  public ResponseEntity<CreaturesResponseDto> creatures(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      creaturesServices.creatures(requestData),
      HttpStatus.OK
    );
  }
}
