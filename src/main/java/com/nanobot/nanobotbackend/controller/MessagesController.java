package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.entity.MessageEntity;
import com.nanobot.nanobotbackend.service.MessagesService;
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
@RequestMapping("messages")
public class MessagesController {

  private MessagesService messagesService;

  private static final String NOT_FOUND_MESSAGE = "Message ID not found";

  public MessagesController(MessagesService messagesService) {
    this.messagesService = messagesService;
  }

  @PostMapping
  public ResponseEntity<MessageEntity> createMessage(
    @RequestBody MessageDto newMessageDto
  ) {
    return new ResponseEntity<>(
      messagesService.createMessage(newMessageDto),
      HttpStatus.CREATED
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getMessageById(
    @PathVariable String id
  ) {
    Optional<MessageEntity> message = messagesService.getMessageById(id);

    if (message.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(message), HttpStatus.OK);
  }

  @GetMapping
  public ResponseEntity<List<MessageEntity>> getMessages() {
    return new ResponseEntity<>(messagesService.getMessages(), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateMessage(
    @PathVariable String id,
    @RequestBody MessageDto updatedMessageDto
  ) {
    Optional<MessageEntity> message = messagesService.updateMessage(
      id,
      updatedMessageDto
    );

    if (message.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(message), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteMessage(
    @PathVariable String id
  ) {
    Optional<MessageEntity> message = messagesService.deleteMessage(id);

    if (message.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
