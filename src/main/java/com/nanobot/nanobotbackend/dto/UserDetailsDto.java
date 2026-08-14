package com.nanobot.nanobotbackend.dto;

import com.nanobot.nanobotbackend.entity.UserDetailsEntity;

public class UserDetailsDto extends BaseDto {

  private StatusDto status;

  private String seed;

  private String userId;

  private String subordinateUserId;

  public UserDetailsDto() {
    super();
  }

  public UserDetailsDto(
    String userId,
    String subordinateUserId,
    StatusDto status,
    String seed
  ) {
    super();
    this.userId = userId;
    this.subordinateUserId = subordinateUserId;
    this.status = status;
    this.seed = seed;
  }

  public UserDetailsDto(UserDetailsEntity entity) {
    super(entity.getId());
    this.status = entity.getStatus();
    this.seed = entity.getSeed();
    this.userId = entity.getUserId();
    this.subordinateUserId = entity.getSubordinateUserId();
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

  public void removeSensitiveData() {
    this.seed = null;
  }
}
