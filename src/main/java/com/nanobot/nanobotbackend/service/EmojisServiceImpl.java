package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.repository.EmojisRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.List;
import java.util.Optional;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class EmojisServiceImpl implements EmojisService {

  private EmojisRepository emojisRepository;

  private final FileLogger fileLogger;

  private static final String UPDATE_BY_ID = "Emoji updated with ID: ";

  private static final String NOT_FOUND_BY_ID = "Emoji not found with ID: ";

  public EmojisServiceImpl(EmojisRepository emojisRepository) {
    this.fileLogger = new FileLogger("EmojisService");
    this.emojisRepository = emojisRepository;
  }

  @Override
  public Optional<EmojiEntity> createEmoji(EmojiDto emojiDto) {
    EmojiEntity emojiEntity = new EmojiEntity(emojiDto);
    ObjectId id = new ObjectId();
    emojiEntity.setId(id.toHexString());
    fileLogger.info(
      "Creating emoji with category/name: " +
      emojiEntity.getCategory() +
      "/" +
      emojiEntity.getName()
    );
    try {
      EmojiEntity created = emojisRepository.insert(emojiEntity);
      fileLogger.info("Emoji created with ID: " + created.getId());
      return Optional.of(created);
    } catch (DuplicateKeyException e) {
      fileLogger.warn(
        "Emoji already exists with specified category/name: " +
        emojiDto.getCategory() +
        "/" +
        emojiDto.getName()
      );
      return Optional.empty();
    } catch (Exception e) {
      fileLogger.error("Error creating emoji: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public List<EmojiEntity> getEmojis(String category, String name) {
    try {
      if (category == null && name == null) {
        fileLogger.info("Fetching all emojis.");
        return emojisRepository.findAll();
      }
      if (category != null && name != null) {
        fileLogger.info(
          "Fetching emojis with category/name: " + category + "/" + name
        );
        return emojisRepository.findByCategoryAndName(category, name);
      }
      if (category != null) {
        fileLogger.info("Fetching emojis with category: " + category);
        return emojisRepository.findByCategory(category);
      }
      fileLogger.info("Fetching emojis with name: " + name);
      return emojisRepository.findByName(name);
    } catch (Exception e) {
      fileLogger.error("Error fetching emojis: " + e.getMessage());
      throw e;
    }
  }

  @Override
  public Optional<EmojiEntity> getEmojiById(String id) {
    if (id != null) {
      try {
        fileLogger.info("Fetching emoji with ID: " + id);
        return emojisRepository.findById(id);
      } catch (Exception e) {
        fileLogger.error("Error fetching emoji by ID: " + e.getMessage());
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<EmojiEntity> getEmojiByCategoryAndName(
    String category,
    String name
  ) {
    if (category != null && name != null) {
      try {
        fileLogger.info(
          "Fetching emoji with category/name: " + category + "/" + name
        );
        List<EmojiEntity> existing =
          emojisRepository.findByCategoryAndName(category, name);
        if (existing.size() > 1) {
          fileLogger.error(
            "Multiple emojis found with the same category/name: " +
            category +
            "/" +
            name
          );
        } else if (!existing.isEmpty()) {
          return Optional.of(existing.get(0));
        }
      } catch (Exception e) {
        fileLogger.error(
          "Error fetching emoji by category and name: " + e.getMessage()
        );
        throw e;
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<EmojiEntity> updateEmoji(String id, EmojiDto emojiDto) {
    if (id != null) {
      try {
        Optional<EmojiEntity> optional = emojisRepository.findById(id);
        if (optional.isPresent()) {
          EmojiEntity entity = optional.get();
          entity.setCategory(emojiDto.getCategory());
          entity.setName(emojiDto.getName());
          entity.setEmoji(emojiDto.getEmoji());

          EmojiEntity updated = emojisRepository.save(entity);
          fileLogger.info(UPDATE_BY_ID + id);
          return Optional.of(updated);
        } else {
          fileLogger.warn(NOT_FOUND_BY_ID + id);
        }
      } catch (Exception e) {
        fileLogger.error("Error updating emoji: " + e.getMessage());
      }
    }
    return Optional.empty();
  }

  @Override
  public Optional<EmojiEntity> deleteEmoji(String id) {
    if (id != null) {
      Optional<EmojiEntity> optional = emojisRepository.findById(id);

      if (optional.isPresent()) {
        EmojiEntity entity = optional.get();
        emojisRepository.deleteById(id);
        fileLogger.info("Emoji deleted with ID: " + id);
        return Optional.of(entity);
      } else {
        fileLogger.warn(NOT_FOUND_BY_ID + id);
      }
    }
    return Optional.empty();
  }
}
