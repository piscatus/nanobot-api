package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsResponseDto;

public interface InventoryServices {
  UserItemsResponseDto inventory(RequestDto requestDto);
}
