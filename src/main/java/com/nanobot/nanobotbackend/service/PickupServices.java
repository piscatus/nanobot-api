package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;

public interface PickupServices {
  BaseResponseDto pickup(RequestDto requestDto);
}
