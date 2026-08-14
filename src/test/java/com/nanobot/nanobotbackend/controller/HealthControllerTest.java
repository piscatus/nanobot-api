package com.nanobot.nanobotbackend.controller;

import static org.hamcrest.Matchers.containsString;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

public class HealthControllerTest {

  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    this.mockMvc = MockMvcBuilders.standaloneSetup(new HealthController()).build();
  }

  @Test
  void healthCheckReturnsOkWithHealthMessage() throws Exception {
    mockMvc
      .perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Nanobot Backend is Healthy")));
  }

  @Test
  void healthCheckResponseContainsTimestamp() throws Exception {
    mockMvc
      .perform(get("/health"))
      .andExpect(status().isOk())
      .andExpect(content().string(containsString("Nanobot Backend is Healthy - ")));
  }

  @Test
  void healthCheckResponseContainsValidIsoTimestamp() throws Exception {
    String content = mockMvc
      .perform(get("/health"))
      .andExpect(status().isOk())
      .andReturn()
      .getResponse()
      .getContentAsString();

    // Timestamp format: "Nanobot Backend is Healthy - 2026-03-15T20:48:58.123456789Z"
    String timestampPart = content.replace("Nanobot Backend is Healthy - ", "").trim();
    assertTrue(
      timestampPart.matches("\\d{4}-\\d{2}-\\d{2}T\\d{2}:\\d{2}:\\d{2}(\\.\\d+)?Z?"),
      "Expected ISO-8601 timestamp format, got: " + timestampPart
    );
  }
}
