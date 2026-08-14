package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class CreaturesResponseDto extends BaseResponseDto {

  private List<ItemDto> bonuses;

  private List<CurrencyDto> currencies;

  private List<CreatureDto> creatures;

  public CreaturesResponseDto() {
    super();
  }

  public CreaturesResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
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
