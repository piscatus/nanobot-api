package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.EmojiEntity;

public class EmojiDto extends BaseDto {

  private String category;

  private String name;

  private String emoji;

  public EmojiDto() {
    super();
  }

  public EmojiDto(EmojiEntity entity) {
    super(entity.getId());
    this.category = entity.getCategory();
    this.name = entity.getName();
    this.emoji = entity.getEmoji();
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
