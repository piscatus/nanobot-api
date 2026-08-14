package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.TransferResponseDto;
import com.nanobot.nanobotbackend.service.TransferServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TransferControllerTest {

  private static final String BASE_PATH = "/requests";

  private MockMvc mockMvc;

  @Mock
  private TransferServices transferServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new TransferController(transferServices))
        .build();
  }

  @Test
  void dropShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    response.setTransactionId("tx1");
    when(transferServices.drop(any())).thenReturn(response);

    mockMvc
      .perform(
        put(BASE_PATH + "/drop")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transactionId").value("tx1"));

    verify(transferServices).drop(any());
  }

  @Test
  void dropShouldReturnErrorResponseWhenServiceReturnsError() throws Exception {
    TransferResponseDto errorResponse = new TransferResponseDto("Drop failed");
    when(transferServices.drop(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        put(BASE_PATH + "/drop")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Drop failed"));

    verify(transferServices).drop(any());
  }

  @Test
  void giftShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    response.setTransactionId("tx2");
    when(transferServices.gift(any())).thenReturn(response);

    mockMvc
      .perform(
        put(BASE_PATH + "/gift")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transactionId").value("tx2"));

    verify(transferServices).gift(any());
  }

  @Test
  void mergeShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    when(transferServices.merge(any())).thenReturn(response);

    mockMvc
      .perform(
        put(BASE_PATH + "/merge")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk());

    verify(transferServices).merge(any());
  }

  @Test
  void rainShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    when(transferServices.rain(any())).thenReturn(response);

    mockMvc
      .perform(
        put(BASE_PATH + "/rain")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk());

    verify(transferServices).rain(any());
  }

  @Test
  void sellShouldReturnOkAndDelegateToService() throws Exception {
    TransferResponseDto response = new TransferResponseDto();
    response.setTransactionId("tx3");
    when(transferServices.sell(any())).thenReturn(response);

    mockMvc
      .perform(
        put(BASE_PATH + "/sell")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{}")
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.transactionId").value("tx3"));

    verify(transferServices).sell(any());
  }

  @Test
  void giftShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        put(BASE_PATH + "/gift")
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(transferServices, never()).gift(any());
  }

  @Test
  void dropShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        put(BASE_PATH + "/drop")
          .contentType(MediaType.APPLICATION_JSON)
          .content("not json")
      )
      .andExpect(status().isBadRequest());

    verify(transferServices, never()).drop(any());
  }
}
