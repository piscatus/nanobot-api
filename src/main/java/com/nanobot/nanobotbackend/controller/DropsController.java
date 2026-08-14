package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.service.DropsService;
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
@RequestMapping("drops")
public class DropsController {

  private DropsService dropsService;

  private static final String NOT_FOUND_MESSAGE = "Drop ID not found";

  public DropsController(DropsService dropsService) {
    this.dropsService = dropsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createDrop(
    @RequestBody DropDto newDropDto
  ) {
    Optional<DropEntity> drop = dropsService.createDrop(newDropDto);

    if (drop.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Drop dropId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(drop), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<DropEntity>> getDrops(
    @RequestParam(required = false) String messageId
  ) {
    return new ResponseEntity<>(
      dropsService.getDrops(messageId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getDropById(@PathVariable String id) {
    Optional<DropEntity> drop = dropsService.getDropById(id);

    if (drop.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(drop), HttpStatus.OK);
  }

  @GetMapping("/messageId/{messageId}/userId/{userId}")
  public ResponseEntity<Optional<Object>> getDropByMessageId(
    @PathVariable String messageId
  ) {
    Optional<DropEntity> drop = dropsService.getDropByMessageId(messageId);

    if (drop.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Drop messageId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(drop), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateDrop(
    @PathVariable String id,
    @RequestBody DropDto updatedDropDto
  ) {
    Optional<DropEntity> drop = dropsService.updateDrop(id, updatedDropDto);

    if (drop.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(drop), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteDrop(@PathVariable String id) {
    Optional<DropEntity> drop = dropsService.deleteDrop(id);

    if (drop.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
