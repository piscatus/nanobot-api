package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.repository.CreaturesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class CreaturesServiceImpl implements CreaturesService {

  private CreaturesRepository creaturesRepository;

  private final FileLogger fileLogger;

  public CreaturesServiceImpl(CreaturesRepository creaturesRepository) {
    this.fileLogger = new FileLogger("CreaturesService");
    this.creaturesRepository = creaturesRepository;
  }

  @Override
  public CreatureDto analyzeCreatures(String inputString) {
    if (!StringUtil.isValidString(inputString)) {
      return null;
    }
    List<CreatureEntity> creaturesDB = getCreatures();
    for (CreatureEntity creature : creaturesDB) {
      if (
        creature.getName().equalsIgnoreCase(inputString) ||
        creature.getPluralization().equalsIgnoreCase(inputString) ||
        creature.getEmoji().equals(inputString)
      ) {
        return new CreatureDto(creature);
      }
    }
    return null;
  }

  @Override
  public void setCreatures(Consumer<List<CreatureDto>> setCreatures) {
    List<CreatureEntity> creaturesEntities = getCreatures();
    List<CreatureDto> creatures = new ArrayList();
    for (CreatureEntity creatureEntity : creaturesEntities) {
      CreatureDto creature = new CreatureDto(creatureEntity);
      creatures.add(creature);
    }
    setCreatures.accept(creatures);
  }

  @Override
  public Optional<CreatureEntity> createCreature(CreatureDto creatureDto) {
    CreatureEntity creatureEntity = new CreatureEntity(creatureDto);
    ObjectId id = new ObjectId();
    creatureEntity.setId(id.toHexString());
    fileLogger.info("Creating creature with name: " + creatureEntity.getName());
    try {
      CreatureEntity createdCreature = creaturesRepository.insert(
        creatureEntity
      );
      fileLogger.info("Creature created with ID: " + createdCreature.getId());
      return Optional.of(createdCreature);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Creature already exists with specified name: " + creatureDto.getName()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating creature: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<CreatureEntity> getCreatures() {
    try {
      // fileLogger.info("Fetching all creatures.");
      return creaturesRepository.findAll();
    } catch (Exception e) {
      fileLogger.error("Error fetching creatures: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<CreatureEntity> getCreatureById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching creature with ID: " + id);
        return creaturesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching creature: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CreatureEntity> getCreatureByName(String name) {
    if (name != null) {
      try {
        fileLogger.info("Fetching creature with name: " + name);
        List<CreatureEntity> existingCreatures = creaturesRepository.findByName(
          name
        );
        if (existingCreatures.size() > 1) {
          fileLogger.error("Multiple creatures found with the same name");
        } else if (!existingCreatures.isEmpty()) {
          return Optional.of(existingCreatures.get(0));
        }
      } catch (Exception e) {
        fileLogger.error("Error fetching creature by name: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CreatureEntity> updateCreature(
    String id,
    CreatureDto creatureDto
  ) {
    if (id != null) {
      try {
        Optional<CreatureEntity> creatureOptional =
          creaturesRepository.findById(id);
        if (creatureOptional.isPresent()) {
          CreatureEntity creature = creatureOptional.get();
          creature.setName(creatureDto.getName());
          creature.setPluralization(creatureDto.getPluralization());
          creature.setCapacity(creatureDto.getCapacity());
          creature.setOdds(creatureDto.getOdds());
          creature.setValue(creatureDto.getValue());
          creature.setTicker(creatureDto.getTicker());
          creature.setEmoji(creatureDto.getEmoji());
          creature.setImage(creatureDto.getImage());

          CreatureEntity updatedCreature = creaturesRepository.save(creature);
          fileLogger.info("Creature updated with ID: " + id);
          return Optional.of(updatedCreature);
        } else {
          fileLogger.warn("Creature not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating creature: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<CreatureEntity> deleteCreature(String id) {
    if (id != null) {
      try {
        Optional<CreatureEntity> creatureOptional =
          creaturesRepository.findById(id);
        if (creatureOptional.isPresent()) {
          CreatureEntity creatureEntity = creatureOptional.get();
          creaturesRepository.deleteById(id);
          fileLogger.info("Creature deleted with ID: " + id);
          return Optional.of(creatureEntity);
        } else {
          fileLogger.warn("Creature not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting creature: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }
}
