package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.ServerResponseDto;

public interface ServerServices {
  ServerResponseDto configurations(RequestDto requestDto);

  ServerResponseDto reserves(RequestDto requestDto);
}
