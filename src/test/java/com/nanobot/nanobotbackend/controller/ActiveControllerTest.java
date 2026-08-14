package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.ActiveServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class ActiveControllerTest {

  private static final String PATH = "/requests/active";

  private MockMvc mockMvc;

  @Mock
  private ActiveServices activeServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new ActiveController(activeServices)).build();
  }

  @Test
  void activeShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    response.setTransactionId("tx1");
    when(activeServices.active(any())).thenReturn(response);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transactionId").value("tx1"));

    verify(activeServices).active(any());
  }

  @Test
  void activeShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    TransferResponseDto errorResponse = new TransferResponseDto("Active failed");
    when(activeServices.active(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Active failed"));

    verify(activeServices).active(any());
  }

  @Test
  void activeShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(activeServices, never()).active(any());
  }
}
