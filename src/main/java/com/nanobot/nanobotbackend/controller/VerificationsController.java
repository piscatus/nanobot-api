package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.VerificationDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import com.nanobot.nanobotbackend.service.EmojisService;
import com.nanobot.nanobotbackend.service.VerificationsService;
import java.util.Comparator;
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
@RequestMapping("verifications")
public class VerificationsController {

  private static final long TWELVE_HOURS_MS = 12L * 60 * 60 * 1000;

  private VerificationsService verificationsService;
  private EmojisService emojisService;

  private static final String NOT_FOUND_MESSAGE = "Verification ID not found";

  public VerificationsController(
    VerificationsService verificationsService,
    EmojisService emojisService
  ) {
    this.verificationsService = verificationsService;
    this.emojisService = emojisService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createVerification(
    @RequestBody VerificationDto newVerificationDto
  ) {
    Optional<VerificationEntity> verification =
      verificationsService.createVerification(newVerificationDto);

    if (verification.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Verification userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(
      Optional.of(verification),
      HttpStatus.CREATED
    );
  }

  @GetMapping
  public ResponseEntity<List<VerificationEntity>> getVerifications(
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      verificationsService.getVerifications(userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getVerificationById(
    @PathVariable String id
  ) {
    Optional<VerificationEntity> verification =
      verificationsService.getVerificationById(id);

    if (verification.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        NOT_FOUND_MESSAGE,
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(verification), HttpStatus.OK);
  }

  @GetMapping("/userId/{userId}")
  public ResponseEntity<Optional<Object>> getVerificationByUserId(
    @PathVariable String userId
  ) {
    Optional<VerificationEntity> verification =
      verificationsService.getVerificationByUserId(userId);

    if (verification.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Verification with specified userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(verification), HttpStatus.OK);
  }

  @GetMapping("/userId/{userId}/check")
  public ResponseEntity<Object> checkVerified(@PathVariable String userId) {
    Optional<VerificationEntity> verification =
      verificationsService.getVerificationByUserId(userId);

    if (verification.isPresent()) {
      Date timestamp = verification.get().getTimestamp();
      if (
        timestamp != null &&
        timestamp.getTime() > System.currentTimeMillis() - TWELVE_HOURS_MS
      ) {
        return new ResponseEntity<>(HttpStatus.OK);
      }
    }

    List<EmojiEntity> emojis = emojisService.getEmojis(null, null);
    emojis.sort(Comparator.comparing(EmojiEntity::getCategory));
    return new ResponseEntity<>(emojis, HttpStatus.UNAUTHORIZED);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateVerification(
    @PathVariable String id,
    @RequestBody VerificationDto updatedVerificationDto
  ) {
    Optional<VerificationEntity> verification =
      verificationsService.updateVerification(id, updatedVerificationDto);

    if (verification.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        NOT_FOUND_MESSAGE,
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(
      Optional.of(verification),
      HttpStatus.ACCEPTED
    );
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteVerification(
    @PathVariable String id
  ) {
    Optional<VerificationEntity> verification =
      verificationsService.deleteVerification(id);

    if (verification.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
