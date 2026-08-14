package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.LeaderboardDto;
import com.nanobot.nanobotbackend.entity.LeaderboardEntity;
import com.nanobot.nanobotbackend.service.LeaderboardsService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class LeaderboardsControllerTest {

  private static final String PATH = "/leaderboards";
  private static final String MESSAGE_PATH = "$.message";

  private MockMvc mockMvc;

  @Mock
  private LeaderboardsService leaderboardsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new LeaderboardsController(leaderboardsService))
        .build();
  }

  @Test
  void createLeaderboardShouldReturnCreatedWhenNew() throws Exception {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    LeaderboardEntity entity = new LeaderboardEntity(dto);
    entity.setId("lb1");
    when(leaderboardsService.createLeaderboard(any()))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());
    verify(leaderboardsService).createLeaderboard(any());
  }

  @Test
  void createLeaderboardShouldReturnBadRequestWhenDuplicate() throws Exception {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    when(leaderboardsService.createLeaderboard(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath(MESSAGE_PATH).value("Leaderboard guildId/userId already exists")
      );
  }

  @Test
  void getLeaderboardsShouldReturnOkWithNoParams() throws Exception {
    when(leaderboardsService.getLeaderboards(null, null)).thenReturn(List.of());

    mockMvc.perform(get(PATH)).andExpect(status().isOk());
    verify(leaderboardsService).getLeaderboards(null, null);
  }

  @Test
  void getLeaderboardsShouldReturnOkWithGuildIdParam() throws Exception {
    when(leaderboardsService.getLeaderboards("g1", null))
      .thenReturn(List.of());

    mockMvc.perform(get(PATH).param("guildId", "g1")).andExpect(status().isOk());
    verify(leaderboardsService).getLeaderboards("g1", null);
  }

  @Test
  void getLeaderboardByIdShouldReturnOkWhenFound() throws Exception {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    entity.setGuildId("g1");
    entity.setUserId("u1");
    when(leaderboardsService.getLeaderboardById("lb1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/lb1")).andExpect(status().isOk());
    verify(leaderboardsService).getLeaderboardById("lb1");
  }

  @Test
  void getLeaderboardByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(leaderboardsService.getLeaderboardById("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Leaderboard ID not found"));
  }

  @Test
  void getLeaderboardByGuildIdAndUserIdShouldReturnOkWhenFound() throws Exception {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    entity.setGuildId("g1");
    entity.setUserId("u1");
    when(leaderboardsService.getLeaderboardByGuildIdAndUserId("g1", "u1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/guildId/g1/userId/u1"))
      .andExpect(status().isOk());
    verify(leaderboardsService).getLeaderboardByGuildIdAndUserId("g1", "u1");
  }

  @Test
  void getLeaderboardByGuildIdAndUserIdShouldReturnNotFoundWhenMissing()
    throws Exception {
    when(leaderboardsService.getLeaderboardByGuildIdAndUserId("g1", "u1"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/guildId/g1/userId/u1"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath(MESSAGE_PATH)
          .value("Leaderboard with specified guildId/userId not found")
      );
  }

  @Test
  void updateLeaderboardShouldReturnAcceptedWhenFound() throws Exception {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    LeaderboardEntity entity = new LeaderboardEntity(dto);
    entity.setId("lb1");
    when(leaderboardsService.updateLeaderboard(eq("lb1"), any()))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        put(PATH + "/lb1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());
    verify(leaderboardsService).updateLeaderboard(eq("lb1"), any());
  }

  @Test
  void updateLeaderboardShouldReturnNotFoundWhenMissing() throws Exception {
    LeaderboardDto dto = new LeaderboardDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    when(leaderboardsService.updateLeaderboard(eq("missing"), any()))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Leaderboard ID not found"));
  }

  @Test
  void deleteLeaderboardShouldReturnNoContentWhenFound() throws Exception {
    LeaderboardEntity entity = new LeaderboardEntity();
    entity.setId("lb1");
    when(leaderboardsService.deleteLeaderboard("lb1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(delete(PATH + "/lb1")).andExpect(status().isNoContent());
    verify(leaderboardsService).deleteLeaderboard("lb1");
  }

  @Test
  void deleteLeaderboardShouldReturnNotFoundWhenMissing() throws Exception {
    when(leaderboardsService.deleteLeaderboard("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Leaderboard ID not found"));
  }
}
