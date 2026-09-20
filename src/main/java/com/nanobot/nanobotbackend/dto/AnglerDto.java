package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.AnglerEntity;
import java.util.Date;

public class AnglerDto extends BaseDto {

  private String guildId;

  private String userId;

  private boolean resting;

  private Date timestamp;

  private String ticker;

  public AnglerDto() {
    super();
  }

  public AnglerDto(AnglerEntity entity) {
    super(entity.getId());
    this.guildId = entity.getGuildId();
    this.userId = entity.getUserId();
    this.resting = entity.getResting();
    this.timestamp = entity.getTimestamp();
    this.ticker = entity.getTicker();
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public boolean getResting() {
    return resting;
  }

  public void setResting(boolean resting) {
    this.resting = resting;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public String getTicker() {
    return ticker;
  }

  public void setTicker(String ticker) {
    this.ticker = ticker;
  }
}
