package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.ProfanityDto;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import com.nanobot.nanobotbackend.repository.ProfanitiesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ProfanitiesServiceImpl implements ProfanitiesService {

  private ProfanitiesRepository profanitiesRepository;

  private final FileLogger fileLogger;

  public ProfanitiesServiceImpl(ProfanitiesRepository profanitiesRepository) {
    this.fileLogger = new FileLogger("ProfanitiesService");
    this.profanitiesRepository = profanitiesRepository;
  }

  @Override
  public Optional<ProfanityEntity> createProfanity(ProfanityDto profanityDto) {
    ProfanityEntity profanityEntity = new ProfanityEntity(profanityDto);
    ObjectId id = new ObjectId();
    profanityEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating profanity with singular/plural: " +
      profanityEntity.getSingular() +
      "/" +
      profanityEntity.getPlural()
    );
    try {
      ProfanityEntity createdProfanity = profanitiesRepository.insert(
        profanityEntity
      );
      fileLogger.info("Profanity created with ID: " + createdProfanity.getId());
      return Optional.of(createdProfanity);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Profanity already exists with specified singular/plural: " +
        profanityDto.getSingular() +
        "/" +
        profanityDto.getPlural()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating profanity: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<ProfanityEntity> getProfanities(String singular) {
    try {
      if (singular == null) {
        fileLogger.info("Fetching all profanities.");
        return profanitiesRepository.findAll();
      }
      fileLogger.info("Fetching profanities by singular: " + singular);
      return profanitiesRepository.findBySingular(singular);
    } catch (Exception e) {
      fileLogger.error("Error fetching profanities: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<ProfanityEntity> getProfanityById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching profanity with ID: " + id);
        return profanitiesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching profanity by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<ProfanityEntity> getProfanityBySingular(String singular) {
    if (singular != null) {
      try {
        fileLogger.info("Fetching profanity with singular: " + singular);
        List<ProfanityEntity> existingProfanities =
          profanitiesRepository.findBySingular(singular);
        if (existingProfanities.size() > 1) {
          fileLogger.error(
            "Multiple profanities found with the same singular: " + singular
          );
        } else if (!existingProfanities.isEmpty()) {
          return Optional.of(existingProfanities.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching profanity by singular: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<ProfanityEntity> updateProfanity(
    String id,
    ProfanityDto profanityDto
  ) {
    if (id != null) {
      try {
        Optional<ProfanityEntity> profanityOptional =
          profanitiesRepository.findById(id);
        if (profanityOptional.isPresent()) {
          ProfanityEntity profanity = profanityOptional.get();
          profanity.setSingular(profanityDto.getSingular());
          profanity.setPlural(profanityDto.getPlural());
          profanity.setContains(profanityDto.getContains());

          ProfanityEntity updatedProfanity = profanitiesRepository.save(
            profanity
          );
          fileLogger.info("Profanity updated with ID: " + id);
          return Optional.of(updatedProfanity);
        } else {
          fileLogger.warn("Profanity not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating profanity: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<ProfanityEntity> deleteProfanity(String id) {
    if (id != null) {
      try {
        Optional<ProfanityEntity> profanityOptional =
          profanitiesRepository.findById(id);
        if (profanityOptional.isPresent()) {
          ProfanityEntity profanity = profanityOptional.get();
          profanitiesRepository.deleteById(id);
          fileLogger.info("Profanity deleted with ID: " + id);
          return Optional.of(profanity);
        } else {
          fileLogger.warn("Profanity not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting profanity: " + id);
      }
    }
    return Optional.empty();
  }
}
