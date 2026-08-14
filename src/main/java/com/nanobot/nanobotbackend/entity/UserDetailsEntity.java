package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "userDetails")
public class UserDetailsEntity extends BaseEntity {

  private StatusDto status;

  private String seed;

  @Indexed(unique = true)
  private String userId;

  private String subordinateUserId;

  public UserDetailsEntity() {
    super();
  }

  public UserDetailsEntity(UserDetailsDto user) {
    super(user.getId());
    this.status = user.getStatus();
    this.seed = user.getSeed();
    this.userId = user.getUserId();
    this.subordinateUserId = user.getSubordinateUserId();
  }

  public StatusDto getStatus() {
    return status;
  }

  public void setStatus(StatusDto status) {
    this.status = status;
  }

  public String getSeed() {
    return seed;
  }

  public void setSeed(String seed) {
    this.seed = seed;
  }

  public String getUserId() {
    return userId;
  }

  public void setUserId(String userId) {
    this.userId = userId;
  }

  public String getSubordinateUserId() {
    return subordinateUserId;
  }

  public void setSubordinateUserId(String subordinateUserId) {
    this.subordinateUserId = subordinateUserId;
  }
}
