package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.repository.PickupsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class PickupsServiceImpl implements PickupsService {

  private PickupsRepository pickupsRepository;

  private final FileLogger fileLogger;

  public PickupsServiceImpl(PickupsRepository pickupsRepository) {
    this.fileLogger = new FileLogger("PickupsService");
    this.pickupsRepository = pickupsRepository;
  }

  @Override
  public Optional<PickupEntity> createPickup(PickupDto pickupDto) {
    PickupEntity pickupEntity = new PickupEntity(pickupDto);
    ObjectId id = new ObjectId();
    pickupEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating pickup with dropId/userId: " +
      pickupEntity.getDropId() +
      "/" +
      pickupEntity.getUserId()
    );
    try {
      PickupEntity createdPickup = pickupsRepository.insert(pickupEntity);
      fileLogger.info("Pickup created with ID: " + createdPickup.getId());
      return Optional.of(createdPickup);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Pickup already exists with specified dropId/userId: " +
        pickupDto.getDropId() +
        "/" +
        pickupDto.getUserId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating pickup: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<PickupEntity> getPickups(String messageId, String userId) {
    try {
      if (messageId == null) {
        fileLogger.info("Fetching all pickups.");
        return pickupsRepository.findAll();
      }
      if (userId == null) {
        fileLogger.info("Fetching pickups with dropId: " + messageId);
        return pickupsRepository.findByDropId(messageId);
      }
      fileLogger.info(
        "Fetching pickups with dropId/userId: " + messageId + "/" + userId
      );
      return pickupsRepository.findByDropIdAndUserId(messageId, userId);
    } catch (Exception e) {
      fileLogger.error("Error fetching pickups: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<PickupEntity> getPickupById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching pickup with ID: " + id);
        return pickupsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching pickup: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<PickupEntity> getPickupByDropIdAndUserId(
    String dropId,
    String userId
  ) {
    if (dropId != null && userId != null) {
      try {
        fileLogger.info(
          "Fetching pickup with dropId/userId: " + dropId + "/" + userId
        );
        List<PickupEntity> existingPickups =
          pickupsRepository.findByDropIdAndUserId(dropId, userId);
        if (existingPickups.size() > 1) {
          fileLogger.error(
            "Multiple pickups found with the same dropId/userId: " +
            dropId +
            "/" +
            userId
          );
        } else if (!existingPickups.isEmpty()) {
          return Optional.of(existingPickups.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching pickup by dropId and userId: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<PickupEntity> updatePickup(String id, PickupDto pickupDto) {
    if (id != null) {
      try {
        Optional<PickupEntity> pickupsOptional = pickupsRepository.findById(id);
        if (pickupsOptional.isPresent()) {
          PickupEntity pickups = pickupsOptional.get();
          pickups.setDropId(pickupDto.getDropId());
          pickups.setUserId(pickupDto.getUserId());
          pickups.setTimestamp(pickupDto.getTimestamp());

          PickupEntity updatedPickup = pickupsRepository.save(pickups);
          fileLogger.info("Pickup updated with ID: " + id);
          return Optional.of(updatedPickup);
        } else {
          fileLogger.warn("Pickup not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating pickup: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<PickupEntity> deletePickup(String id) {
    if (id != null) {
      try {
        Optional<PickupEntity> pickupsOptional = pickupsRepository.findById(id);
        if (pickupsOptional.isPresent()) {
          PickupEntity pickup = pickupsOptional.get();
          pickupsRepository.deleteById(id);
          fileLogger.info("Pickup deleted with ID: " + id);
          return Optional.of(pickup);
        } else {
          fileLogger.warn("Pickup not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting pickup: " + e.getMessage());
      }
    }
    return Optional.empty();
  }
}
