package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.ServerResponseDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.service.ServerServices;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ServerControllerTest {

  private static final String BASE_PATH = "/requests";

  private MockMvc mockMvc;

  @Mock
  private ServerServices serverServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new ServerController(serverServices)).build();
  }

  @Test
  void serverShouldReturnOkAndDelegateToConfigurations() throws Exception {
    ServerResponseDto response = new ServerResponseDto();
    response.setCurrencies(List.of(new CurrencyDto("XNO", "Nano", true, ":xno:", "30")));
    when(serverServices.configurations(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(BASE_PATH + "/server")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.currencies").isArray())
      .andExpect(jsonPath("$.currencies[0].ticker").value("XNO"));

    verify(serverServices).configurations(any());
  }

  @Test
  void reservesShouldReturnOkAndDelegateToService() throws Exception {
    ServerResponseDto response = new ServerResponseDto();
    response.setGuildWallets(List.of(new WalletDto("XNO", "1000000000000000000000000000000")));
    when(serverServices.reserves(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(BASE_PATH + "/reserves")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.guildWallets").isArray())
      .andExpect(jsonPath("$.guildWallets[0].ticker").value("XNO"));

    verify(serverServices).reserves(any());
  }

  @Test
  void serverShouldReturnErrorWhenServiceReturnsError() throws Exception {
    ServerResponseDto response = new ServerResponseDto("Guild not configured");
    when(serverServices.configurations(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(BASE_PATH + "/server")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Guild not configured"));

    verify(serverServices).configurations(any());
  }

  @Test
  void reservesShouldReturnErrorWhenServiceReturnsError() throws Exception {
    ServerResponseDto response = new ServerResponseDto("Insufficient reserves");
    when(serverServices.reserves(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(BASE_PATH + "/reserves")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Insufficient reserves"));

    verify(serverServices).reserves(any());
  }

  private RequestDto minimalRequest() {
    return new RequestDto(
      null,
      null,
      false,
      null,
      null,
      false,
      "guild-1",
      null,
      null,
      null,
      null,
      null,
      "user-1",
      null,
      null,
      null
    );
  }
}
