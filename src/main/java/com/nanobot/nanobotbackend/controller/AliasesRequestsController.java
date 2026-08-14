package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.AliasesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.AliasesServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests/aliases")
public class AliasesRequestsController {

  private final AliasesServices aliasesServices;

  public AliasesRequestsController(AliasesServices aliasesServices) {
    this.aliasesServices = aliasesServices;
  }

  @PostMapping
  public ResponseEntity<AliasesResponseDto> aliases(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      aliasesServices.aliases(requestData),
      HttpStatus.OK
    );
  }
}
