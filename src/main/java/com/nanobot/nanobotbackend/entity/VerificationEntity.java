package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.VerificationDto;
import java.util.Date;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "verifications")
public class VerificationEntity extends BaseEntity {

  @Indexed(unique = true)
  private String userId;

  private Date timestamp;

  public VerificationEntity() {
    super();
  }

  public VerificationEntity(String userId, Date timestamp) {
    super();
    this.userId = userId;
    this.timestamp = timestamp;
  }

  public VerificationEntity(VerificationDto dto) {
    super(dto.getId());
    this.userId = dto.getUserId();
    this.timestamp = new Date();
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
