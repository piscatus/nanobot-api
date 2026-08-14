package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.service.PickupsService;
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
@RequestMapping("pickups")
public class PickupsController {

  private PickupsService pickupsService;

  private static final String NOT_FOUND_MESSAGE = "Pickup ID not found";

  public PickupsController(PickupsService pickupsService) {
    this.pickupsService = pickupsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createPickup(
    @RequestBody PickupDto newPickupDto
  ) {
    Optional<PickupEntity> pickup = pickupsService.createPickup(newPickupDto);

    if (pickup.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Pickup dropId/userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(pickup), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<PickupEntity>> getPickups(
    @RequestParam(required = false) String dropId,
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      pickupsService.getPickups(dropId, userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getPickupById(
    @PathVariable String id
  ) {
    Optional<PickupEntity> pickups = pickupsService.getPickupById(id);

    if (pickups.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(pickups), HttpStatus.OK);
  }

  @GetMapping("/dropId/{dropId}/userId/{userId}")
  public ResponseEntity<Optional<Object>> getPickupByDropIdAndUserId(
    @PathVariable String dropId,
    @PathVariable String userId
  ) {
    Optional<PickupEntity> pickup = pickupsService.getPickupByDropIdAndUserId(
      dropId,
      userId
    );

    if (pickup.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Pickup with specified dropId/userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(pickup), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updatePickup(
    @PathVariable String id,
    @RequestBody PickupDto updatedPickupDto
  ) {
    Optional<PickupEntity> pickups = pickupsService.updatePickup(
      id,
      updatedPickupDto
    );

    if (pickups.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(pickups), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deletePickup(
    @PathVariable String id
  ) {
    Optional<PickupEntity> pickups = pickupsService.deletePickup(id);

    if (pickups.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
