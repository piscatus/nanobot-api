package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.service.CommandsService;
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
@RequestMapping("commands")
public class CommandsController {

  private CommandsService commandsService;

  private static final String NOT_FOUND_MESSAGE = "Command ID not found";

  public CommandsController(CommandsService commandsService) {
    this.commandsService = commandsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createCommand(
    @RequestBody CommandDto newCommandDto
  ) {
    Optional<CommandEntity> command = commandsService.createCommand(
      newCommandDto
    );

    if (command.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Command name already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(command), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<CommandEntity>> getCommands(
    @RequestParam(required = false) String name
  ) {
    return new ResponseEntity<>(
      commandsService.getCommands(name),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getCommandById(
    @PathVariable String id
  ) {
    Optional<CommandEntity> command = commandsService.getCommandById(id);

    if (command.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(command), HttpStatus.OK);
  }

  @GetMapping("/name/{name}")
  public ResponseEntity<Optional<Object>> getCommandByName(
    @PathVariable String name
  ) {
    Optional<CommandEntity> command = commandsService.getCommandByName(name);

    if (command.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Command with specified name not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(command), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateCommand(
    @PathVariable String id,
    @RequestBody CommandDto updatedCommandDto
  ) {
    Optional<CommandEntity> command = commandsService.updateCommand(
      id,
      updatedCommandDto
    );

    if (command.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(command), HttpStatus.ACCEPTED);
  }

  @PutMapping
  public ResponseEntity<Optional<Object>> updateCommands(
    @RequestBody List<CommandDto> commands
  ) {
    List<CommandDto> updatedCommands = commandsService.updateCommands(commands);

    if (updatedCommands.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Commands not updated",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(
      Optional.of(updatedCommands),
      HttpStatus.ACCEPTED
    );
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteCommand(
    @PathVariable String id
  ) {
    Optional<CommandEntity> command = commandsService.deleteCommand(id);

    if (command.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
