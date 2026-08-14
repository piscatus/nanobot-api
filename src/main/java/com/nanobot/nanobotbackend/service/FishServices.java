package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import java.util.List;

public interface FishServices {
  TransferResponseDto fish(RequestDto requestDto);
}
