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
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.UserItemsResponseDto;
import com.nanobot.nanobotbackend.service.InventoryServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class InventoryControllerTest {

  private static final String PATH = "/requests/inventory";

  private MockMvc mockMvc;

  @Mock
  private InventoryServices inventoryServices;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new InventoryController(inventoryServices))
        .build();
  }

  @Test
  void inventoryShouldReturnOkAndDelegateToService() throws Exception {
    UserItemsResponseDto response = new UserItemsResponseDto();
    when(inventoryServices.inventory(any())).thenReturn(response);

    RequestDto request = minimalRequest();
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(request))
      )
      .andExpect(status().isOk());

    verify(inventoryServices).inventory(any());
  }

  @Test
  void inventoryShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(inventoryServices, never()).inventory(any());
  }

  @Test
  void inventoryShouldReturnOkWithErrorMessageWhenServiceReturnsError()
    throws Exception {
    UserItemsResponseDto errorResponse = new UserItemsResponseDto("Inventory unavailable");
    when(inventoryServices.inventory(any())).thenReturn(errorResponse);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(minimalRequest()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Inventory unavailable"));

    verify(inventoryServices).inventory(any());
  }

  @Test
  void inventoryShouldReturnResponseBodyWhenServiceReturnsSuccess()
    throws Exception {
    UserItemsResponseDto response = new UserItemsResponseDto();
    response.setUserItems(java.util.List.of());
    when(inventoryServices.inventory(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(minimalRequest()))
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.userItems").isArray());

    verify(inventoryServices).inventory(any());
  }

  @Test
  void inventoryShouldPassRequestBodyToService() throws Exception {
    UserItemsResponseDto response = new UserItemsResponseDto();
    when(inventoryServices.inventory(any())).thenReturn(response);

    String requestBody = "{\"guildId\":\"guild-1\",\"userId\":\"user-1\",\"channelId\":\"ch-1\"}";
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk());

    verify(inventoryServices).inventory(argThat(req ->
      "guild-1".equals(req.getGuildId())
        && "user-1".equals(req.getUserId())
        && "ch-1".equals(req.getChannelId())
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
