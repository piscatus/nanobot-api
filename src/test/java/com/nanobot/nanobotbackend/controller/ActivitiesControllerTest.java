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
import com.nanobot.nanobotbackend.dto.ActivityDto;
import com.nanobot.nanobotbackend.entity.ActivityEntity;
import com.nanobot.nanobotbackend.service.ActivitiesService;
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
class ActivitiesControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private ActivitiesService activitiesService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new ActivitiesController(activitiesService)).build();
  }

  @Test
  void createActivityReturnsCreatedWhenSuccess() throws Exception {
    ActivityDto dto = new ActivityDto();
    dto.setGuildId("g1");
    dto.setChannelId("ch1");
    dto.setUserId("u1");
    ActivityEntity created = new ActivityEntity();
    created.setId("act-1");
    created.setGuildId("g1");
    created.setChannelId("ch1");
    created.setUserId("u1");

    when(activitiesService.createActivity(any(ActivityDto.class)))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post("/activities")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$").isNotEmpty());

    verify(activitiesService).createActivity(any(ActivityDto.class));
  }

  @Test
  void createActivityReturnsBadRequestWhenDuplicate() throws Exception {
    ActivityDto dto = new ActivityDto();
    dto.setGuildId("g1");
    dto.setChannelId("ch1");
    dto.setUserId("u1");

    when(activitiesService.createActivity(any(ActivityDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post("/activities")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$").isNotEmpty());

    verify(activitiesService).createActivity(any(ActivityDto.class));
  }

  @Test
  void getActivitiesReturnsOkWithList() throws Exception {
    ActivityEntity e1 = new ActivityEntity();
    e1.setId("act-1");
    when(activitiesService.getActivities(null, null, null))
      .thenReturn(List.of(e1));

    mockMvc
      .perform(get("/activities"))
      .andExpect(status().isOk());

    verify(activitiesService).getActivities(null, null, null);
  }

  @Test
  void getActivitiesWithParamsPassesToService() throws Exception {
    when(activitiesService.getActivities("g1", "ch1", "u1"))
      .thenReturn(List.of());

    mockMvc
      .perform(get("/activities").param("guildId", "g1").param("channelId", "ch1").param("userId", "u1"))
      .andExpect(status().isOk());

    verify(activitiesService).getActivities("g1", "ch1", "u1");
  }

  @Test
  void getActivityByIdReturnsOkWhenFound() throws Exception {
    ActivityEntity entity = new ActivityEntity();
    entity.setId("act-1");
    when(activitiesService.getActivityById("act-1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get("/activities/act-1"))
      .andExpect(status().isOk());

    verify(activitiesService).getActivityById("act-1");
  }

  @Test
  void getActivityByIdReturnsNotFoundWhenMissing() throws Exception {
    when(activitiesService.getActivityById("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get("/activities/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Activity ID not found"));

    verify(activitiesService).getActivityById("missing");
  }

  @Test
  void updateActivityReturnsAcceptedWhenSuccess() throws Exception {
    ActivityDto dto = new ActivityDto();
    dto.setGuildId("g1");
    ActivityEntity updated = new ActivityEntity();
    updated.setId("act-1");
    when(activitiesService.updateActivity(eq("act-1"), any(ActivityDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put("/activities/act-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(activitiesService).updateActivity(eq("act-1"), any(ActivityDto.class));
  }

  @Test
  void updateActivityReturnsNotFoundWhenMissing() throws Exception {
    ActivityDto dto = new ActivityDto();
    when(activitiesService.updateActivity(eq("missing"), any(ActivityDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put("/activities/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Activity ID not found"));

    verify(activitiesService).updateActivity(eq("missing"), any(ActivityDto.class));
  }

  @Test
  void deleteActivityReturnsNoContentWhenSuccess() throws Exception {
    ActivityEntity deleted = new ActivityEntity();
    deleted.setId("act-1");
    when(activitiesService.deleteActivity("act-1"))
      .thenReturn(Optional.of(deleted));

    mockMvc
      .perform(delete("/activities/act-1"))
      .andExpect(status().isNoContent());

    verify(activitiesService).deleteActivity("act-1");
  }

  @Test
  void deleteActivityReturnsNotFoundWhenMissing() throws Exception {
    when(activitiesService.deleteActivity("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(delete("/activities/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Activity ID not found"));

    verify(activitiesService).deleteActivity("missing");
  }

  @Test
  void updateOrCreateActivityReturnsOk() throws Exception {
    ActivityEntity result = new ActivityEntity();
    result.setId("act-1");
    result.setGuildId("g1");
    result.setChannelId("ch1");
    result.setUserId("u1");
    when(activitiesService.updateOrCreateActivity("g1", "ch1", "u1"))
      .thenReturn(result);

    mockMvc
      .perform(
        put("/activities/updateOrCreate")
          .param("guildId", "g1")
          .param("channelId", "ch1")
          .param("userId", "u1")
      )
      .andExpect(status().isOk());

    verify(activitiesService).updateOrCreateActivity("g1", "ch1", "u1");
  }
}
