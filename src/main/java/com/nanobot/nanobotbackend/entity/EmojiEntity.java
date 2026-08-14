package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.EmojiDto;
import org.springframework.data.mongodb.core.index.CompoundIndex;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "emojis")
public class EmojiEntity extends BaseEntity {

  @Indexed
  private String category;

  @Indexed
  private String name;

  private String emoji;

  public EmojiEntity() {
    super();
  }

  public EmojiEntity(String category, String name, String emoji) {
    super();
    this.category = category;
    this.name = name;
    this.emoji = emoji;
  }

  public EmojiEntity(EmojiDto dto) {
    super(dto.getId());
    this.category = dto.getCategory();
    this.name = dto.getName();
    this.emoji = dto.getEmoji();
  }

  public String getCategory() {
    return category;
  }

  public void setCategory(String category) {
    this.category = category;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }
}
