package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.ActivityDto;
import java.util.Date;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "activities")
public class ActivityEntity extends BaseEntity {

  @Indexed
  private String guildId;

  @Indexed
  private String channelId;

  @Indexed
  private String userId;

  @Indexed
  private Date timestamp;

  public ActivityEntity() {
    super();
  }

  public ActivityEntity(ActivityDto activities) {
    super(activities.getId());
    this.guildId = activities.getGuildId();
    this.channelId = activities.getChannelId();
    this.userId = activities.getUserId();
    this.timestamp = new Date();
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
