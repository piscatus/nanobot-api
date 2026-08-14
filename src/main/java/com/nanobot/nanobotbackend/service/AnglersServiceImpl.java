package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.repository.AnglersRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AnglersServiceImpl implements AnglersService {

  private AnglersRepository anglersRepository;

  private final FileLogger fileLogger;

  private static final String UPDATE_BY_ID = "Angler updated with ID: ";

  private static final String NOT_FOUND_BY_ID = "Angler not found with ID: ";

  public AnglersServiceImpl(AnglersRepository anglersRepository) {
    this.fileLogger = new FileLogger("AnglersService");
    this.anglersRepository = anglersRepository;
  }

  @Override
  public Optional<AnglerEntity> createAngler(AnglerDto anglerDto) {
    AnglerEntity anglerEntity = new AnglerEntity(anglerDto);
    ObjectId id = new ObjectId();
    anglerEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating angler with guildId/userId: " +
      anglerEntity.getGuildId() +
      "/" +
      anglerEntity.getUserId()
    );
    try {
      AnglerEntity createdAngler = anglersRepository.insert(anglerEntity);
      fileLogger.info("Angler created with ID: " + createdAngler.getId());
      return Optional.of(createdAngler);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Angler already exists with specified guildId/userId: " +
        anglerDto.getGuildId() +
        "/" +
        anglerDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating angler: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<AnglerEntity> getAnglers(String guildId, String userId) {
    try {
      if (guildId == null && userId == null) {
        fileLogger.info("Fetching all anglers.");
        return anglersRepository.findAll();
      }
      fileLogger.info(
        "Fetching anglers with guildId/userId: " + guildId + "/" + userId
      );
      return anglersRepository.findByGuildIdAndUserId(guildId, userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching anglers: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<AnglerEntity> getRestingAnglers() {
    try {
      // fileLogger.info("Fetching all resting anglers.");
      return anglersRepository.findAllRestingAnglers();
    } catch (Exception e) {
      fileLogger.error("Error fetching resting anglers: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<AnglerEntity> getAnglerById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching angler with ID: " + id);
        return anglersRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching angler by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AnglerEntity> getAnglerByGuildIdAndUserId(
    String guildId,
    String userId
  ) {
    if (guildId != null && userId != null) {
      try {
        fileLogger.info(
          "Fetching angler with guildId/userId: " + guildId + "/" + userId
        );
        List<AnglerEntity> existingAngler =
          anglersRepository.findByGuildIdAndUserId(guildId, userId);
        if (existingAngler.size() > 1) {
          fileLogger.error(
            "Multiple anglers found with the same guildId/userId: " +
            guildId +
            "/" +
            userId
          );
        } else if (!existingAngler.isEmpty()) {
          return Optional.of(existingAngler.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching angler by guildId and userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AnglerEntity> updateAngler(String id, AnglerDto anglerDto) {
    if (id != null) {
      try {
        Optional<AnglerEntity> anglersOptional = anglersRepository.findById(id);
        if (anglersOptional.isPresent()) {
          AnglerEntity anglers = anglersOptional.get();
          anglers.setGuildId(anglerDto.getGuildId());
          anglers.setUserId(anglerDto.getUserId());
          anglers.setResting(anglerDto.getResting());
          anglers.setTimestamp(new Date());

          AnglerEntity updatedAngler = anglersRepository.save(anglers);
          fileLogger.info(UPDATE_BY_ID + id);
          return Optional.of(updatedAngler);
        } else {
          fileLogger.warn(NOT_FOUND_BY_ID + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating angler: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AnglerEntity> updateAnglerRestless(String id) {
    if (id != null) {
      try {
        Optional<AnglerEntity> anglersOptional = anglersRepository.findById(id);
        if (anglersOptional.isPresent()) {
          AnglerEntity anglers = anglersOptional.get();
          anglers.setResting(false);

          AnglerEntity updatedAngler = anglersRepository.save(anglers);
          fileLogger.info(UPDATE_BY_ID + id);
          return Optional.of(updatedAngler);
        } else {
          fileLogger.warn(NOT_FOUND_BY_ID + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating angler: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AnglerEntity> deleteAngler(String id) {
    if (id != null) {
      Optional<AnglerEntity> anglerOptional = anglersRepository.findById(id);

      if (anglerOptional.isPresent()) {
        AnglerEntity anglers = anglerOptional.get();
        anglersRepository.deleteById(id);
        fileLogger.info("Angler deleted with ID: " + id);
        return Optional.of(anglers);
      } else {
        fileLogger.warn(NOT_FOUND_BY_ID + id);
      }
    }
    return Optional.empty();
  }

  public AnglerEntity updateOrCreateAngler(String guildId, String userId) {
    // Search for an existing entry
    List<AnglerEntity> existingAnglers =
      anglersRepository.findByGuildIdAndUserId(guildId, userId);

    AnglerEntity angler;

    if (!existingAnglers.isEmpty()) {
      // If an existing entry is found, update its timestamp
      angler = existingAnglers.get(0); // Assuming the first match is what we want
      angler.setTimestamp(new Date());
      angler.setResting(true);
    } else {
      // If no entry is found, create a new one
      angler = new AnglerEntity(guildId, userId, true, new Date());

      // Set an ID for the new angler
      ObjectId id = new ObjectId();
      angler.setId(id.toHexString());
    }

    // Save the angler update / creation
    AnglerEntity updatedAngler = anglersRepository.save(angler);

    // Log the update / creation
    fileLogger.info(UPDATE_BY_ID + updatedAngler.getId());

    // Return the updated or new entry to the database
    return updatedAngler;
  }
}
