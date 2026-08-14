package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.repository.LeaderboardsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class LeaderboardsServiceImpl implements LeaderboardsService {

  private LeaderboardsRepository leaderboardsRepository;

  private final FileLogger fileLogger;

  public LeaderboardsServiceImpl(
    LeaderboardsRepository leaderboardsRepository
  ) {
    this.fileLogger = new FileLogger("LeaderboardsService");
    this.leaderboardsRepository = leaderboardsRepository;
  }

  @Override
  public Optional<LeaderboardEntity> createLeaderboard(
    LeaderboardDto leaderboardDto
  ) {
    LeaderboardEntity leaderboardEntity = new LeaderboardEntity(leaderboardDto);
    ObjectId id = new ObjectId();
    leaderboardEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating leaderboard with guildId/userId: " +
      leaderboardEntity.getGuildId() +
      "/" +
      leaderboardEntity.getUserId()
    );
    try {
      LeaderboardEntity createdLeaderboard = leaderboardsRepository.insert(
        leaderboardEntity
      );
      fileLogger.info(
        "Leaderboard created with ID: " + createdLeaderboard.getId()
      );
      return Optional.of(createdLeaderboard);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Leaderboard already exists with specified guildId/userId: " +
        leaderboardDto.getGuildId() +
        "/" +
        leaderboardDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating leaderboard: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<LeaderboardEntity> getLeaderboards(
    String guildId,
    String userId
  ) {
    try {
      if (guildId == null && userId == null) {
        fileLogger.info("Fetching all leaderboards.");
        return leaderboardsRepository.findAll();
      }
      if (userId == null) {
        fileLogger.info("Fetching leaderboards with guildId: " + guildId);
        return leaderboardsRepository.findByGuildId(guildId);
      }
      fileLogger.info(
        "Fetching leaderboards with guildId/userId: " + guildId + "/" + userId
      );
      return leaderboardsRepository.findByGuildIdAndUserId(guildId, userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching leaderboards: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<LeaderboardEntity> getLeaderboardById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching leaderboard with ID: " + id);
        return leaderboardsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching leaderboard: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<LeaderboardEntity> getLeaderboardByGuildIdAndUserId(
    String guildId,
    String userId
  ) {
    if (guildId != null && userId != null) {
      try {
        fileLogger.info(
          "Fetching leaderboard with guildId/userId: " + guildId + "/" + userId
        );
        List<LeaderboardEntity> existingLeaderboards =
          leaderboardsRepository.findByGuildIdAndUserId(guildId, userId);
        if (existingLeaderboards.size() > 1) {
          fileLogger.error(
            "Multiple leaderboards found with the same guildId/userId: " +
            guildId +
            "/" +
            userId
          );
        } else if (!existingLeaderboards.isEmpty()) {
          return Optional.of(existingLeaderboards.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching leaderboard by guildId and userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<LeaderboardEntity> updateLeaderboard(
    String id,
    LeaderboardDto leaderboardDto
  ) {
    if (id != null) {
      try {
        Optional<LeaderboardEntity> leaderboardsOptional =
          leaderboardsRepository.findById(id);
        if (leaderboardsOptional.isPresent()) {
          LeaderboardEntity leaderboards = leaderboardsOptional.get();
          leaderboards.setGuildId(leaderboardDto.getGuildId());
          leaderboards.setUserId(leaderboardDto.getUserId());
          leaderboards.setItems(leaderboardDto.getItems());

          LeaderboardEntity updatedLeaderboard = leaderboardsRepository.save(
            leaderboards
          );
          fileLogger.info("Leaderboard updated with ID: " + id);
          return Optional.of(updatedLeaderboard);
        } else {
          fileLogger.warn("Leaderboard not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating leaderboard: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<LeaderboardEntity> deleteLeaderboard(String id) {
    if (id != null) {
      try {
        Optional<LeaderboardEntity> leaderboardsOptional =
          leaderboardsRepository.findById(id);
        if (leaderboardsOptional.isPresent()) {
          LeaderboardEntity leaderboards = leaderboardsOptional.get();
          List<ItemDto> emptyItems = leaderboards.getItems();
          for (ItemDto item : emptyItems) {
            item.setQuantity(0);
          }
          leaderboards.setItems(emptyItems);
          LeaderboardEntity updatedItems = leaderboardsRepository.save(
            leaderboards
          );
          fileLogger.info("Leaderboard data removed with ID: " + id);
          return Optional.of(updatedItems);
        } else {
          fileLogger.warn("Leaderboard not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting leaderboard: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public LeaderboardEntity incrementOrCreateLeaderboard(
    String guildId,
    String userId,
    CreatureDto creature
  ) {
    fileLogger.info(
      "Fetching leaderboard with guildId/userId: " + guildId + "/" + userId
    );

    // Search for an existing entry
    List<LeaderboardEntity> existingLeaderboard =
      leaderboardsRepository.findByGuildIdAndUserId(guildId, userId);

    LeaderboardEntity leaderboard;

    if (!existingLeaderboard.isEmpty()) {
      // If an existing entry is found, increment the creature
      leaderboard = existingLeaderboard.get(0);

      // Check if the creature exists in the leaderboard's items list
      boolean creatureFound = false;

      for (ItemDto item : leaderboard.getItems()) {
        if (item.getName().equalsIgnoreCase(creature.getName())) {
          // Increment the count for the matching creature
          item.setQuantity(item.getQuantity() + 1);
          item.setTimestamp(new Date());
          creatureFound = true;
          break; // Exit loop once we find and increment the item
        }
      }

      // If the creature wasn't found in the existing leaderboard, add it as a new item
      if (!creatureFound) {
        ItemDto newItem = new ItemDto(creature.getName().toUpperCase(), 1);
        newItem.setTimestamp(new Date());
        leaderboard.getItems().add(newItem);
      }
    } else {
      // If no entry is found, create a new one
      leaderboard = new LeaderboardEntity(guildId, userId, creature);

      ObjectId id = new ObjectId();
      leaderboard.setId(id.toHexString());
    }

    // Save the leaderboard update / creation
    LeaderboardEntity updatedLeaderboard = leaderboardsRepository.save(
      leaderboard
    );

    // Log the update / creation
    fileLogger.info(
      "Leaderboard created with ID: " + updatedLeaderboard.getId()
    );

    // Return the updated or new entry
    return updatedLeaderboard;
  }
}
