package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ConfigResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import java.util.List;

public interface ConfigServices {
  ConfigResponseDto config(GuildConfigurationsDto requestDto);
}
