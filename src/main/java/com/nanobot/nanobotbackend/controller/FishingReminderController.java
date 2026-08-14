package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.FishingReminderResponseDto;
import com.nanobot.nanobotbackend.service.FishingReminderService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("reminders")
public class FishingReminderController {

  private FishingReminderService fishingReminderService;

  public FishingReminderController(
    FishingReminderService fishingReminderService
  ) {
    this.fishingReminderService = fishingReminderService;
  }

  @GetMapping
  public ResponseEntity<FishingReminderResponseDto> getFishingReminders() {
    return new ResponseEntity<>(
      fishingReminderService.getFishingReminder(),
      HttpStatus.OK
    );
  }
}
