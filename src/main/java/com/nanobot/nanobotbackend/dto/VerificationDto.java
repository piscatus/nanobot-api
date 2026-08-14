package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.VerificationEntity;
import java.util.Date;

public class VerificationDto extends BaseDto {

  private String userId;

  private Date timestamp;

  public VerificationDto() {
    super();
  }

  public VerificationDto(VerificationEntity entity) {
    super(entity.getId());
    this.userId = entity.getUserId();
    this.timestamp = entity.getTimestamp();
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
