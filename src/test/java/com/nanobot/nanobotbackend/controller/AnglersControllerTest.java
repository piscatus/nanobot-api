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
import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.entity.AnglerEntity;
import com.nanobot.nanobotbackend.service.AnglersService;
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
class AnglersControllerTest {

  private static final String PATH = "/anglers";
  private static final String NOT_FOUND_MESSAGE = "Angler ID not found";

  private MockMvc mockMvc;

  @Mock
  private AnglersService anglersService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new AnglersController(anglersService)).build();
  }

  @Test
  void createAnglerShouldReturnCreatedWhenSuccess() throws Exception {
    AnglerDto dto = new AnglerDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersService.createAngler(any())).thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());

    verify(anglersService).createAngler(any());
  }

  @Test
  void createAnglerShouldReturnBadRequestWhenDuplicate() throws Exception {
    when(anglersService.createAngler(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Angler guildId/userId already exists"));

    verify(anglersService).createAngler(any());
  }

  @Test
  void getAnglersShouldReturnOkAndDelegateToService() throws Exception {
    List<AnglerEntity> anglers = List.of(new AnglerEntity());
    when(anglersService.getAnglers("g1", "u1")).thenReturn(anglers);

    mockMvc.perform(get(PATH).param("guildId", "g1").param("userId", "u1")).andExpect(status().isOk());

    verify(anglersService).getAnglers("g1", "u1");
  }

  @Test
  void getRestingAnglersShouldReturnOkAndDelegateToService() throws Exception {
    List<AnglerEntity> anglers = List.of(new AnglerEntity());
    when(anglersService.getRestingAnglers()).thenReturn(anglers);

    mockMvc.perform(get(PATH + "/resting")).andExpect(status().isOk());

    verify(anglersService).getRestingAnglers();
  }

  @Test
  void getAnglerByIdShouldReturnOkWhenFound() throws Exception {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersService.getAnglerById("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/id-1")).andExpect(status().isOk());

    verify(anglersService).getAnglerById("id-1");
  }

  @Test
  void getAnglerByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(anglersService.getAnglerById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));

    verify(anglersService).getAnglerById("missing");
  }

  @Test
  void getAnglerByGuildIdAndUserIdShouldReturnOkWhenFound() throws Exception {
    AnglerEntity entity = new AnglerEntity();
    entity.setGuildId("g1");
    entity.setUserId("u1");
    when(anglersService.getAnglerByGuildIdAndUserId("g1", "u1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/guildId/g1/userId/u1")).andExpect(status().isOk());

    verify(anglersService).getAnglerByGuildIdAndUserId("g1", "u1");
  }

  @Test
  void getAnglerByGuildIdAndUserIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(anglersService.getAnglerByGuildIdAndUserId("g1", "u1")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/guildId/g1/userId/u1"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Angler with specified guildId/userId not found"));

    verify(anglersService).getAnglerByGuildIdAndUserId("g1", "u1");
  }

  @Test
  void updateAnglersShouldReturnAcceptedWhenFound() throws Exception {
    AnglerDto dto = new AnglerDto();
    dto.setGuildId("g1");
    dto.setUserId("u1");
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersService.updateAngler(eq("id-1"), any())).thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        put(PATH + "/id-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(anglersService).updateAngler(eq("id-1"), any());
  }

  @Test
  void updateAnglersShouldReturnNotFoundWhenMissing() throws Exception {
    when(anglersService.updateAngler(eq("missing"), any())).thenReturn(Optional.empty());

    mockMvc
      .perform(put(PATH + "/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));

    verify(anglersService).updateAngler(eq("missing"), any());
  }

  @Test
  void deleteAnglersShouldReturnNoContentWhenFound() throws Exception {
    AnglerEntity entity = new AnglerEntity();
    when(anglersService.deleteAngler("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete(PATH + "/id-1")).andExpect(status().isNoContent());

    verify(anglersService).deleteAngler("id-1");
  }

  @Test
  void deleteAnglersShouldReturnNotFoundWhenMissing() throws Exception {
    when(anglersService.deleteAngler("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));

    verify(anglersService).deleteAngler("missing");
  }

  @Test
  void updateOrCreateAnglerShouldReturnOkAndDelegateToService() throws Exception {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersService.updateOrCreateAngler("g1", "u1")).thenReturn(entity);

    mockMvc
      .perform(put(PATH + "/updateOrCreate").param("guildId", "g1").param("userId", "u1"))
      .andExpect(status().isOk());

    verify(anglersService).updateOrCreateAngler("g1", "u1");
  }

  @Test
  void updateAnglerRestlessShouldReturnAcceptedWhenFound() throws Exception {
    AnglerEntity entity = new AnglerEntity();
    entity.setId("id-1");
    when(anglersService.updateAnglerRestless("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(put(PATH + "/restless/id-1")).andExpect(status().isAccepted());

    verify(anglersService).updateAnglerRestless("id-1");
  }

  @Test
  void updateAnglerRestlessShouldReturnNotFoundWhenMissing() throws Exception {
    when(anglersService.updateAnglerRestless("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(put(PATH + "/restless/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));

    verify(anglersService).updateAnglerRestless("missing");
  }

  @Test
  void createAnglerShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());
  }
}
