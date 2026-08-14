package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.ServerResponseDto;
import com.nanobot.nanobotbackend.service.ServerServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class ServerController {

  private final ServerServices serverServices;

  public ServerController(ServerServices serverServices) {
    this.serverServices = serverServices;
  }

  @PostMapping("/reserves")
  public ResponseEntity<ServerResponseDto> reserves(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      serverServices.reserves(requestData),
      HttpStatus.OK
    );
  }

  @PostMapping("/server")
  public ResponseEntity<ServerResponseDto> server(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      serverServices.configurations(requestData),
      HttpStatus.OK
    );
  }
}
