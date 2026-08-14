package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.PickupEntity;
import java.util.Date;

public class PickupDto extends BaseDto {

  private String dropId;

  private String userId;

  private Date timestamp;

  public PickupDto() {
    super();
  }

  public PickupDto(String id, String dropId, String userId, Date timestamp) {
    super(id);
    this.dropId = dropId;
    this.userId = userId;
    this.timestamp = timestamp;
  }

  public PickupDto(PickupEntity entity) {
    super(entity.getId());
    this.dropId = entity.getDropId();
    this.userId = entity.getUserId();
    this.timestamp = entity.getTimestamp();
  }

  public String getDropId() {
    return dropId;
  }

  public void setDropId(String dropId) {
    this.dropId = dropId;
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
