package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.repository.GuildConfigurationsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.math.BigInteger;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.dao.OptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
public class GuildConfigurationsServiceImpl
  implements GuildConfigurationsService {

  private final FileLogger fileLogger;

  private GuildConfigurationsRepository guildConfigurationsRepository;

  public GuildConfigurationsServiceImpl(
    GuildConfigurationsRepository guildConfigurationsRepository
  ) {
    this.fileLogger = new FileLogger("GuildConfigurationsService");
    this.guildConfigurationsRepository = guildConfigurationsRepository;
  }

  @Override
  public boolean setGuildConfigurations(
    String guildId,
    Consumer<GuildConfigurationsDto> setGuildConfigurations
  ) {
    if (StringUtil.isValidString(guildId)) {
      GuildConfigurationsDto guildConfigurationsDto =
        new GuildConfigurationsDto(getOrCreateGuildConfiguration(guildId));
      setGuildConfigurations.accept(guildConfigurationsDto);
      return guildConfigurationsDto.getStatus() == StatusDto.ACTIVE;
    }
    return true;
  }

  @Override
  public Optional<GuildConfigurationsEntity> createGuildConfiguration(
    GuildConfigurationsDto guildConfigurationsDto
  ) {
    GuildConfigurationsEntity guildConfigurationsEntity =
      new GuildConfigurationsEntity(guildConfigurationsDto);
    ObjectId id = new ObjectId();
    guildConfigurationsEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating guild configuration with guildId: " +
      guildConfigurationsEntity.getGuildId()
    );
    try {
      GuildConfigurationsEntity createdGuildConfiguration =
        guildConfigurationsRepository.insert(guildConfigurationsEntity);
      fileLogger.info(
        "Guild Configuration created with ID: " +
        createdGuildConfiguration.getId()
      );
      return Optional.of(createdGuildConfiguration);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Guild Configuration already exists with specified guildId: " +
        guildConfigurationsDto.getGuildId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating guild configuration: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<GuildConfigurationsEntity> getGuildConfigurations(
    String guildId
  ) {
    try {
      if (guildId == null) {
        // fileLogger.info("Fetching all guild configurations.");
        return guildConfigurationsRepository.findAll();
      }
      fileLogger.info(
        "Fetching all guild configurations with guildId: " + guildId
      );
      return guildConfigurationsRepository.findByGuildId(guildId);
    } catch (Exception e) {
      fileLogger.error(
        "Error fetching guild configurations: " + e.getMessage()
      );
      throw e;
    }
  }

  @Override
  public Optional<GuildConfigurationsEntity> getGuildConfigurationById(
    String id
  ) {
    if (id != null) {
      try {
        fileLogger.info("Fetching guild configuration with ID: " + id);
        return guildConfigurationsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching guild configuration by ID: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<GuildConfigurationsEntity> getGuildConfigurationByGuildId(
    String guildId
  ) {
    if (guildId != null) {
      try {
        fileLogger.info(
          "Fetching guild configuration with guildId: " + guildId
        );
        List<GuildConfigurationsEntity> existingGuildConfiguration =
          guildConfigurationsRepository.findByGuildId(guildId);
        if (existingGuildConfiguration.size() > 1) {
          fileLogger.error(
            "Multiple guild configurations found with the same guildId"
          );
        } else if (!existingGuildConfiguration.isEmpty()) {
          return Optional.of(existingGuildConfiguration.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching guild configuration by guildId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<GuildConfigurationsEntity> updateGuildConfiguration(
    String id,
    GuildConfigurationsDto guildConfigurationsDto
  ) {
    if (id != null) {
      System.out.println("Updating guild configuration with ID: " + id);
      System.out.println(guildConfigurationsDto);
      try {
        Optional<GuildConfigurationsEntity> guildConfigOptional =
          guildConfigurationsRepository.findById(id);
        if (guildConfigOptional.isPresent()) {
          GuildConfigurationsEntity guildConfig = guildConfigOptional.get();
          if (guildConfigurationsDto.getGuildId() != null) {
            guildConfig.setGuildId(guildConfigurationsDto.getGuildId());
          }
          if (guildConfigurationsDto.getStatus() != null) {
            guildConfig.setStatus(guildConfigurationsDto.getStatus());
          }
          if (guildConfigurationsDto.getFishingLoggingChannelId() != null) {
            guildConfig.setFishingLoggingChannelId(
              guildConfigurationsDto.getFishingLoggingChannelId()
            );
          }
          if (guildConfigurationsDto.getTransferLoggingChannelId() != null) {
            guildConfig.setTransferLoggingChannelId(
              guildConfigurationsDto.getTransferLoggingChannelId()
            );
          }
          if (guildConfigurationsDto.getFishingChannelId() != null) {
            guildConfig.setFishingChannelId(
              guildConfigurationsDto.getFishingChannelId()
            );
          }
          if (guildConfigurationsDto.getFishingRole() != null) {
            guildConfig.setFishingRole(guildConfigurationsDto.getFishingRole());
          }
          guildConfig.setFishingError(guildConfigurationsDto.getFishingError());
          guildConfig.setFishingFrequency(
            guildConfigurationsDto.getFishingFrequency()
          );
          guildConfig.setMaximumMinutesActive(
            guildConfigurationsDto.getMaximumMinutesActive()
          );
          guildConfig.setMaximumActiveUsers(
            guildConfigurationsDto.getMaximumActiveUsers()
          );

          GuildConfigurationsEntity updatedGuildConfiguration =
            guildConfigurationsRepository.save(guildConfig);
          fileLogger.info("Guild Configuration updated with ID: " + id);
          return Optional.of(updatedGuildConfiguration);
        } else {
          fileLogger.warn("Guild Configuration not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error updating guild configuration: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<GuildConfigurationsEntity> deleteGuildConfiguration(
    String id
  ) {
    if (id != null) {
      try {
        Optional<GuildConfigurationsEntity> guildConfigOptional =
          guildConfigurationsRepository.findById(id);
        if (guildConfigOptional.isPresent()) {
          GuildConfigurationsEntity guildConfig = guildConfigOptional.get();
          guildConfigurationsRepository.deleteById(id);
          fileLogger.info("Guild Configuration deleted with ID: " + id);
          return Optional.of(guildConfig);
        } else {
          fileLogger.warn("Guild Configuration not found with ID: " + id);
        }
      } catch (Exception e) {}
    }
    return Optional.empty();
  }

  @Override
  public GuildConfigurationsEntity getOrCreateGuildConfiguration(
    String guildId
  ) {
    // Logging
    fileLogger.info("Fetching guild configuration with guildId: " + guildId);

    // Check if the guild configuration already exists
    List<GuildConfigurationsEntity> existingGuildConfigs =
      guildConfigurationsRepository.findByGuildId(guildId);

    if (!existingGuildConfigs.isEmpty()) {
      return existingGuildConfigs.get(0);
    } else if (existingGuildConfigs.size() > 1) {
      throw new Error(
        "Multiple guilds found with the same guild ID: " + guildId
      );
    } else {
      // Create a new GuildConfigurationsDto with default values
      GuildConfigurationsDto newGuildConfigDto = new GuildConfigurationsDto();
      newGuildConfigDto.setGuildId(guildId);
      newGuildConfigDto.setStatus(StatusDto.ACTIVE);

      // Convert the DTO to an entity
      GuildConfigurationsEntity newGuildConfigEntity =
        new GuildConfigurationsEntity(newGuildConfigDto);

      // Assign a new ID
      ObjectId id = new ObjectId();
      newGuildConfigEntity.setId(id.toHexString());

      // Save the new guild configuration to the repository
      GuildConfigurationsEntity updatedGuildConfiguration =
        guildConfigurationsRepository.save(newGuildConfigEntity);

      // Log the update / creation
      fileLogger.info(
        "Guild Configuration created with ID: " +
        updatedGuildConfiguration.getId()
      );

      // Return the updated or new entry
      return updatedGuildConfiguration;
    }
  }

  public GuildConfigurationsEntity saveGuildConfigurations(
    GuildConfigurationsEntity guildConfigurationsEntity
  ) {
    return guildConfigurationsRepository.save(guildConfigurationsEntity);
  }

  public GuildConfigurationsEntity insertGuildConfigurations(
    GuildConfigurationsEntity guildConfigurationsEntity
  ) {
    return guildConfigurationsRepository.insert(guildConfigurationsEntity);
  }
}
