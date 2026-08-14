package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.ConfigResponseDto;
import com.nanobot.nanobotbackend.service.ConfigServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ConfigControllerTest {

  private static final String PATH = "/requests/config";

  private MockMvc mockMvc;

  @Mock
  private ConfigServices configServices;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new ConfigController(configServices)).build();
  }

  @Test
  void configShouldReturnAcceptedAndDelegateToService() throws Exception {
    ConfigResponseDto response = new ConfigResponseDto();
    when(configServices.config(any())).thenReturn(response);

    mockMvc
      .perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isAccepted());

    verify(configServices).config(any());
  }

  @Test
  void configShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(configServices, never()).config(any());
  }

  @Test
  void configShouldPassRequestBodyToService() throws Exception {
    ConfigResponseDto response = new ConfigResponseDto();
    when(configServices.config(any())).thenReturn(response);

    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"guild-123\"}")
      )
      .andExpect(status().isAccepted());

    verify(configServices).config(argThat(req -> "guild-123".equals(req.getGuildId())));
  }

  @Test
  void configShouldReturnErrorResponseBodyWhenServiceReturnsError() throws Exception {
    ConfigResponseDto errorResponse = new ConfigResponseDto("Configuration failed");
    when(configServices.config(any())).thenReturn(errorResponse);

    mockMvc
      .perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.errorMessage").value("Configuration failed"));

    verify(configServices).config(any());
  }
}
