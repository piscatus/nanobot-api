package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.service.LeaderboardsService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("leaderboards")
public class LeaderboardsController {

  private LeaderboardsService leaderboardsService;

  private static final String NOT_FOUND_MESSAGE = "Leaderboard ID not found";

  public LeaderboardsController(LeaderboardsService leaderboardsService) {
    this.leaderboardsService = leaderboardsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createLeaderboard(
    @RequestBody LeaderboardDto newLeaderboardDto
  ) {
    Optional<LeaderboardEntity> leaderboard =
      leaderboardsService.createLeaderboard(newLeaderboardDto);

    if (leaderboard.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Leaderboard guildId/userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(leaderboard), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<LeaderboardEntity>> getLeaderboards(
    @RequestParam(required = false) String guildId,
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      leaderboardsService.getLeaderboards(guildId, userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getLeaderboardById(
    @PathVariable String id
  ) {
    Optional<LeaderboardEntity> leaderboards =
      leaderboardsService.getLeaderboardById(id);

    if (leaderboards.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");

      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }

    return new ResponseEntity<>(Optional.of(leaderboards), HttpStatus.OK);
  }

  @GetMapping("/guildId/{guildId}/userId/{userId}")
  public ResponseEntity<Optional<Object>> getLeaderboardByGuildIdAndUserId(
    @PathVariable String guildId,
    @PathVariable String userId
  ) {
    Optional<LeaderboardEntity> leaderboard =
      leaderboardsService.getLeaderboardByGuildIdAndUserId(guildId, userId);

    if (leaderboard.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Leaderboard with specified guildId/userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(leaderboard), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateLeaderboard(
    @PathVariable String id,
    @RequestBody LeaderboardDto updatedLeaderboardDto
  ) {
    Optional<LeaderboardEntity> leaderboards =
      leaderboardsService.updateLeaderboard(id, updatedLeaderboardDto);

    if (leaderboards.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(leaderboards), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteLeaderboard(
    @PathVariable String id
  ) {
    Optional<LeaderboardEntity> leaderboards =
      leaderboardsService.deleteLeaderboard(id);

    if (leaderboards.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
