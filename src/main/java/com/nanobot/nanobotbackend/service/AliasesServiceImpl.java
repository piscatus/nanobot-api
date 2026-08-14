package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import com.nanobot.nanobotbackend.repository.AliasesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class AliasesServiceImpl implements AliasesService {

  private AliasesRepository aliasesRepository;

  private final FileLogger fileLogger;

  private static final String FETCH_BY_GUILD = "Fetching aliases by guildId: ";

  public AliasesServiceImpl(AliasesRepository aliasesRepository) {
    this.fileLogger = new FileLogger("AliasesService");
    this.aliasesRepository = aliasesRepository;
  }

  public AliasDto analyzeAliases(String guildId, String inputString) {
    if (!StringUtil.isValidString(inputString)) {
      return null;
    }
    List<AliasEntity> aliasesDB = getAliasesByEitherGuildId(guildId, "GLOBAL");
    for (AliasEntity entity : aliasesDB) {
      if (
        entity.getSingular().equalsIgnoreCase(inputString) ||
        entity.getPlural().equalsIgnoreCase(inputString) ||
        entity.getEmoji().equals(inputString)
      ) {
        return new AliasDto(entity);
      }
    }
    return null;
  }

  @Override
  public Optional<AliasEntity> createAlias(AliasDto aliasDto) {
    AliasEntity aliasEntity = new AliasEntity(aliasDto);
    ObjectId id = new ObjectId();
    aliasEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating alias with guildId/singular: " +
      aliasEntity.getGuildId() +
      "/" +
      aliasEntity.getSingular()
    );
    try {
      AliasEntity createdAlias = aliasesRepository.insert(aliasEntity);
      fileLogger.info("Alias created with ID: " + createdAlias.getId());
      return Optional.of(createdAlias);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Alias already exists with specified guildId/singular: " +
        aliasDto.getGuildId() +
        "/" +
        aliasDto.getSingular()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating alias: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<AliasEntity> getAliases(String guildId, String singular) {
    try {
      if (guildId == null && singular == null) {
        fileLogger.info("Fetching all aliases.");
        return aliasesRepository.findAll();
      } else if (singular == null) {
        fileLogger.info(FETCH_BY_GUILD + guildId);
        return aliasesRepository.findByGuildId(guildId);
      }
      fileLogger.info(
        "Fetching aliases by guildId/singular: " + guildId + "/" + singular
      );
      return aliasesRepository.findByGuildIdAndSingular(guildId, singular);
    } catch (Exception e) {
      fileLogger.error("Error fetching aliases: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<AliasEntity> getAliasesByEitherGuildId(
    String guildId1,
    String guildId2
  ) {
    try {
      if (guildId1 == null && guildId2 == null) {
        fileLogger.info("Fetching all aliases.");
        return aliasesRepository.findAll();
      } else if (guildId1 == null) {
        fileLogger.info(FETCH_BY_GUILD + guildId2);
        return aliasesRepository.findByGuildId(guildId2);
      } else if (guildId2 == null) {
        fileLogger.info(FETCH_BY_GUILD + guildId1);
        return aliasesRepository.findByGuildId(guildId1);
      }
      fileLogger.info(
        "Fetching aliases by either guildId: " + guildId1 + "/" + guildId2
      );
      return aliasesRepository.findByEitherGuildId(guildId1, guildId2);
    } catch (Exception e) {
      fileLogger.error("Error fetching aliases: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<AliasEntity> getAliasById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching alias with ID: " + id);
        return aliasesRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching aliases by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AliasEntity> getAliasByGuildIdAndSingular(
    String guildId,
    String singular
  ) {
    if (guildId != null && singular != null) {
      try {
        fileLogger.info(
          "Fetching alias with guildId/singular: " + guildId + "/" + singular
        );
        List<AliasEntity> existingAliases =
          aliasesRepository.findByGuildIdAndSingular(guildId, singular);
        if (existingAliases.size() > 1) {
          fileLogger.error(
            "Multiple aliases found with the same guildId/singular: " +
            guildId +
            "/" +
            singular
          );
        } else if (!existingAliases.isEmpty()) {
          return Optional.of(existingAliases.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching alias by guildId and singular: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AliasEntity> updateAlias(String id, AliasDto aliasDto) {
    if (id != null) {
      try {
        Optional<AliasEntity> aliasesOptional = aliasesRepository.findById(id);
        if (aliasesOptional.isPresent()) {
          AliasEntity aliases = aliasesOptional.get();
          aliases.setGuildId(aliasDto.getGuildId());
          aliases.setOwnerId(aliasDto.getOwnerId());
          aliases.setSingular(aliasDto.getSingular());
          aliases.setPlural(aliasDto.getPlural());
          aliases.setTicker(aliasDto.getTicker());
          aliases.setValue(aliasDto.getValue());
          aliases.setInput(aliasDto.getInput());
          aliases.setEmoji(aliasDto.getEmoji());

          AliasEntity updatedAlias = aliasesRepository.save(aliases);
          fileLogger.info("Alias updated with ID: " + id);
          return Optional.of(updatedAlias);
        } else {
          fileLogger.warn("Alias not found with ID: " + id);
          return Optional.empty();
        }
      } catch (Exception e) {
        fileLogger.error("Error updating alias: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<AliasEntity> deleteAlias(String id) {
    if (id != null) {
      try {
        Optional<AliasEntity> aliasesOptional = aliasesRepository.findById(id);
        if (aliasesOptional.isPresent()) {
          AliasEntity aliases = aliasesOptional.get();
          aliasesRepository.deleteById(id);
          fileLogger.info("Alias deleted with ID: " + id);
          return Optional.of(aliases);
        } else {
          fileLogger.warn("Alias not found with ID: " + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error deleting alias: " + id);
      }
    }
    return Optional.empty();
  }

  @Override
  public void setAliases(Consumer<List<AliasDto>> setAliases, String guildId) {
    List<AliasEntity> aliasEntities = getAliases(guildId, null);
    List<AliasDto> aliasDtos = new ArrayList<>();
    for (AliasEntity entity : aliasEntities) {
      aliasDtos.add(new AliasDto(entity));
    }
    setAliases.accept(aliasDtos);
  }
}
