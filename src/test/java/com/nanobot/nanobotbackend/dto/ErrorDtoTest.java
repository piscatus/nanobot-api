package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Date;
import org.junit.jupiter.api.Test;

class ErrorDtoTest {

  @Test
  void constructorSetsAllFields() {
    Date timestamp = new Date();
    ErrorDto dto = new ErrorDto(timestamp, 404, "Not Found", "trace-1");

    assertEquals(timestamp, dto.getTimestamp());
    assertEquals(404, dto.getStatus());
    assertEquals("Not Found", dto.getMessage());
    assertEquals("trace-1", dto.getTrace());
  }

  @Test
  void settersUpdateFields() {
    ErrorDto dto = new ErrorDto(null, 500, null, null);
    Date ts = new Date();

    dto.setTimestamp(ts);
    dto.setStatus(503);
    dto.setMessage("Service Unavailable");
    dto.setTrace("stack-trace");

    assertEquals(ts, dto.getTimestamp());
    assertEquals(503, dto.getStatus());
    assertEquals("Service Unavailable", dto.getMessage());
    assertEquals("stack-trace", dto.getTrace());
  }

  @Test
  void handlesNullValues() {
    ErrorDto dto = new ErrorDto(null, null, null, null);
    assertNotNull(dto);
  }
}
