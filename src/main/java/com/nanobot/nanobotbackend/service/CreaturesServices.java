package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import java.util.List;

public interface CreaturesServices {
  CreaturesResponseDto creatures(RequestDto requestDto);
}
