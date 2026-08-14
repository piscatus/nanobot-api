package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import java.util.List;
import java.util.Optional;

public interface LeaderboardsService {
  Optional<LeaderboardEntity> createLeaderboard(LeaderboardDto leaderboardDto);

  List<LeaderboardEntity> getLeaderboards(String guildId, String userId);

  Optional<LeaderboardEntity> getLeaderboardById(String userId);

  Optional<LeaderboardEntity> getLeaderboardByGuildIdAndUserId(
    String guildId,
    String userId
  );

  Optional<LeaderboardEntity> updateLeaderboard(
    String id,
    LeaderboardDto leaderboardDto
  );

  Optional<LeaderboardEntity> deleteLeaderboard(String id);

  LeaderboardEntity incrementOrCreateLeaderboard(
    String guildId,
    String userId,
    CreatureDto creature
  );
}
