package com.nanobot.nanobotbackend.entity;

import com.fasterxml.jackson.annotation.JsonInclude;
import tools.jackson.core.JacksonException;
import tools.jackson.databind.json.JsonMapper;
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
