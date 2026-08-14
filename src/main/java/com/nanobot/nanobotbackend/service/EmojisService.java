package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import java.util.List;
import java.util.Optional;

public interface EmojisService {
  Optional<EmojiEntity> createEmoji(EmojiDto emojiDto);

  List<EmojiEntity> getEmojis(String category, String name);

  Optional<EmojiEntity> getEmojiById(String id);

  Optional<EmojiEntity> getEmojiByCategoryAndName(
    String category,
    String name
  );

  Optional<EmojiEntity> updateEmoji(String id, EmojiDto emojiDto);

  Optional<EmojiEntity> deleteEmoji(String id);
}
