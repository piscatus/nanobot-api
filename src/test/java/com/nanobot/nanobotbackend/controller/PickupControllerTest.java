package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.PickupServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class PickupControllerTest {

  private static final String PATH = "/requests/pickup";

  private MockMvc mockMvc;

  @Mock
  private PickupServices pickupServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new PickupController(pickupServices)).build();
  }

  @Test
  void pickupShouldReturnAcceptedWhenSuccess() throws Exception {
    BaseResponseDto response = new BaseResponseDto();
    when(pickupServices.pickup(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isAccepted());

    verify(pickupServices).pickup(any());
  }

  @Test
  void pickupShouldReturnAcceptedWithErrorMessageWhenServiceReturnsError()
    throws Exception {
    BaseResponseDto errorResponse = new BaseResponseDto("Pickup failed");
    when(pickupServices.pickup(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(minimalRequest()))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.errorMessage").value("Pickup failed"));

    verify(pickupServices).pickup(any());
  }

  @Test
  void pickupShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(pickupServices, never()).pickup(any());
  }

  @Test
  void pickupShouldPassRequestBodyToService() throws Exception {
    BaseResponseDto response = new BaseResponseDto();
    when(pickupServices.pickup(any())).thenReturn(response);

    String requestBody = "{\"guildId\":\"g-123\",\"userId\":\"u-456\",\"channelId\":\"c-789\"}";
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isAccepted());

    verify(pickupServices).pickup(argThat(req ->
      "g-123".equals(req.getGuildId())
        && "u-456".equals(req.getUserId())
        && "c-789".equals(req.getChannelId())
    ));
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
