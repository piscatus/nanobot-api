package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.DropDto;
import com.nanobot.nanobotbackend.entity.DropEntity;
import com.nanobot.nanobotbackend.service.DropsService;
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
class DropsControllerTest {

  private static final String BASE_PATH = "/drops";

  private MockMvc mockMvc;

  @Mock
  private DropsService dropsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new DropsController(dropsService)).build();
  }

  @Test
  void createDropShouldReturnCreatedWhenSuccess() throws Exception {
    DropDto dto = new DropDto();
    dto.setMessageId("msg-1");
    dto.setGuildId("g1");
    dto.setChannelId("c1");
    DropEntity created = new DropEntity();
    created.setId("d1");
    created.setMessageId("msg-1");
    created.setGuildId("g1");
    created.setChannelId("c1");
    when(dropsService.createDrop(any(DropDto.class))).thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("d1"))
      .andExpect(jsonPath("$.messageId").value("msg-1"))
      .andExpect(jsonPath("$.guildId").value("g1"));

    verify(dropsService).createDrop(any(DropDto.class));
  }

  @Test
  void createDropShouldReturnBadRequestWhenDuplicate() throws Exception {
    DropDto dto = new DropDto();
    dto.setMessageId("msg-1");
    when(dropsService.createDrop(any(DropDto.class))).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Drop dropId already exists"));

    verify(dropsService).createDrop(any(DropDto.class));
  }

  @Test
  void createDropShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(dropsService, never()).createDrop(any());
  }

  @Test
  void getDropsShouldReturnOkAndDelegateToService() throws Exception {
    DropEntity drop = new DropEntity();
    drop.setId("d1");
    drop.setMessageId("msg-1");
    when(dropsService.getDrops(null)).thenReturn(List.of(drop));

    mockMvc
      .perform(get(BASE_PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value("d1"))
      .andExpect(jsonPath("$[0].messageId").value("msg-1"));

    verify(dropsService).getDrops(null);
  }

  @Test
  void getDropsShouldPassMessageIdParamWhenProvided() throws Exception {
    List<DropEntity> drops = List.of(new DropEntity());
    when(dropsService.getDrops("msg-123")).thenReturn(drops);

    mockMvc
      .perform(get(BASE_PATH).param("messageId", "msg-123"))
      .andExpect(status().isOk());

    verify(dropsService).getDrops("msg-123");
  }

  @Test
  void getDropByIdShouldReturnOkWhenFound() throws Exception {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    when(dropsService.getDropById("d1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/d1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("d1"));

    verify(dropsService).getDropById("d1");
  }

  @Test
  void getDropByIdShouldReturnNotFoundWhenEmpty() throws Exception {
    when(dropsService.getDropById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Drop ID not found"));

    verify(dropsService).getDropById("missing");
  }

  @Test
  void getDropByMessageIdShouldReturnOkWhenFound() throws Exception {
    DropEntity entity = new DropEntity();
    entity.setMessageId("msg-1");
    when(dropsService.getDropByMessageId("msg-1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/messageId/msg-1/userId/any"))
      .andExpect(status().isOk());

    verify(dropsService).getDropByMessageId("msg-1");
  }

  @Test
  void getDropByMessageIdShouldReturnNotFoundWhenEmpty() throws Exception {
    when(dropsService.getDropByMessageId("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/messageId/missing/userId/any"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Drop messageId not found"));

    verify(dropsService).getDropByMessageId("missing");
  }

  @Test
  void updateDropShouldReturnAcceptedWhenSuccess() throws Exception {
    DropDto dto = new DropDto();
    dto.setMessageId("msg-1");
    DropEntity updated = new DropEntity();
    updated.setId("d1");
    when(dropsService.updateDrop(eq("d1"), any(DropDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(BASE_PATH + "/d1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.id").value("d1"));

    verify(dropsService).updateDrop(eq("d1"), any(DropDto.class));
  }

  @Test
  void updateDropShouldReturnNotFoundWhenEmpty() throws Exception {
    DropDto dto = new DropDto();
    dto.setMessageId("msg-1");
    when(dropsService.updateDrop(eq("missing"), any(DropDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Drop ID not found"));

    verify(dropsService).updateDrop(eq("missing"), any(DropDto.class));
  }

  @Test
  void deleteDropShouldReturnNoContentWhenSuccess() throws Exception {
    DropEntity entity = new DropEntity();
    entity.setId("d1");
    when(dropsService.deleteDrop("d1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/d1")).andExpect(status().isNoContent());

    verify(dropsService).deleteDrop("d1");
  }

  @Test
  void deleteDropShouldReturnNotFoundWhenEmpty() throws Exception {
    when(dropsService.deleteDrop("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Drop ID not found"));

    verify(dropsService).deleteDrop("missing");
  }
}
