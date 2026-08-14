package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.repository.DropsRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class DropsServiceImpl implements DropsService {

  private DropsRepository dropsRepository;

  private final FileLogger fileLogger;

  public DropsServiceImpl(DropsRepository dropsRepository) {
    this.fileLogger = new FileLogger("DropsService");
    this.dropsRepository = dropsRepository;
  }

  @Override
  public void setDrops(Consumer<List<DropDto>> setDrops) {
    List<DropEntity> entities = getDrops(null);
    List<DropDto> drops = new ArrayList<>();
    for (DropEntity dropEntity : entities) {
      drops.add(new DropDto(dropEntity));
    }
    setDrops.accept(drops);
  }

  @Override
  public Optional<DropEntity> createDrop(DropDto dropDto) {
    DropEntity dropEntity = new DropEntity(dropDto);
    ObjectId id = new ObjectId();
    dropEntity.setId(id.toHexString());
    fileLogger.info("Creating drop with ID: " + dropEntity.getId());
    try {
      DropEntity createdDrop = dropsRepository.insert(dropEntity);
      fileLogger.info("Drop created with ID: " + createdDrop.getId());
      return Optional.of(createdDrop);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Drop already exists with specified messageId: " +
        dropDto.getMessageId()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating drop: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<DropEntity> getDrops(String messageId) {
    try {
      if (messageId == null) {
        // fileLogger.info("Fetching all drops.");
        return dropsRepository.findAll();
      }
      // fileLogger.info("Fetching drops with messageId: " + messageId);
      return dropsRepository.findByMessageId(messageId);
    } catch (Exception e) {
      fileLogger.error("Error fetching drops: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<DropEntity> getDropById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching drop with ID: " + id);
        return dropsRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching drop by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<DropEntity> getDropByMessageId(String messageId) {
    if (messageId != null) {
      try {
        fileLogger.info("Fetching drop with messageId: " + messageId);
        List<DropEntity> existingDrops = dropsRepository.findByMessageId(
          messageId
        );
        if (existingDrops.size() > 1) {
          fileLogger.error(
            "Multiple drops found with the same messageId: " + messageId
          );
        } else if (!existingDrops.isEmpty()) {
          return Optional.of(existingDrops.get(0));
        }
      } catch (Exception e) {
        fileLogger.error("Error fetching drop by messageId: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<DropEntity> updateDrop(String id, DropDto dropDto) {
    if (id != null) {
      try {
        Optional<DropEntity> dropsOptional = dropsRepository.findById(id);
        if (dropsOptional.isPresent()) {
          DropEntity drop = dropsOptional.get();
          drop.setMessageId(dropDto.getMessageId());
          drop.setGuildId(dropDto.getGuildId());
          drop.setChannelId(dropDto.getChannelId());
          drop.setUserId(dropDto.getUserId());
          drop.setStartTime(dropDto.getStartTime());
          drop.setEndTime(dropDto.getEndTime());
          drop.setInput(dropDto.getInput());
          drop.setTransfer(dropDto.getTransfer());
          drop.setRequiredRole(dropDto.getRequiredRole());
          drop.setMaximumEntries(dropDto.getMaximumEntries());
          drop.setNumberWinners(dropDto.getNumberWinners());

          DropEntity updatedDrop = dropsRepository.save(drop);
          fileLogger.info("Drop updated with ID: " + id);
          return Optional.of(updatedDrop);
        } else {
          fileLogger.warn("Drop not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating drop: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<DropEntity> updateDropMessageId(RequestDto requestDto) {
    if (
      requestDto.getId() != null &&
      !requestDto.getId().equals("") &&
      !requestDto.getId().equals("0") &&
      requestDto.getDropId() != null &&
      !requestDto.getDropId().equals("") &&
      !requestDto.getDropId().equals("0")
    ) {
      try {
        Optional<DropEntity> dropsOptional = dropsRepository.findById(
          requestDto.getId()
        );
        if (dropsOptional.isPresent()) {
          DropEntity drop = dropsOptional.get();
          drop.setMessageId(requestDto.getDropId());
          drop.setMessageData(requestDto.getMessageData());
          DropEntity updatedDrop = dropsRepository.save(drop);
          fileLogger.info("Drop updated with ID: " + requestDto.getId());
          return Optional.of(updatedDrop);
        } else {
          fileLogger.warn("Drop not found with ID: " + requestDto.getId());
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating drop: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<DropEntity> deleteDrop(String id) {
    if (id != null) {
      try {
        Optional<DropEntity> dropsOptional = dropsRepository.findById(id);
        if (dropsOptional.isPresent()) {
          DropEntity drops = dropsOptional.get();
          dropsRepository.deleteById(id);
          fileLogger.info("Drop deleted with ID: " + id);
          return Optional.of(drops);
        } else {
          fileLogger.warn("Drop not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting drop: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }
}
