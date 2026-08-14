package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.ProfanityDto;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import com.nanobot.nanobotbackend.service.ProfanitiesService;
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
@RequestMapping("profanities")
public class ProfanitiesController {

  private ProfanitiesService profanitiesService;

  private static final String NOT_FOUND_MESSAGE = "Profanity ID not found";

  public ProfanitiesController(ProfanitiesService profanitiesService) {
    this.profanitiesService = profanitiesService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createProfanity(
    @RequestBody ProfanityDto newProfanityDto
  ) {
    Optional<ProfanityEntity> profanity = profanitiesService.createProfanity(
      newProfanityDto
    );

    if (profanity.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Profanity singular/plural already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(profanity), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<ProfanityEntity>> getProfanities(
    @RequestParam(required = false) String singular
  ) {
    return new ResponseEntity<>(
      profanitiesService.getProfanities(singular),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getProfanityById(
    @PathVariable String id
  ) {
    Optional<ProfanityEntity> profanity = profanitiesService.getProfanityById(
      id
    );

    if (profanity.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(profanity), HttpStatus.OK);
  }

  @GetMapping("/singular/{singular}")
  public ResponseEntity<Optional<Object>> getProfanityBySingular(
    @PathVariable String singular
  ) {
    Optional<ProfanityEntity> profanity =
      profanitiesService.getProfanityBySingular(singular);

    if (profanity.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Profanity with specified singular not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(profanity), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateProfanity(
    @PathVariable String id,
    @RequestBody ProfanityDto updatedProfanityDto
  ) {
    Optional<ProfanityEntity> profanity = profanitiesService.updateProfanity(
      id,
      updatedProfanityDto
    );

    if (profanity.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(profanity), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteProfanity(
    @PathVariable String id
  ) {
    Optional<ProfanityEntity> profanity = profanitiesService.deleteProfanity(
      id
    );

    if (profanity.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
