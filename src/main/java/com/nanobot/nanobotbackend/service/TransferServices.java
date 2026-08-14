package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import java.util.List;

public interface TransferServices {
  TransferResponseDto drop(RequestDto requestDto);

  TransferResponseDto gift(RequestDto requestDto);

  TransferResponseDto merge(RequestDto requestDto);

  TransferResponseDto rain(RequestDto requestDto);

  TransferResponseDto sell(RequestDto requestDto);
}
