package com.nanobot.nanobotbackend.dto;

import java.util.ArrayList;
import java.util.List;

public class BonusesDto extends BaseDto {

  private List<ItemDto> bonuses = new ArrayList<>();

  public BonusesDto() {
    bonuses.addAll(
      List.of(
        new ItemDto("1.25", 10),
        new ItemDto("1.50", 20),
        new ItemDto("1.75", 50),
        new ItemDto("2.00", 100)
      )
    );
  }

  public List<ItemDto> getBonuses() {
    return bonuses;
  }

  public void setBonuses(List<ItemDto> bonuses) {
    this.bonuses = bonuses;
  }
}
