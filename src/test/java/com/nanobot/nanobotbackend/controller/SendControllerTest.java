package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.SendServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class SendControllerTest {

  private static final String BASE_PATH = "/requests";
  private static final String UPDATE_PATH = BASE_PATH + "/update";
  private static final String SEND_PATH = BASE_PATH + "/send";

  private MockMvc mockMvc;

  @Mock
  private SendServices sendServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new SendController(sendServices)).build();
  }

  @Test
  void updateShouldReturnOkAndDelegateToService() throws Exception {
    RequestDto request = minimalRequest();
    TransferResponseDto response = new TransferResponseDto();
    response.setConfirmation(true);
    when(sendServices.update(any())).thenReturn(response);

    mockMvc
      .perform(
        put(UPDATE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.confirmation").value(true));

    verify(sendServices).update(any());
  }

  @Test
  void updateShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    TransferResponseDto errorResponse = new TransferResponseDto("Transfer failed");
    when(sendServices.update(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        put(UPDATE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(minimalRequest()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Transfer failed"));

    verify(sendServices).update(any());
  }

  @Test
  void updateShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        put(UPDATE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(sendServices, never()).update(any());
  }

  @Test
  void sendShouldReturnOkAndDelegateToService() throws Exception {
    RequestDto request = minimalRequest();
    TransferResponseDto response = new TransferResponseDto();
    response.setConfirmation(true);
    when(sendServices.send(any())).thenReturn(response);

    mockMvc
      .perform(
        put(SEND_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.confirmation").value(true));

    verify(sendServices).send(any());
  }

  @Test
  void sendShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    TransferResponseDto errorResponse = new TransferResponseDto("Send failed");
    when(sendServices.send(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        put(SEND_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(minimalRequest()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Send failed"));

    verify(sendServices).send(any());
  }

  @Test
  void sendShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        put(SEND_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(sendServices, never()).send(any());
  }

  private RequestDto minimalRequest() {
    return new RequestDto(
      null,
      "channel-1",
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
