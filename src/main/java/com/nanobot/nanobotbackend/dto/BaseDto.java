package com.nanobot.nanobotbackend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
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
    JsonMapper.Builder mapperBuilder = JsonMapper.builder();

    if (!includeNulls) {
      mapperBuilder.changeDefaultPropertyInclusion(
        incl -> incl.withValueInclusion(JsonInclude.Include.NON_NULL)
      );
    }

    try {
      return mapperBuilder.build().writeValueAsString(this);
    } catch (JacksonException e) {
      e.printStackTrace();
      return null;
    }
  }
}
