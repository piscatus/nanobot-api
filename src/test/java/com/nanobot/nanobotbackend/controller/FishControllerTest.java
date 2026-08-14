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
import com.nanobot.nanobotbackend.service.FishServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FishControllerTest {

  private static final String PATH = "/requests/fish";

  private MockMvc mockMvc;

  @Mock
  private FishServices fishServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new FishController(fishServices)).build();
  }

  @Test
  void fishShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    response.setConfirmation(true);
    when(fishServices.fish(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.confirmation").value(true));

    verify(fishServices).fish(any());
  }

  @Test
  void fishShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    TransferResponseDto response = new TransferResponseDto("Must wait to fish again");
    when(fishServices.fish(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Must wait to fish again"));

    verify(fishServices).fish(any());
  }

  @Test
  void fishShouldReturnBadRequestWhenMalformedJson() throws Exception {
    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{invalid json}")
      )
      .andExpect(status().isBadRequest());

    verify(fishServices, never()).fish(any());
  }

  @Test
  void fishShouldDelegateRequestToServiceWithCorrectBody() throws Exception {
    RequestDto request = minimalRequest();
    request.setGuildId("guild-123");
    request.setUserId("user-456");
    TransferResponseDto response = new TransferResponseDto();
    response.setConfirmation(true);
    when(fishServices.fish(any())).thenReturn(response);

    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk());

    verify(fishServices).fish(any());
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
