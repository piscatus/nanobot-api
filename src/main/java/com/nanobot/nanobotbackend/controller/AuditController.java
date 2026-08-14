package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.AuditResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.AuditServices;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("requests")
public class AuditController {

  private final AuditServices auditServices;

  public AuditController(AuditServices auditServices) {
    this.auditServices = auditServices;
  }

  @PostMapping("/audit")
  public ResponseEntity<AuditResponseDto> audit(
    @RequestBody RequestDto requestData
  ) {
    return new ResponseEntity<>(
      auditServices.audit(requestData),
      HttpStatus.OK
    );
  }
}
