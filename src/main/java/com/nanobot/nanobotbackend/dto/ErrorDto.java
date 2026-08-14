package com.nanobot.nanobotbackend.dto;

import java.util.Date;

public class ErrorDto extends BaseDto {

  private Date timestamp;

  private Integer status;

  private String message;

  private String trace;

  public ErrorDto(
    Date timestamp,
    Integer status,
    String message,
    String trace
  ) {
    super();
    this.timestamp = timestamp;
    this.status = status;
    this.message = message;
    this.trace = trace;
  }

  public Date getTimestamp() {
    return timestamp;
  }

  public void setTimestamp(Date timestamp) {
    this.timestamp = timestamp;
  }

  public Integer getStatus() {
    return status;
  }

  public void setStatus(Integer status) {
    this.status = status;
  }

  public String getMessage() {
    return message;
  }

  public void setMessage(String message) {
    this.message = message;
  }

  public String getTrace() {
    return trace;
  }

  public void setTrace(String trace) {
    this.trace = trace;
  }
}
