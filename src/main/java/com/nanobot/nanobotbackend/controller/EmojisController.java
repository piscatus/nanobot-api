package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.service.EmojisService;
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
@RequestMapping("emojis")
public class EmojisController {

  private EmojisService emojisService;

  private static final String NOT_FOUND_MESSAGE = "Emoji ID not found";

  public EmojisController(EmojisService emojisService) {
    this.emojisService = emojisService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createEmoji(
    @RequestBody EmojiDto newEmojiDto
  ) {
    Optional<EmojiEntity> emoji = emojisService.createEmoji(newEmojiDto);

    if (emoji.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Emoji category/name already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(emoji), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<EmojiEntity>> getEmojis(
    @RequestParam(required = false) String category,
    @RequestParam(required = false) String name
  ) {
    return new ResponseEntity<>(
      emojisService.getEmojis(category, name),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getEmojiById(
    @PathVariable String id
  ) {
    Optional<EmojiEntity> emoji = emojisService.getEmojiById(id);

    if (emoji.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        NOT_FOUND_MESSAGE,
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(emoji), HttpStatus.OK);
  }

  @GetMapping("/category/{category}/name/{name}")
  public ResponseEntity<Optional<Object>> getEmojiByCategoryAndName(
    @PathVariable String category,
    @PathVariable String name
  ) {
    Optional<EmojiEntity> emoji =
      emojisService.getEmojiByCategoryAndName(category, name);

    if (emoji.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Emoji with specified category/name not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(emoji), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateEmoji(
    @PathVariable String id,
    @RequestBody EmojiDto updatedEmojiDto
  ) {
    Optional<EmojiEntity> emoji =
      emojisService.updateEmoji(id, updatedEmojiDto);

    if (emoji.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        NOT_FOUND_MESSAGE,
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(emoji), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteEmoji(
    @PathVariable String id
  ) {
    Optional<EmojiEntity> emoji = emojisService.deleteEmoji(id);

    if (emoji.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
