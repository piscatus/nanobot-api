package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.CreaturesResponseDto;
import com.nanobot.nanobotbackend.service.CreaturesServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CreaturesRequestsControllerTest {

  private static final String PATH = "/requests/creatures";

  private MockMvc mockMvc;

  @Mock
  private CreaturesServices creaturesServices;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new CreaturesRequestsController(creaturesServices))
        .build();
  }

  @Test
  void creaturesShouldReturnOkAndDelegateToService() throws Exception {
    CreaturesResponseDto response = new CreaturesResponseDto();
    when(creaturesServices.creatures(any())).thenReturn(response);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(creaturesServices).creatures(any());
  }

  @Test
  void creaturesShouldPassRequestBodyAndReturnResponseBody() throws Exception {
    String requestBody = "{\"guildId\":\"g1\",\"userId\":\"u1\",\"channelId\":\"c1\"}";
    CreaturesResponseDto response = new CreaturesResponseDto();
    response.setCreatures(java.util.List.of());
    when(creaturesServices.creatures(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.creatures").isArray());

    verify(creaturesServices).creatures(any());
  }

  @Test
  void creaturesShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(creaturesServices, never()).creatures(any());
  }

  @Test
  void creaturesShouldReturnOkWithErrorMessageWhenServiceReturnsError() throws Exception {
    CreaturesResponseDto errorResponse = new CreaturesResponseDto("Creatures fetch failed");
    when(creaturesServices.creatures(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Creatures fetch failed"));

    verify(creaturesServices).creatures(any());
  }
}
