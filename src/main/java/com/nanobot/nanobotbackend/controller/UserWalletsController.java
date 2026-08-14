package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.service.UserWalletsService;
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
@RequestMapping("userWallets")
public class UserWalletsController {

  private UserWalletsService userWalletsService;

  private static final String NOT_FOUND_MESSAGE = "Wallets ID not found";

  public UserWalletsController(UserWalletsService userWalletsService) {
    this.userWalletsService = userWalletsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createUserWallets(
    @RequestBody UserWalletsDto newUserWalletsDto
  ) {
    Optional<UserWalletsEntity> wallet = userWalletsService.createUserWallets(
      newUserWalletsDto
    );

    if (wallet.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "User Wallets userId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(wallet), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<UserWalletsEntity>> getUsersWallets(
    @RequestParam(required = false) String userId
  ) {
    return new ResponseEntity<>(
      userWalletsService.getUsersWallets(userId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getUserWalletsById(
    @PathVariable String id
  ) {
    Optional<UserWalletsEntity> wallet = userWalletsService.getUserWalletsById(
      id
    );

    if (wallet.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(wallet), HttpStatus.OK);
  }

  @GetMapping("/userId/{userId}")
  public ResponseEntity<Optional<Object>> getUserWalletsByUserId(
    @PathVariable String userId
  ) {
    Optional<UserWalletsEntity> user =
      userWalletsService.getUserWalletsByUserId(userId);

    if (user.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "User Wallets with specified userId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(user), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateUserWallet(
    @PathVariable String id,
    @RequestBody UserWalletsDto updatedUserWalletsDto
  ) {
    Optional<UserWalletsEntity> wallets = userWalletsService.updateUserWallets(
      id,
      updatedUserWalletsDto
    );

    if (wallets.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(wallets), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteWallet(
    @PathVariable String id
  ) {
    Optional<UserWalletsEntity> wallets = userWalletsService.deleteUserWallets(
      id
    );

    if (wallets.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
