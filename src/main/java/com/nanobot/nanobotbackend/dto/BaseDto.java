package com.nanobot.nanobotbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import java.util.List;
import org.springframework.data.annotation.Id;

public class BaseDto {

  @Id
  private String id;

  public BaseDto() {
    this.id = null;
  }

  public BaseDto(String id) {
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
