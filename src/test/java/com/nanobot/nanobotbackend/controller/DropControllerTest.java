package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.service.DropService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class DropControllerTest {

  private static final String PATH = "/drop/update";

  private MockMvc mockMvc;

  @Mock
  private DropService dropService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new DropController(dropService)).build();
  }

  @Test
  void updateDropMessageIdShouldReturnAcceptedWhenSuccess() throws Exception {
    BaseResponseDto successResponse = new BaseResponseDto();
    when(dropService.dropUpdate(any())).thenReturn(successResponse);

    String body =
      "{\"id\":\"drop123\",\"dropId\":\"msg456\",\"address\":null,\"channelId\":null,"
        +
        "\"confirmation\":false,\"duration\":null,\"global\":false,\"guildId\":null,"
        +
        "\"roleId\":null,\"input\":null,\"messageData\":null,\"random\":null,"
        +
        "\"receiverIds\":null,\"userId\":null,\"userIdsWithRole\":null,"
        +
        "\"userRoles\":null,\"users\":null}";

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().isAccepted());

    verify(dropService).dropUpdate(any());
  }

  @Test
  void updateDropShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());
  }

  @Test
  void updateDropMessageIdShouldReturnAcceptedWithErrorMessageWhenDropFails()
    throws Exception {
    BaseResponseDto errorResponse = new BaseResponseDto("Drop failed to update.");
    when(dropService.dropUpdate(any())).thenReturn(errorResponse);

    String body =
      "{\"id\":\"invalid\",\"dropId\":\"invalid\",\"address\":null,\"channelId\":null,"
        +
        "\"confirmation\":false,\"duration\":null,\"global\":false,\"guildId\":null,"
        +
        "\"roleId\":null,\"input\":null,\"messageData\":null,\"random\":null,"
        +
        "\"receiverIds\":null,\"userId\":null,\"userIdsWithRole\":null,"
        +
        "\"userRoles\":null,\"users\":null}";

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(body))
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.errorMessage").value("Drop failed to update."));
  }
}
