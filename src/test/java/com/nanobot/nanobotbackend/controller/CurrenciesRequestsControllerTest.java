package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.CurrenciesResponseDto;
import com.nanobot.nanobotbackend.dto.HelpResponseDto;
import com.nanobot.nanobotbackend.dto.ReceiveResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.CurrenciesServices;
import java.util.Collections;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CurrenciesRequestsControllerTest {

  private static final String BASE_PATH = "/requests";

  private MockMvc mockMvc;

  @Mock
  private CurrenciesServices currenciesServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new CurrenciesRequestsController(currenciesServices))
        .build();
  }

  @Test
  void currenciesShouldReturnOkWithResponseBody() throws Exception {
    CurrenciesResponseDto response = new CurrenciesResponseDto();
    response.setCurrencies(Collections.emptyList());
    when(currenciesServices.currencies(any(RequestDto.class))).thenReturn(response);

    mockMvc
      .perform(
        post(BASE_PATH + "/currencies")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\",\"channelId\":\"ch1\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.currencies").isArray());

    verify(currenciesServices).currencies(any(RequestDto.class));
  }

  @Test
  void currenciesShouldReturnErrorWhenServiceReturnsError() throws Exception {
    CurrenciesResponseDto errorResponse = new CurrenciesResponseDto("Service error");
    when(currenciesServices.currencies(any(RequestDto.class)))
      .thenReturn(errorResponse);

    mockMvc
      .perform(
        post(BASE_PATH + "/currencies")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Service error"));

    verify(currenciesServices).currencies(any(RequestDto.class));
  }

  @Test
  void helpShouldReturnOkWithResponseBody() throws Exception {
    HelpResponseDto response = new HelpResponseDto();
    response.setCurrencies(Collections.emptyList());
    when(currenciesServices.help(any(RequestDto.class))).thenReturn(response);

    mockMvc
      .perform(
        post(BASE_PATH + "/help")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.currencies").isArray());

    verify(currenciesServices).help(any(RequestDto.class));
  }

  @Test
  void helpShouldReturnErrorWhenServiceReturnsError() throws Exception {
    HelpResponseDto errorResponse = new HelpResponseDto("Help unavailable");
    when(currenciesServices.help(any(RequestDto.class)))
      .thenReturn(errorResponse);

    mockMvc
      .perform(
        post(BASE_PATH + "/help")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Help unavailable"));

    verify(currenciesServices).help(any(RequestDto.class));
  }

  @Test
  void receiveShouldReturnOkWithResponseBody() throws Exception {
    ReceiveResponseDto response = new ReceiveResponseDto();
    response.setAddresses(Collections.emptyList());
    response.setCurrencies(Collections.emptyList());
    when(currenciesServices.receive(any(RequestDto.class))).thenReturn(response);

    mockMvc
      .perform(
        post(BASE_PATH + "/receive")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"userId\":\"u1\",\"guildId\":\"g1\"}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.addresses").isArray())
      .andExpect(jsonPath("$.currencies").isArray());

    verify(currenciesServices).receive(any(RequestDto.class));
  }

  @Test
  void receiveShouldReturnErrorWhenServiceReturnsError() throws Exception {
    ReceiveResponseDto errorResponse = new ReceiveResponseDto("Receive failed");
    when(currenciesServices.receive(any(RequestDto.class)))
      .thenReturn(errorResponse);

    mockMvc
      .perform(
        post(BASE_PATH + "/receive")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Receive failed"));

    verify(currenciesServices).receive(any(RequestDto.class));
  }

  @Test
  void currenciesShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH + "/currencies")
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());
  }
}
