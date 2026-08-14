package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AuditResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;

public interface AuditServices {
  AuditResponseDto audit(RequestDto requestDto);
}
