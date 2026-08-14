package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;

public interface SendServices {
  TransferResponseDto update(RequestDto requestDto);

  TransferResponseDto send(RequestDto requestDto);
}
