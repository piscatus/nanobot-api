package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;

public interface AliasesServices {
  AliasesResponseDto aliases(RequestDto requestDto);
}
