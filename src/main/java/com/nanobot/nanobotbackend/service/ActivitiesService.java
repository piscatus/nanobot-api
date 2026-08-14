package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.ActivityEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface ActivitiesService {
  Optional<ActivityEntity> createActivity(ActivityDto activityDto);

  List<ActivityEntity> getActivities(
    String guildId,
    String channelId,
    String userId
  );

  List<ActivityEntity> getActiveActivities(
    RequestDto requestDto,
    TransferResponseDto transferResponseDto,
    boolean filterOutUserId
  );

  Optional<ActivityEntity> getActivityById(String id);

  Optional<ActivityEntity> updateActivity(String id, ActivityDto activityDto);

  Optional<ActivityEntity> deleteActivity(String id);

  ActivityEntity updateOrCreateActivity(
    String guildId,
    String channelId,
    String userId
  );

  void setActivities(
    RequestDto requestDto,
    boolean filterOutUserId,
    TransferResponseDto transferResponseDto,
    Consumer<List<ActivityDto>> setActivities
  );
}
