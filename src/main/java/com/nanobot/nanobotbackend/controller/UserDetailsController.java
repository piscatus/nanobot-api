package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.service.UserDetailsService;
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
@RequestMapping("userDetails")
public class UserDetailsController {

  private UserDetailsService userDetailsService;

  public UserDetailsController(UserDetailsService userDetailsService) {
    this.userDetailsService = userDetailsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createUser(
    @RequestBody UserDetailsDto newUserDetailsDto
  ) {
    Optional<UserDetailsEntity> userDetails =
      userDetailsService.createUserDetails(newUserDetailsDto);

    if (userDetails.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "User Details userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(userDetails), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<UserDetailsEntity>> getUsers(
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      userDetailsService.getUsersDetails(userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getUserDetailsById(
    @PathVariable String id
  ) {
    Optional<UserDetailsEntity> user = userDetailsService.getUserDetailsById(
      id
    );

    if (user.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Details ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(user), HttpStatus.OK);
  }

  @GetMapping("/userId/{userId}")
  public ResponseEntity<Optional<Object>> getUserDetailsByUserId(
    @PathVariable String userId
  ) {
    Optional<UserDetailsEntity> user =
      userDetailsService.getUserDetailsByUserId(userId);

    if (user.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "User with specified userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(user), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateUserDetails(
    @PathVariable String id,
    @RequestBody UserDetailsDto updatedUserDetailsDto
  ) {
    Optional<UserDetailsEntity> user = userDetailsService.updateUserDetails(
      id,
      updatedUserDetailsDto
    );

    if (user.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Details ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(user), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteUserDetails(
    @PathVariable String id
  ) {
    Optional<UserDetailsEntity> user = userDetailsService.deleteUserDetails(id);

    if (user.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(
          new ErrorDto(new Date(), 404, "User Details ID not found", "")
        ),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/active")
  public ResponseEntity<List<UserDetailsEntity>> getActiveUsers() {
    return new ResponseEntity<>(
      userDetailsService.getActiveUsersDetails(),
      HttpStatus.OK
    );
  }

  @GetMapping("/locked")
  public ResponseEntity<List<UserDetailsEntity>> getLockedUsers() {
    return new ResponseEntity<>(
      userDetailsService.getLockedUsersDetails(),
      HttpStatus.OK
    );
  }

  @GetMapping("/banned")
  public ResponseEntity<List<UserDetailsEntity>> getBannedUsers() {
    return new ResponseEntity<>(
      userDetailsService.getBannedUsersDetails(),
      HttpStatus.OK
    );
  }

  @GetMapping("/getOrCreate/{userId}")
  public ResponseEntity<Optional<Object>> getOrCreateUserDetails(
    @PathVariable String userId
  ) {
    UserDetailsEntity userDetails = userDetailsService.getOrCreateUserDetails(
      userId
    );

    return new ResponseEntity<>(Optional.of(userDetails), HttpStatus.OK);
  }
}
