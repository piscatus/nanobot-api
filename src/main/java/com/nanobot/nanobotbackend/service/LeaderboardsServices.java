package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.LeaderboardsResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import java.util.List;

public interface LeaderboardsServices {
  LeaderboardsResponseDto leaderboards(RequestDto requestDto);
}
