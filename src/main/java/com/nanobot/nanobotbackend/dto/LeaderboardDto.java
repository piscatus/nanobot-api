package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import java.util.List;

public class LeaderboardDto extends BaseDto {

  private String guildId;

  private String userId;

  private List<ItemDto> items;

  public LeaderboardDto() {
    super();
  }

  public LeaderboardDto(LeaderboardEntity entity) {
    super(entity.getId());
    this.guildId = entity.getGuildId();
    this.userId = entity.getUserId();
    this.items = entity.getItems();
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public List<ItemDto> getItems() {
    return items;
  }

  public void setItems(List<ItemDto> items) {
    this.items = items;
  }
}
