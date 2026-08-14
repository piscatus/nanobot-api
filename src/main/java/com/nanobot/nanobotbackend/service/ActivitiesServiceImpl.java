package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.entity.ActivityEntity;
import com.nanobot.nanobotbackend.repository.ActivitiesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.security.SecureRandom;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ActivitiesServiceImpl implements ActivitiesService {

  private ActivitiesRepository activitiesRepository;

  private final FileLogger fileLogger;

  public ActivitiesServiceImpl(ActivitiesRepository activitiesRepository) {
    this.fileLogger = new FileLogger("ActivitiesService");
    this.activitiesRepository = activitiesRepository;
  }

  @Override
  public void setActivities(
    RequestDto requestDto,
    boolean filterOutUserId,
    TransferResponseDto transferResponseDto,
    Consumer<List<ActivityDto>> setActivities
  ) {
    if (
      StringUtil.isValidString(requestDto.getGuildId()) &&
      StringUtil.isValidString(requestDto.getChannelId()) &&
      StringUtil.isValidString(requestDto.getUserId())
    ) {
      List<ActivityEntity> activitiesEntityList = getActiveActivities(
        requestDto,
        transferResponseDto,
        filterOutUserId
      );

      setActivities.accept(
        activitiesEntityList
          .stream()
          .map(ActivityDto::new)
          .collect(Collectors.toList())
      );
    }
  }

  @Override
  public Optional<ActivityEntity> createActivity(ActivityDto activityDto) {
    ActivityEntity activityEntity = new ActivityEntity(activityDto);
    activityEntity.setId(new ObjectId().toHexString());
    fileLogger.info(
      "Creating activity with userId: " + activityEntity.getUserId()
    );
    try {
      ActivityEntity createdActivity = activitiesRepository.insert(
        activityEntity
      );
      fileLogger.info("Activity created with ID: " + createdActivity.getId());
      return Optional.of(createdActivity);
    } catch (DuplicateKeyException e) {
      fileLogger.error(
        "Activity already exists with specified guildId/channelId/userId: " +
        activityDto.getGuildId() +
        "/" +
        activityDto.getChannelId() +
        "/" +
        activityDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating activity: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<ActivityEntity> getActivities(
    String guildId,
    String channelId,
    String userId
  ) {
    try {
      if (guildId == null && channelId == null && userId == null) {
        fileLogger.info("Fetching all activities.");
        return activitiesRepository.findAll();
      } else if (channelId == null && userId == null) {
        fileLogger.info("Fetching activities by guildId: " + guildId);
        return activitiesRepository.findByGuildId(guildId);
      } else if (userId == null) {
        fileLogger.info(
          "Fetching activities by guildId/channelId: " +
          guildId +
          "/" +
          channelId
        );
        return activitiesRepository.findByGuildIdAndChannelId(
          guildId,
          channelId
        );
      }
      fileLogger.info(
        "Fetching activities with guildId/channelId/userId: " +
        guildId +
        "/" +
        channelId +
        "/" +
        userId
      );
      return activitiesRepository.findByGuildIdAndChannelIdAndUserId(
        guildId,
        channelId,
        userId
      );
    } catch (Exception e) {
      fileLogger.error("Error fetching activities: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<ActivityEntity> getActiveActivities(
    RequestDto requestDto,
    TransferResponseDto transferResponseDto,
    boolean filterOutUserId
  ) {
    try {
      if (
        !StringUtil.isValidString(requestDto.getGuildId()) ||
        !StringUtil.isValidString(requestDto.getChannelId()) ||
        !StringUtil.isValidString(requestDto.getUserId())
      ) {
        fileLogger.error(
          "Cannot fetch active activities without necessary input."
        );
        return new ArrayList<>();
      }

      if (
        requestDto.getUserIdsWithRole() != null &&
        requestDto.getUserIdsWithRole().isEmpty()
      ) {
        return new ArrayList<>();
      }

      int activeUsers = (requestDto.getUsers() == null ||
          requestDto.getUsers() == 0)
        ? Optional.ofNullable(
          transferResponseDto.getGuildConfigurations().getMaximumActiveUsers()
        )
          .filter(u -> u > 0)
          .orElse(Constants.defaultActiveUsers)
        : requestDto.getUsers();

      int minutesActive = (requestDto.getDuration() == null ||
          requestDto.getDuration() == 0)
        ? Optional.ofNullable(
          transferResponseDto.getGuildConfigurations().getMaximumMinutesActive()
        )
          .filter(d -> d > 0)
          .orElse(Constants.defaultMinutesActive)
        : requestDto.getDuration();

      List<ActivityEntity> unfilteredActivities =
        activitiesRepository.findByGuildIdAndChannelId(
          requestDto.getGuildId(),
          requestDto.getChannelId()
        );

      long currentTime = System.currentTimeMillis();
      long maxActiveMillis = minutesActive * 60000;

      List<ActivityEntity> filteredActivities = unfilteredActivities
        .stream()
        .filter(
          activity ->
            !filterOutUserId ||
            !activity.getUserId().equals(requestDto.getUserId())
        )
        .filter(
          activity ->
            (currentTime -
              activity.getTimestamp().toInstant().toEpochMilli()) <=
            maxActiveMillis
        )
        .filter(activity -> {
          if (requestDto.getUserIdsWithRole() != null) {
            return requestDto
              .getUserIdsWithRole()
              .contains(activity.getUserId());
          }
          return true;
        })
        .sorted((a, b) -> b.getTimestamp().compareTo(a.getTimestamp()))
        .limit(activeUsers)
        .collect(Collectors.toList());

      if (
        (requestDto.getRandom() == null || requestDto.getRandom() == 0) &&
        requestDto.getUsers() != null &&
        requestDto.getUsers() != 0
      ) {
        if (
          !filteredActivities.isEmpty() &&
          requestDto.getUsers() > filteredActivities.size()
        ) {
          transferResponseDto.setErrorMessage(
            "The number of users specified (" +
            requestDto.getUsers() +
            ") exceeds the number of users currently active given your criteria (" +
            filteredActivities.size() +
            ")."
          );
          return filteredActivities;
        } else {
          filteredActivities = filteredActivities
            .stream()
            .limit(activeUsers)
            .collect(Collectors.toList());
        }
      }

      if (
        requestDto.getRandom() != null &&
        requestDto.getRandom() > 0 &&
        requestDto.getRandom() < filteredActivities.size()
      ) {
        if (
          !filteredActivities.isEmpty() &&
          requestDto.getRandom() >= filteredActivities.size()
        ) {
          transferResponseDto.setErrorMessage(
            "The random number specified (" +
            requestDto.getRandom() +
            ") must be less than the number of users currently active given your criteria (" +
            filteredActivities.size() +
            ")."
          );
          return filteredActivities;
        } else {
          Collections.shuffle(filteredActivities, new SecureRandom());
          filteredActivities = filteredActivities.subList(
            0,
            requestDto.getRandom()
          );
        }
      }

      return filteredActivities;
    } catch (Exception e) {
      fileLogger.error("Error fetching activities: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<ActivityEntity> getActivityById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching activity with ID: " + id);
        return activitiesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching activity by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<ActivityEntity> updateActivity(
    String id,
    ActivityDto activityDto
  ) {
    if (id != null) {
      try {
        Optional<ActivityEntity> activitiesOptional =
          activitiesRepository.findById(id);
        if (activitiesOptional.isPresent()) {
          ActivityEntity activities = activitiesOptional.get();
          activities.setGuildId(activityDto.getGuildId());
          activities.setChannelId(activityDto.getChannelId());
          activities.setUserId(activityDto.getUserId());
          activities.setTimestamp(new Date());

          ActivityEntity updatedActivity = activitiesRepository.save(
            activities
          );
          fileLogger.info("Activity updated with ID: " + id);
          return Optional.of(updatedActivity);
        } else {
          fileLogger.warn("Activity not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating activity: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<ActivityEntity> deleteActivity(String id) {
    if (id != null) {
      try {
        Optional<ActivityEntity> activitiesOptional =
          activitiesRepository.findById(id);
        if (activitiesOptional.isPresent()) {
          ActivityEntity activities = activitiesOptional.get();
          activitiesRepository.deleteById(id);
          fileLogger.info("Activity deleted with ID: " + id);
          return Optional.of(activities);
        } else {
          fileLogger.warn("Activity not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting activity: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  public ActivityEntity updateOrCreateActivity(
    String guildId,
    String channelId,
    String userId
  ) {
    //Logging
    fileLogger.info(
      "Fetching activity with guildId/channelId/userId: " +
      guildId +
      "/" +
      channelId +
      "/" +
      userId
    );

    // Search for an existing entry
    List<ActivityEntity> existingActivities =
      activitiesRepository.findByGuildIdAndChannelIdAndUserId(
        guildId,
        channelId,
        userId
      );

    ActivityEntity activity;

    if (!existingActivities.isEmpty()) {
      // If an existing entry is found, update its timestamp
      activity = existingActivities.get(0); // Assuming the first match is what we want
      activity.setTimestamp(new Date());
    } else {
      // If no entry is found, create a new one
      activity = new ActivityEntity();
      activity.setGuildId(guildId);
      activity.setChannelId(channelId);
      activity.setUserId(userId);
      activity.setTimestamp(new Date());

      // Set an ID for the new activity
      ObjectId id = new ObjectId();
      activity.setId(id.toHexString());
    }

    // Save the activity update / creation
    ActivityEntity updatedActivity = activitiesRepository.save(activity);

    // Log the update / creation
    fileLogger.info("Activity created with ID: " + updatedActivity.getId());

    // Return the updated or new entry
    return updatedActivity;
  }
}
