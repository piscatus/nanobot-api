package com.nanobot.nanobotbackend.dto;

import java.util.List;

public class LeaderboardsResponseDto extends BaseResponseDto {

  private List<CurrencyDto> currencies;

  private List<LeaderboardDto> leaderboards;

  private List<CreatureDto> creatures;

  public LeaderboardsResponseDto() {
    super();
  }

  public LeaderboardsResponseDto(String errorMessage) {
    super(errorMessage);
  }

  public List<CurrencyDto> getCurrencies() {
    return currencies;
  }

  public void setCurrencies(List<CurrencyDto> currencies) {
    this.currencies = currencies;
  }

  public List<LeaderboardDto> getLeaderboards() {
    return leaderboards;
  }

  public void setLeaderboards(List<LeaderboardDto> leaderboards) {
    this.leaderboards = leaderboards;
  }

  public List<CreatureDto> getCreatures() {
    return creatures;
  }

  public void setCreatures(List<CreatureDto> creatures) {
    this.creatures = creatures;
  }
}
