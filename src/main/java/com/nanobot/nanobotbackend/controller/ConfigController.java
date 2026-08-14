package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ConfigResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.ConfigServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class ConfigController {

  private final ConfigServices configServices;

  public ConfigController(ConfigServices configServices) {
    this.configServices = configServices;
  }

  @PutMapping("/config")
  public ResponseEntity<ConfigResponseDto> config(
    @RequestBody GuildConfigurationsDto requestData
  ) {
    return new ResponseEntity<>(
      configServices.config(requestData),
      HttpStatus.ACCEPTED
    );
  }
}
