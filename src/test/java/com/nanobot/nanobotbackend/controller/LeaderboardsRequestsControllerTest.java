package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.LeaderboardsResponseDto;
import com.nanobot.nanobotbackend.service.LeaderboardsServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LeaderboardsRequestsControllerTest {

  private static final String PATH = "/requests/leaderboards";

  private MockMvc mockMvc;

  @Mock
  private LeaderboardsServices leaderboardsServices;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new LeaderboardsRequestsController(leaderboardsServices))
        .build();
  }

  @Test
  void leaderboardsShouldReturnOkAndDelegateToService() throws Exception {
    LeaderboardsResponseDto response = new LeaderboardsResponseDto();
    when(leaderboardsServices.leaderboards(any())).thenReturn(response);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(leaderboardsServices).leaderboards(any());
  }

  @Test
  void leaderboardsShouldPassRequestBodyAndReturnResponseBody() throws Exception {
    String requestBody = "{\"guildId\":\"g1\",\"userId\":\"u1\",\"channelId\":\"c1\"}";
    LeaderboardsResponseDto response = new LeaderboardsResponseDto();
    response.setLeaderboards(java.util.List.of());
    when(leaderboardsServices.leaderboards(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.leaderboards").isArray());

    verify(leaderboardsServices).leaderboards(any());
  }

  @Test
  void leaderboardsShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(leaderboardsServices, never()).leaderboards(any());
  }

  @Test
  void leaderboardsShouldReturnOkWithErrorMessageWhenServiceReturnsError() throws Exception {
    LeaderboardsResponseDto errorResponse = new LeaderboardsResponseDto("Leaderboards unavailable");
    when(leaderboardsServices.leaderboards(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Leaderboards unavailable"));

    verify(leaderboardsServices).leaderboards(any());
  }
}
