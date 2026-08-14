package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class UserItemsResponseDto extends BaseResponseDto {

  private List<ItemDto> bonuses;

  private List<ItemDto> userItems;

  private List<ItemDto> subordinateItems;

  private List<CurrencyDto> currencies;

  private List<CreatureDto> creatures;

  public UserItemsResponseDto() {
    super();
  }

  public UserItemsResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
  }

  public List<ItemDto> getUserItems() {
    return userItems;
  }

  public void setUserItems(List<ItemDto> userItems) {
    this.userItems = userItems;
  }

  public List<ItemDto> getSubordinateItems() {
    return subordinateItems;
  }

  public void setSubordinateItems(List<ItemDto> subordinateItems) {
    this.subordinateItems = subordinateItems;
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<CreatureDto> getCreatures() {
    return creatures;
  }

  public void setCreatures(List<CreatureDto> creatures) {
    this.creatures = creatures;
  }
}
