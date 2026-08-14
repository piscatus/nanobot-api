package com.nanobot.nanobotbackend.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.TransactionsResponseDto;
import com.nanobot.nanobotbackend.service.TransactionsServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class TransactionsRequestsControllerTest {

  private static final String PATH = "/requests/transactions";

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private TransactionsServices transactionsServices;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new TransactionsRequestsController(transactionsServices))
        .build();
  }

  @Test
  void transactionsShouldReturnOkAndDelegateToService() throws Exception {
    TransactionsResponseDto response = new TransactionsResponseDto();
    when(transactionsServices.transactions(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk());

    ArgumentCaptor<RequestDto> captor = ArgumentCaptor.forClass(RequestDto.class);
    verify(transactionsServices).transactions(captor.capture());
    assertEquals("guild-1", captor.getValue().getGuildId());
    assertEquals("user-1", captor.getValue().getUserId());
  }

  @Test
  void transactionsShouldReturnErrorWhenServiceReturnsError() throws Exception {
    TransactionsResponseDto response = new TransactionsResponseDto("Transaction failed");
    when(transactionsServices.transactions(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Transaction failed"));

    verify(transactionsServices).transactions(any());
  }

  @Test
  void transactionsShouldReturnBadRequestWhenMalformedJson() throws Exception {
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{invalid json}")
      )
      .andExpect(status().isBadRequest());

    verify(transactionsServices, never()).transactions(any());
  }

  @Test
  void transactionsShouldPassRequestBodyToService() throws Exception {
    RequestDto request = minimalRequest();
    request.setGuildId("custom-guild");
    request.setUserId("custom-user");
    request.setChannelId("ch-123");
    TransactionsResponseDto response = new TransactionsResponseDto();
    when(transactionsServices.transactions(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk());

    ArgumentCaptor<RequestDto> captor = ArgumentCaptor.forClass(RequestDto.class);
    verify(transactionsServices).transactions(captor.capture());
    RequestDto captured = captor.getValue();
    assertEquals("custom-guild", captured.getGuildId());
    assertEquals("custom-user", captured.getUserId());
    assertEquals("ch-123", captured.getChannelId());
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
