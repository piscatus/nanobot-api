package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.service.CreaturesService;
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
@RequestMapping("creatures")
public class CreaturesController {

  private CreaturesService creaturesService;

  private static final String NOT_FOUND_MESSAGE = "Creature ID not found";

  public CreaturesController(CreaturesService creaturesService) {
    this.creaturesService = creaturesService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createCreature(
    @RequestBody CreatureDto creatureDto
  ) {
    Optional<CreatureEntity> creature = creaturesService.createCreature(
      creatureDto
    );

    if (creature.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Creature name already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(creature), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<CreatureEntity>> getCreatures() {
    return new ResponseEntity<>(creaturesService.getCreatures(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getCreatureById(
    @PathVariable String id
  ) {
    Optional<CreatureEntity> creature = creaturesService.getCreatureById(id);

    if (creature.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(creature), HttpStatus.OK);
  }

  @GetMapping("/name/{name}")
  public ResponseEntity<Optional<Object>> getCreatureByName(
    @PathVariable String name
  ) {
    Optional<CreatureEntity> creature = creaturesService.getCreatureByName(
      name
    );

    if (creature.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Creature with specified name not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(creature), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateCreature(
    @PathVariable String id,
    @RequestBody CreatureDto updatedCreatureDto
  ) {
    Optional<CreatureEntity> creature = creaturesService.updateCreature(
      id,
      updatedCreatureDto
    );

    if (creature.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(creature), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteCreature(
    @PathVariable String id
  ) {
    Optional<CreatureEntity> creature = creaturesService.deleteCreature(id);

    if (creature.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
