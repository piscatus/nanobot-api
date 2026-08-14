package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.service.GuildWalletsService;
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
@RequestMapping("guildWallets")
public class GuildWalletsController {

  private GuildWalletsService guildWalletsService;

  private static final String NOT_FOUND_MESSAGE = "Wallets ID not found";

  public GuildWalletsController(GuildWalletsService guildWalletsService) {
    this.guildWalletsService = guildWalletsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createGuildWallets(
    @RequestBody GuildWalletsDto newGuildWalletsDto
  ) {
    Optional<GuildWalletsEntity> wallet =
      guildWalletsService.createGuildWallets(newGuildWalletsDto);

    if (wallet.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Guild Wallets guildId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(wallet), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<GuildWalletsEntity>> getGuildsWallets(
    @RequestParam(required = false) String guildId
  ) {
    return new ResponseEntity<>(
      guildWalletsService.getGuildsWallets(guildId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getGuildWalletsById(
    @PathVariable String id
  ) {
    Optional<GuildWalletsEntity> wallet =
      guildWalletsService.getGuildWalletsById(id);

    if (wallet.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(wallet), HttpStatus.OK);
  }

  @GetMapping("/guildId/{guildId}")
  public ResponseEntity<Optional<Object>> getGuildWalletsByGuildId(
    @PathVariable String guildId
  ) {
    Optional<GuildWalletsEntity> guild =
      guildWalletsService.getGuildWalletsByGuildId(guildId);

    if (guild.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Guild Wallets with specified guildId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(guild), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateGuildWallet(
    @PathVariable String id,
    @RequestBody GuildWalletsDto updatedGuildWalletsDto
  ) {
    Optional<GuildWalletsEntity> wallets =
      guildWalletsService.updateGuildWallets(id, updatedGuildWalletsDto);

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
    Optional<GuildWalletsEntity> wallets =
      guildWalletsService.deleteGuildWallets(id);

    if (wallets.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
