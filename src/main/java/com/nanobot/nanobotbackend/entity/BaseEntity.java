package com.nanobot.nanobotbackend.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.BaseDto;
import org.springframework.data.annotation.Id;

public class BaseEntity {

  @Id
  private String id;

  public BaseEntity() {
    this.id = null;
  }

  public BaseEntity(BaseDto base) {
    this.id = base.getId();
  }

  public BaseEntity(String id) {
    this.id = id;
  }

  public String getId() {
    return id;
  }

  public void setId(String id) {
    this.id = id;
  }

  public String toJson(boolean includeNulls) {
    ObjectMapper mapper = new ObjectMapper();

    if (!includeNulls) {
      mapper.setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    try {
      return mapper.writeValueAsString(this);
    } catch (JsonProcessingException e) {
      e.printStackTrace();
      return null;
    }
  }
}
