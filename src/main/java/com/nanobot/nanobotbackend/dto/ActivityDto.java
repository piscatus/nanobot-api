package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.ActivityEntity;
import java.util.Date;

public class ActivityDto extends BaseDto {

  private String guildId;

  private String channelId;

  private String userId;

  private Date timestamp;

  public ActivityDto() {
    super();
  }

  public ActivityDto(ActivityEntity activity) {
    super(activity.getId());
    this.guildId = activity.getGuildId();
    this.channelId = activity.getChannelId();
    this.userId = activity.getUserId();
    this.timestamp = activity.getTimestamp();
  }

  public ActivityDto(
    String guildId,
    String channelId,
    String userId,
    Date timestamp
  ) {
    super();
    this.guildId = guildId;
    this.channelId = channelId;
    this.userId = userId;
    this.timestamp = timestamp;
  }

  public String getGuildId() {
    return guildId;
  }

  public void setGuildId(String guildId) {
    this.guildId = guildId;
  }

  public String getChannelId() {
    return channelId;
  }

  public void setChannelId(String channelId) {
    this.channelId = channelId;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }
}
