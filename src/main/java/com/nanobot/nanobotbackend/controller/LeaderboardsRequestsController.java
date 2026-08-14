package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.LeaderboardsResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.LeaderboardsServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/leaderboards")
public class LeaderboardsRequestsController {

  private final LeaderboardsServices leaderboardsServices;

  public LeaderboardsRequestsController(
    LeaderboardsServices leaderboardsServices
  ) {
    this.leaderboardsServices = leaderboardsServices;
  }

  @PostMapping
  public ResponseEntity<LeaderboardsResponseDto> leaderboards(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      leaderboardsServices.leaderboards(requestData),
      HttpStatus.OK
    );
  }
}
