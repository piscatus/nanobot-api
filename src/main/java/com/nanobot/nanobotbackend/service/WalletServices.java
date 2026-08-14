package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserWalletsResponseDto;

public interface WalletServices {
  UserWalletsResponseDto wallet(RequestDto requestDto);
}
