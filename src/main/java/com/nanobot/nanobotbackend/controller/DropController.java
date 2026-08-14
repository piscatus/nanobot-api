package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.DropService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("drop")
public class DropController {

  private DropService dropService;

  public DropController(DropService dropService) {
    this.dropService = dropService;
  }

  @PostMapping("/update")
  public ResponseEntity<BaseResponseDto> updateDropMessageId(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      dropService.dropUpdate(requestData),
      HttpStatus.ACCEPTED
    );
  }
}
