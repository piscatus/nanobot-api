package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.service.UserItemsService;
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
@RequestMapping("userItems")
public class UserItemsController {

  private UserItemsService userItemsService;

  private static final String NOT_FOUND_MESSAGE = "Items ID not found";

  public UserItemsController(UserItemsService userItemsService) {
    this.userItemsService = userItemsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createItem(
    @RequestBody UserItemsDto newUserItemsDto
  ) {
    Optional<UserItemsEntity> userItems = userItemsService.createUserItems(
      newUserItemsDto
    );

    if (userItems.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "User Items userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(userItems), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<UserItemsEntity>> getItems(
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      userItemsService.getUsersItems(userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getItemById(@PathVariable String id) {
    Optional<UserItemsEntity> item = userItemsService.getUserItemsById(id);

    if (item.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(item), HttpStatus.OK);
  }

  @GetMapping("/userId/{userId}")
  public ResponseEntity<Optional<Object>> getUserItemsByUserId(
    @PathVariable String userId
  ) {
    Optional<UserItemsEntity> user = userItemsService.getUserItemsByUserId(
      userId
    );

    if (user.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "User Items with specified userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(user), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateItem(
    @PathVariable String id,
    @RequestBody UserItemsDto updatedUserItemsDto
  ) {
    Optional<UserItemsEntity> items = userItemsService.updateUserItems(
      id,
      updatedUserItemsDto
    );

    if (items.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(items), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteUserItems(
    @PathVariable String id
  ) {
    Optional<UserItemsEntity> user = userItemsService.deleteUserItems(id);

    if (user.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
