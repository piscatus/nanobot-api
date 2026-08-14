package com.nanobot.nanobotbackend.controller;

import java.time.Instant;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("health")
public class HealthController {

  @GetMapping
  public String nanobotBackendHealthCheck() {
    return "Nanobot Backend is Healthy - " + Instant.now();
  }
}
