package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.ActivityEntity;
import com.nanobot.nanobotbackend.service.ActivitiesService;
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
@RequestMapping("activities")
public class ActivitiesController {

  private ActivitiesService activitiesService;

  private static final String NOT_FOUND_MESSAGE = "Activity ID not found";

  public ActivitiesController(ActivitiesService activitiesService) {
    this.activitiesService = activitiesService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createActivity(
    @RequestBody ActivityDto newActivityDto
  ) {
    Optional<ActivityEntity> activity = activitiesService.createActivity(
      newActivityDto
    );

    if (activity.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Activity guildId/channelId/userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(activity), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<ActivityEntity>> getActivities(
    @RequestParam(required = false) String guildId,
    @RequestParam(required = false) String channelId,
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      activitiesService.getActivities(guildId, channelId, userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getActivityById(
    @PathVariable String id
  ) {
    Optional<ActivityEntity> activity = activitiesService.getActivityById(id);

    if (activity.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(activity), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateActivity(
    @PathVariable String id,
    @RequestBody ActivityDto updatedActivityDto
  ) {
    Optional<ActivityEntity> activity = activitiesService.updateActivity(
      id,
      updatedActivityDto
    );

    if (activity.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(activity), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteActivity(
    @PathVariable String id
  ) {
    Optional<ActivityEntity> activity = activitiesService.deleteActivity(id);

    if (activity.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/updateOrCreate")
  public ResponseEntity<ActivityEntity> updateOrCreateActivity(
    @RequestParam String guildId,
    @RequestParam String channelId,
    @RequestParam String userId
  ) {
    ActivityEntity updatedOrNewActivity =
      activitiesService.updateOrCreateActivity(guildId, channelId, userId);
    return new ResponseEntity<>(updatedOrNewActivity, HttpStatus.OK);
  }
}
