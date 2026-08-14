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
import com.nanobot.nanobotbackend.dto.PickupDto;
import com.nanobot.nanobotbackend.entity.PickupEntity;
import com.nanobot.nanobotbackend.service.PickupsService;
import java.util.Collections;
import java.util.Date;
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
class PickupsControllerTest {

  private static final String PATH = "/pickups";

  private MockMvc mockMvc;

  @Mock
  private PickupsService pickupsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new PickupsController(pickupsService))
        .build();
  }

  @Test
  void createPickupShouldReturnCreatedWhenSuccess() throws Exception {
    PickupEntity entity = new PickupEntity();
    entity.setId("pickup1");
    entity.setDropId("drop1");
    entity.setUserId("user1");
    when(pickupsService.createPickup(any())).thenReturn(Optional.of(entity));

    PickupDto dto = new PickupDto(null, "drop1", "user1", new Date());
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$").isNotEmpty());

    verify(pickupsService).createPickup(any());
  }

  @Test
  void createPickupShouldReturnBadRequestWhenDropIdUserIdAlreadyExists()
    throws Exception {
    when(pickupsService.createPickup(any())).thenReturn(Optional.empty());

    PickupDto dto = new PickupDto(null, "drop1", "user1", new Date());
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.message").value("Pickup dropId/userId already exists")
      );

    verify(pickupsService).createPickup(any());
  }

  @Test
  void getPickupsShouldReturnOkWithDropIdAndUserId() throws Exception {
    List<PickupEntity> entities = Collections.singletonList(new PickupEntity());
    when(pickupsService.getPickups("drop1", "user1")).thenReturn(entities);

    mockMvc
      .perform(get(PATH).param("dropId", "drop1").param("userId", "user1"))
      .andExpect(status().isOk());

    verify(pickupsService).getPickups("drop1", "user1");
  }

  @Test
  void getPickupsShouldReturnOkWithoutParams() throws Exception {
    List<PickupEntity> entities = Collections.singletonList(new PickupEntity());
    when(pickupsService.getPickups(null, null)).thenReturn(entities);

    mockMvc.perform(get(PATH)).andExpect(status().isOk());

    verify(pickupsService).getPickups(null, null);
  }

  @Test
  void getPickupByIdShouldReturnOkWhenFound() throws Exception {
    PickupEntity entity = new PickupEntity();
    entity.setId("pickup1");
    entity.setDropId("drop1");
    entity.setUserId("user1");
    when(pickupsService.getPickupById("pickup1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/pickup1"))
      .andExpect(status().isOk());

    verify(pickupsService).getPickupById("pickup1");
  }

  @Test
  void getPickupByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(pickupsService.getPickupById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Pickup ID not found"));

    verify(pickupsService).getPickupById("missing");
  }

  @Test
  void getPickupByDropIdAndUserIdShouldReturnOkWhenFound() throws Exception {
    PickupEntity entity = new PickupEntity();
    entity.setId("pickup1");
    entity.setDropId("drop1");
    entity.setUserId("user1");
    when(pickupsService.getPickupByDropIdAndUserId("drop1", "user1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/dropId/drop1/userId/user1"))
      .andExpect(status().isOk());

    verify(pickupsService).getPickupByDropIdAndUserId("drop1", "user1");
  }

  @Test
  void getPickupByDropIdAndUserIdShouldReturnNotFoundWhenMissing()
    throws Exception {
    when(pickupsService.getPickupByDropIdAndUserId("drop1", "user1"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/dropId/drop1/userId/user1"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message")
          .value("Pickup with specified dropId/userId not found")
      );

    verify(pickupsService).getPickupByDropIdAndUserId("drop1", "user1");
  }

  @Test
  void updatePickupShouldReturnAcceptedWhenFound() throws Exception {
    PickupEntity entity = new PickupEntity();
    entity.setId("pickup1");
    entity.setDropId("drop2");
    entity.setUserId("user2");
    PickupDto dto = new PickupDto("pickup1", "drop2", "user2", new Date());
    when(pickupsService.updatePickup(eq("pickup1"), any()))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        put(PATH + "/pickup1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(pickupsService).updatePickup(eq("pickup1"), any());
  }

  @Test
  void updatePickupShouldReturnNotFoundWhenMissing() throws Exception {
    PickupDto dto = new PickupDto("pickup1", "drop1", "user1", new Date());
    when(pickupsService.updatePickup(eq("missing"), any()))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Pickup ID not found"));

    verify(pickupsService).updatePickup(eq("missing"), any());
  }

  @Test
  void deletePickupShouldReturnNoContentWhenFound() throws Exception {
    PickupEntity entity = new PickupEntity();
    entity.setId("pickup1");
    when(pickupsService.deletePickup("pickup1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(delete(PATH + "/pickup1"))
      .andExpect(status().isNoContent());

    verify(pickupsService).deletePickup("pickup1");
  }

  @Test
  void deletePickupShouldReturnNotFoundWhenMissing() throws Exception {
    when(pickupsService.deletePickup("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Pickup ID not found"));

    verify(pickupsService).deletePickup("missing");
  }
}
