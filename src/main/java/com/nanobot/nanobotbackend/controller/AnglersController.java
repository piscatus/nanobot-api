package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.service.AnglersService;
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
@RequestMapping("anglers")
public class AnglersController {

  private AnglersService anglersService;

  private static final String NOT_FOUND_MESSAGE = "Angler ID not found";

  public AnglersController(AnglersService anglersService) {
    this.anglersService = anglersService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createAngler(
    @RequestBody AnglerDto newAnglerDto
  ) {
    Optional<AnglerEntity> angler = anglersService.createAngler(newAnglerDto);

    if (angler.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Angler guildId/userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(angler), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<AnglerEntity>> getAnglers(
    @RequestParam(required = false) String guildId,
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      anglersService.getAnglers(guildId, userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/resting")
  public ResponseEntity<List<AnglerEntity>> getRestingAnglers() {
    return new ResponseEntity<>(
      anglersService.getRestingAnglers(),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getAnglerById(
    @PathVariable String id
  ) {
    Optional<AnglerEntity> angler = anglersService.getAnglerById(id);

    if (angler.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(angler), HttpStatus.OK);
  }

  @GetMapping("/guildId/{guildId}/userId/{userId}")
  public ResponseEntity<Optional<Object>> getAnglerByGuildIdAndUserId(
    @PathVariable String guildId,
    @PathVariable String userId
  ) {
    Optional<AnglerEntity> angler = anglersService.getAnglerByGuildIdAndUserId(
      guildId,
      userId
    );

    if (angler.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Angler with specified guildId/userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(angler), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateAnglers(
    @PathVariable String id,
    @RequestBody AnglerDto updatedAnglerDto
  ) {
    Optional<AnglerEntity> anglers = anglersService.updateAngler(
      id,
      updatedAnglerDto
    );

    if (anglers.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(anglers), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteAnglers(
    @PathVariable String id
  ) {
    Optional<AnglerEntity> anglers = anglersService.deleteAngler(id);

    if (anglers.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }

  @PutMapping("/updateOrCreate")
  public ResponseEntity<AnglerEntity> updateOrCreateAngler(
    @RequestParam String guildId,
    @RequestParam String userId
  ) {
    AnglerEntity updatedOrNewAngler = anglersService.updateOrCreateAngler(
      guildId,
      userId
    );
    return new ResponseEntity<>(updatedOrNewAngler, HttpStatus.OK);
  }

  @PutMapping("/restless/{id}")
  public ResponseEntity<Optional<Object>> updateAnglers(
    @PathVariable String id
  ) {
    Optional<AnglerEntity> anglers = anglersService.updateAnglerRestless(id);

    if (anglers.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(anglers), HttpStatus.ACCEPTED);
  }
}
