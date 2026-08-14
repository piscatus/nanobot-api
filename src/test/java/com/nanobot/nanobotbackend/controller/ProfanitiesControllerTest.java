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
import com.nanobot.nanobotbackend.dto.ProfanityDto;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import com.nanobot.nanobotbackend.service.ProfanitiesService;
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
class ProfanitiesControllerTest {

  private static final String BASE_PATH = "/profanities";

  private MockMvc mockMvc;

  @Mock
  private ProfanitiesService profanitiesService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new ProfanitiesController(profanitiesService)).build();
  }

  @Test
  void createProfanityShouldReturnCreatedWhenSuccess() throws Exception {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("badword");
    dto.setPlural("badwords");
    dto.setContains(false);

    ProfanityEntity created = new ProfanityEntity(dto);
    created.setId("prof-1");
    when(profanitiesService.createProfanity(any(ProfanityDto.class)))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("prof-1"))
      .andExpect(jsonPath("$.singular").value("badword"));

    verify(profanitiesService).createProfanity(any(ProfanityDto.class));
  }

  @Test
  void createProfanityShouldReturnBadRequestWhenDuplicate() throws Exception {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("badword");
    dto.setPlural("badwords");
    when(profanitiesService.createProfanity(any(ProfanityDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Profanity singular/plural already exists"));

    verify(profanitiesService).createProfanity(any(ProfanityDto.class));
  }

  @Test
  void getProfanitiesShouldReturnOkAndDelegateToService() throws Exception {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("prof-1");
    entity.setSingular("badword");
    when(profanitiesService.getProfanities(null)).thenReturn(List.of(entity));

    mockMvc
      .perform(get(BASE_PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].singular").value("badword"));

    verify(profanitiesService).getProfanities(null);
  }

  @Test
  void getProfanitiesShouldPassSingularParamWhenProvided() throws Exception {
    when(profanitiesService.getProfanities("badword")).thenReturn(List.of());

    mockMvc.perform(get(BASE_PATH).param("singular", "badword")).andExpect(status().isOk());

    verify(profanitiesService).getProfanities("badword");
  }

  @Test
  void getProfanityByIdShouldReturnOkWhenFound() throws Exception {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("prof-1");
    entity.setSingular("badword");
    when(profanitiesService.getProfanityById("prof-1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/prof-1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("prof-1"))
      .andExpect(jsonPath("$.singular").value("badword"));

    verify(profanitiesService).getProfanityById("prof-1");
  }

  @Test
  void getProfanityByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(profanitiesService.getProfanityById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Profanity ID not found"));

    verify(profanitiesService).getProfanityById("missing");
  }

  @Test
  void getProfanityBySingularShouldReturnOkWhenFound() throws Exception {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("prof-1");
    entity.setSingular("badword");
    when(profanitiesService.getProfanityBySingular("badword"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/singular/badword"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("prof-1"))
      .andExpect(jsonPath("$.singular").value("badword"));

    verify(profanitiesService).getProfanityBySingular("badword");
  }

  @Test
  void getProfanityBySingularShouldReturnNotFoundWhenMissing() throws Exception {
    when(profanitiesService.getProfanityBySingular("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/singular/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Profanity with specified singular not found"));

    verify(profanitiesService).getProfanityBySingular("missing");
  }

  @Test
  void updateProfanityShouldReturnAcceptedWhenFound() throws Exception {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("updated");
    dto.setPlural("updateds");

    ProfanityEntity updated = new ProfanityEntity(dto);
    updated.setId("prof-1");
    when(profanitiesService.updateProfanity(eq("prof-1"), any(ProfanityDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(BASE_PATH + "/prof-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(profanitiesService).updateProfanity(eq("prof-1"), any(ProfanityDto.class));
  }

  @Test
  void updateProfanityShouldReturnNotFoundWhenMissing() throws Exception {
    ProfanityDto dto = new ProfanityDto();
    dto.setSingular("updated");
    when(profanitiesService.updateProfanity(eq("missing"), any(ProfanityDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Profanity ID not found"));

    verify(profanitiesService).updateProfanity(eq("missing"), any(ProfanityDto.class));
  }

  @Test
  void deleteProfanityShouldReturnNoContentWhenFound() throws Exception {
    ProfanityEntity entity = new ProfanityEntity();
    entity.setId("prof-1");
    when(profanitiesService.deleteProfanity("prof-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/prof-1")).andExpect(status().isNoContent());

    verify(profanitiesService).deleteProfanity("prof-1");
  }

  @Test
  void deleteProfanityShouldReturnNotFoundWhenMissing() throws Exception {
    when(profanitiesService.deleteProfanity("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Profanity ID not found"));

    verify(profanitiesService).deleteProfanity("missing");
  }
}
