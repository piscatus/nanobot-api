package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.service.GuildConfigurationsService;
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
@RequestMapping("guildConfigurations")
public class GuildConfigurationsController {

  private GuildConfigurationsService guildConfigurationsService;

  private static final String NOT_FOUND_MESSAGE =
    "Guild Configuration ID not found";

  public GuildConfigurationsController(
    GuildConfigurationsService guildConfigurationsService
  ) {
    this.guildConfigurationsService = guildConfigurationsService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createGuildConfiguration(
    @RequestBody GuildConfigurationsDto newGuildConfigurationsDto
  ) {
    Optional<GuildConfigurationsEntity> guildConfiguration =
      guildConfigurationsService.createGuildConfiguration(
        newGuildConfigurationsDto
      );

    if (guildConfiguration.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Guild Configuration guildId already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(
      Optional.of(guildConfiguration),
      HttpStatus.CREATED
    );
  }

  @GetMapping
  public ResponseEntity<List<GuildConfigurationsEntity>> getGuildConfigurations(
    @RequestParam(required = false) String guildId
  ) {
    return new ResponseEntity<>(
      guildConfigurationsService.getGuildConfigurations(guildId),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getGuildConfigurationById(
    @PathVariable String id
  ) {
    Optional<GuildConfigurationsEntity> guildConfiguration =
      guildConfigurationsService.getGuildConfigurationById(id);

    if (guildConfiguration.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(guildConfiguration), HttpStatus.OK);
  }

  @GetMapping("/guildId/{guildId}")
  public ResponseEntity<Optional<Object>> getGuildConfigurationByGuildId(
    @PathVariable String guildId
  ) {
    Optional<GuildConfigurationsEntity> guild =
      guildConfigurationsService.getGuildConfigurationByGuildId(guildId);

    if (guild.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Guild Configuration with specified guildId not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(guild), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateGuildConfiguration(
    @PathVariable String id,
    @RequestBody GuildConfigurationsDto updatedGuildConfigurationsDto
  ) {
    Optional<GuildConfigurationsEntity> guildConfiguration =
      guildConfigurationsService.updateGuildConfiguration(
        id,
        updatedGuildConfigurationsDto
      );

    if (guildConfiguration.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(
      Optional.of(guildConfiguration),
      HttpStatus.ACCEPTED
    );
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteGuildConfiguration(
    @PathVariable String id
  ) {
    Optional<GuildConfigurationsEntity> guildConfiguration =
      guildConfigurationsService.deleteGuildConfiguration(id);

    if (guildConfiguration.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }

  @GetMapping("/getOrCreate/{guildId}")
  public ResponseEntity<
    GuildConfigurationsEntity
  > getOrCreateGuildConfiguration(@PathVariable String guildId) {
    GuildConfigurationsEntity guildConfiguration =
      guildConfigurationsService.getOrCreateGuildConfiguration(guildId);

    return new ResponseEntity<>(guildConfiguration, HttpStatus.OK);
  }
}
