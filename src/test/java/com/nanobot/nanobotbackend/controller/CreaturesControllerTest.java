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
import com.nanobot.nanobotbackend.dto.CreatureDto;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.service.CreaturesService;
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
class CreaturesControllerTest {

  private static final String PATH = "/creatures";
  private static final String MESSAGE_PATH = "$.message";

  private MockMvc mockMvc;

  @Mock
  private CreaturesService creaturesService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new CreaturesController(creaturesService))
        .build();
  }

  @Test
  void createCreatureShouldReturnCreatedWhenNew() throws Exception {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    CreatureEntity entity = new CreatureEntity(dto);
    entity.setId("c1");
    when(creaturesService.createCreature(any())).thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());
    verify(creaturesService).createCreature(any());
  }

  @Test
  void createCreatureShouldReturnBadRequestWhenDuplicate() throws Exception {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    when(creaturesService.createCreature(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath(MESSAGE_PATH).value("Creature name already exists"));
  }

  @Test
  void getCreaturesShouldReturnOk() throws Exception {
    when(creaturesService.getCreatures()).thenReturn(List.of());

    mockMvc.perform(get(PATH)).andExpect(status().isOk());
    verify(creaturesService).getCreatures();
  }

  @Test
  void getCreatureByIdShouldReturnOkWhenFound() throws Exception {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    when(creaturesService.getCreatureById("c1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/c1")).andExpect(status().isOk());
    verify(creaturesService).getCreatureById("c1");
  }

  @Test
  void getCreatureByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(creaturesService.getCreatureById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Creature ID not found"));
  }

  @Test
  void getCreatureByNameShouldReturnOkWhenFound() throws Exception {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    entity.setName("Fish");
    when(creaturesService.getCreatureByName("Fish")).thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/name/Fish")).andExpect(status().isOk());
    verify(creaturesService).getCreatureByName("Fish");
  }

  @Test
  void getCreatureByNameShouldReturnNotFoundWhenMissing() throws Exception {
    when(creaturesService.getCreatureByName("Unknown")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/name/Unknown"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath(MESSAGE_PATH).value("Creature with specified name not found")
      );
  }

  @Test
  void updateCreatureShouldReturnAcceptedWhenFound() throws Exception {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    CreatureEntity entity = new CreatureEntity(dto);
    entity.setId("c1");
    when(creaturesService.updateCreature(eq("c1"), any()))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        put(PATH + "/c1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());
    verify(creaturesService).updateCreature(eq("c1"), any());
  }

  @Test
  void updateCreatureShouldReturnNotFoundWhenMissing() throws Exception {
    CreatureDto dto = new CreatureDto("Fish", "Fish", "🐟");
    when(creaturesService.updateCreature(eq("missing"), any()))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Creature ID not found"));
  }

  @Test
  void deleteCreatureShouldReturnNoContentWhenFound() throws Exception {
    CreatureEntity entity = new CreatureEntity();
    entity.setId("c1");
    when(creaturesService.deleteCreature("c1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete(PATH + "/c1")).andExpect(status().isNoContent());
    verify(creaturesService).deleteCreature("c1");
  }

  @Test
  void deleteCreatureShouldReturnNotFoundWhenMissing() throws Exception {
    when(creaturesService.deleteCreature("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(MESSAGE_PATH).value("Creature ID not found"));
  }
}
