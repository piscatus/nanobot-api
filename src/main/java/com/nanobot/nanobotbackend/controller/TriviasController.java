package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.TriviaBulkResultDto;
import com.nanobot.nanobotbackend.dto.TriviaDto;
import com.nanobot.nanobotbackend.entity.TriviaEntity;
import com.nanobot.nanobotbackend.service.TriviasService;
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
@RequestMapping("trivias")
public class TriviasController {

  private final TriviasService triviasService;

  private static final String NOT_FOUND_MESSAGE = "Trivia ID not found";

  public TriviasController(TriviasService triviasService) {
    this.triviasService = triviasService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createTrivia(
    @RequestBody TriviaDto newTriviaDto
  ) {
    String validationError = triviasService.validateTrivia(newTriviaDto);
    if (validationError != null) {
      ErrorDto error = new ErrorDto(new Date(), 400, validationError, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    Optional<TriviaEntity> trivia = triviasService.createTrivia(newTriviaDto);
    if (trivia.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Trivia with the same question already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(trivia), HttpStatus.CREATED);
  }

  @PostMapping("/bulk")
  public ResponseEntity<TriviaBulkResultDto> createTrivias(
    @RequestBody List<TriviaDto> newTriviaDtos
  ) {
    return new ResponseEntity<>(
      triviasService.createTrivias(newTriviaDtos),
      HttpStatus.OK
    );
  }

  @GetMapping
  public ResponseEntity<List<TriviaEntity>> getTrivias(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) Boolean enabled,
    @RequestParam(required = false) String source
  ) {
    return new ResponseEntity<>(
      triviasService.getTrivias(category, enabled, source),
      HttpStatus.OK
    );
  }

  @GetMapping("/categories")
  public ResponseEntity<List<String>> getCategories() {
    return new ResponseEntity<>(triviasService.getCategories(), HttpStatus.OK);
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getTriviaById(
    @PathVariable String id
  ) {
    Optional<TriviaEntity> trivia = triviasService.getTriviaById(id);
    if (trivia.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(trivia), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateTrivia(
    @PathVariable String id,
    @RequestBody TriviaDto updatedTriviaDto
  ) {
    String validationError = triviasService.validateTrivia(updatedTriviaDto);
    if (validationError != null) {
      ErrorDto error = new ErrorDto(new Date(), 400, validationError, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    Optional<TriviaEntity> trivia = triviasService.updateTrivia(
      id,
      updatedTriviaDto
    );
    if (trivia.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(trivia), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteTrivia(
    @PathVariable String id
  ) {
    Optional<TriviaEntity> trivia = triviasService.deleteTrivia(id);
    if (trivia.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
