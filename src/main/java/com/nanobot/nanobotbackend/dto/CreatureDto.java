package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.CreatureEntity;

public class CreatureDto extends BaseDto {

  private String name;

  private String pluralization;

  private int capacity;

  private int odds;

  private String value;

  private String ticker;

  private String emoji;

  private String image;

  public CreatureDto() {
    super();
  }

  public CreatureDto(CreatureEntity entity) {
    super(entity.getId());
    this.name = entity.getName();
    this.pluralization = entity.getPluralization();
    this.capacity = entity.getCapacity();
    this.odds = entity.getOdds();
    this.value = entity.getValue();
    this.ticker = entity.getTicker();
    this.emoji = entity.getEmoji();
    this.image = entity.getImage();
  }

  public CreatureDto(String name, String pluralization, String emoji) {
    super();
    this.name = name;
    this.pluralization = pluralization;
    this.emoji = emoji;
  }

  public CreatureDto(
    String name,
    String pluralization,
    String emoji,
    String ticker
  ) {
    super();
    this.name = name;
    this.pluralization = pluralization;
    this.emoji = emoji;
    this.ticker = ticker;
  }

  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public String getPluralization() {
    return pluralization;
  }

  public void setPluralization(String pluralization) {
    this.pluralization = pluralization;
  }

  public int getCapacity() {
    return capacity;
  }

  public void setCapacity(int capacity) {
    this.capacity = capacity;
  }

  public int getOdds() {
    return odds;
  }

  public void setOdds(int odds) {
    this.odds = odds;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }

  public String getImage() {
    return image;
  }

  public void setImage(String image) {
    this.image = image;
  }
}
