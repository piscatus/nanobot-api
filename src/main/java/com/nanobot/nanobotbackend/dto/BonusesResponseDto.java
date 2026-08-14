package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class BonusesResponseDto extends BaseResponseDto {

  private List<ItemDto> bonuses;

  public BonusesResponseDto() {
    super();
  }

  public BonusesResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
  }
}
