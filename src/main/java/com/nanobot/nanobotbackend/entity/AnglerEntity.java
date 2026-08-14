package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import java.util.Date;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "anglers")
public class AnglerEntity extends BaseEntity {

  @Indexed
  private String guildId;

  @Indexed
  private String userId;

  private boolean resting;

  private Date timestamp;

  public AnglerEntity() {
    super();
  }

  public AnglerEntity(
    String guildId,
    String userId,
    boolean resting,
    Date timestamp
  ) {
    super();
    this.guildId = guildId;
    this.userId = userId;
    this.resting = resting;
    this.timestamp = timestamp;
  }

  public AnglerEntity(AnglerDto anglers) {
    super(anglers.getId());
    this.guildId = anglers.getGuildId();
    this.userId = anglers.getUserId();
    this.resting = anglers.getResting();
    this.timestamp = new Date();
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
}
