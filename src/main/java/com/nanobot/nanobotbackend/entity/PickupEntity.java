package com.nanobot.nanobotbackend.entity;

import com.nanobot.nanobotbackend.dto.PickupDto;
import java.util.Date;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

@Document(collection = "pickups")
public class PickupEntity extends BaseEntity {

  @Indexed
  private String dropId;

  @Indexed
  private String userId;

  private Date timestamp;

  public PickupEntity() {
    super();
  }

  public PickupEntity(PickupDto pickup) {
    super(pickup.getId());
    this.dropId = pickup.getDropId();
    this.userId = pickup.getUserId();
    this.timestamp = new Date();
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
