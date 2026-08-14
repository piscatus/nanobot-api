package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import com.nanobot.nanobotbackend.service.AliasesService;
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
@RequestMapping("aliases")
public class AliasesController {

  private AliasesService aliasesService;

  private static final String NOT_FOUND_MESSAGE = "Alias ID not found";

  public AliasesController(AliasesService aliasesService) {
    this.aliasesService = aliasesService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createAlias(
    @RequestBody AliasDto newAliasDto
  ) {
    Optional<AliasEntity> alias = aliasesService.createAlias(newAliasDto);

    if (alias.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Alias guildId/singular already exists",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(alias), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<AliasEntity>> getAliases(
    @RequestParam(required = false) String guildId,
    @RequestParam(required = false) String singular
  ) {
    return new ResponseEntity<>(
      aliasesService.getAliases(guildId, singular),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getAliasById(
    @PathVariable String id
  ) {
    Optional<AliasEntity> aliases = aliasesService.getAliasById(id);

    if (aliases.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(aliases), HttpStatus.OK);
  }

  @GetMapping("/guildId/{guildId}/singular/{singular}")
  public ResponseEntity<Optional<Object>> getAliasByGuildIdAndSingular(
    @PathVariable String guildId,
    @PathVariable String singular
  ) {
    Optional<AliasEntity> alias = aliasesService.getAliasByGuildIdAndSingular(
      guildId,
      singular
    );

    if (alias.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Alias with specified guildId/singular not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(alias), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateAlias(
    @PathVariable String id,
    @RequestBody AliasDto updatedAliasDto
  ) {
    Optional<AliasEntity> aliases = aliasesService.updateAlias(
      id,
      updatedAliasDto
    );

    if (aliases.isEmpty()) {
      ErrorDto error = new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "");
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(aliases), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteAlias(@PathVariable String id) {
    Optional<AliasEntity> aliases = aliasesService.deleteAlias(id);

    if (aliases.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, NOT_FOUND_MESSAGE, "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
