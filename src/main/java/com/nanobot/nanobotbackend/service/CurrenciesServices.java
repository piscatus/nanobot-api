package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrenciesResponseDto;
import com.nanobot.nanobotbackend.dto.HelpResponseDto;
import com.nanobot.nanobotbackend.dto.ReceiveResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;

public interface CurrenciesServices {
  CurrenciesResponseDto currencies(RequestDto requestDto);

  HelpResponseDto help(RequestDto requestDto);

  ReceiveResponseDto receive(RequestDto requestDto);
}
