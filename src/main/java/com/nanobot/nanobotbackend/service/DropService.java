package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;

public interface DropService {
  void checkDropsByPickupSize();

  void checkDropsByTime();

  BaseResponseDto dropUpdate(RequestDto requestDto);
}
