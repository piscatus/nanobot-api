package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.CreatureDto;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "creatures")
public class CreatureEntity extends BaseEntity {

  @Indexed(unique = true)
  private String name;

  @Indexed(unique = true)
  private String pluralization;

  private int capacity;

  private int odds;

  private String value;

  private String ticker;

  private String emoji;

  private String image;

  public CreatureEntity() {
    super();
  }

  public CreatureEntity(CreatureDto creature) {
    super(creature.getId());
    this.name = creature.getName();
    this.pluralization = creature.getPluralization();
    this.capacity = creature.getCapacity();
    this.odds = creature.getOdds();
    this.value = creature.getValue();
    this.ticker = creature.getTicker();
    this.emoji = creature.getEmoji();
    this.image = creature.getImage();
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
