package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransactionsResponseDto;
import java.util.List;

public interface TransactionsServices {
  TransactionsResponseDto transactions(RequestDto requestDto);
}
