package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.AliasEntity;

public class AliasDto extends BaseDto {

  private String guildId;

  private String ownerId;

  private String singular;

  private String plural;

  private String ticker;

  private String value;

  private String input;

  private String emoji;

  public AliasDto() {
    super();
  }

  public AliasDto(AliasEntity alias) {
    super(alias.getId());
    this.guildId = alias.getGuildId();
    this.ownerId = alias.getOwnerId();
    this.singular = alias.getSingular();
    this.plural = alias.getPlural();
    this.ticker = alias.getTicker();
    this.value = alias.getValue();
    this.input = alias.getInput();
    this.emoji = alias.getEmoji();
  }

  public AliasDto(
    String guildId,
    String ownerId,
    String singular,
    String plural,
    String ticker,
    String value,
    String emoji
  ) {
    super();
    this.guildId = guildId;
    this.ownerId = ownerId;
    this.singular = singular;
    this.plural = plural;
    this.ticker = ticker;
    this.value = value;
    this.emoji = emoji;
  }

  public AliasDto(
    String guildId,
    String singular,
    String plural,
    String ticker,
    String value,
    String emoji
  ) {
    super();
    this.guildId = guildId;
    this.singular = singular;
    this.plural = plural;
    this.ticker = ticker;
    this.value = value;
    this.emoji = emoji;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getOwnerId() {
    return ownerId;
  }

  public void setOwnerId(String ownerId) {
    this.ownerId = ownerId;
  }

  public String getSingular() {
    return singular;
  }

  public void setSingular(String singular) {
    this.singular = singular;
  }

  public String getPlural() {
    return plural;
  }

  public void setPlural(String plural) {
    this.plural = plural;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }

  public String getValue() {
    return value;
  }

  public void setValue(String value) {
    this.value = value;
  }

  public String getInput() {
    return input;
  }

  public void setInput(String input) {
    this.input = input;
  }

  public String getEmoji() {
    return emoji;
  }

  public void setEmoji(String emoji) {
    this.emoji = emoji;
  }
}
