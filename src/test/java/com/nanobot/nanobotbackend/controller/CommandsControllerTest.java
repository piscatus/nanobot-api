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
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.CommandEntity;
import com.nanobot.nanobotbackend.service.CommandsService;
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
class CommandsControllerTest {

  private static final String PATH = "/commands";
  private static final String JSON_MESSAGE_PATH = "$.message";

  private MockMvc mockMvc;

  @Mock
  private CommandsService commandsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new CommandsController(commandsService)).build();
  }

  @Test
  void createCommandShouldReturnCreatedOnSuccess() throws Exception {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    dto.setStatus(StatusDto.ACTIVE);
    when(commandsService.createCommand(any())).thenReturn(Optional.of(new CommandEntity(dto)));

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isCreated());

    verify(commandsService).createCommand(any());
  }

  @Test
  void createCommandShouldReturnBadRequestWhenDuplicate() throws Exception {
    when(commandsService.createCommand(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Command name already exists"));
  }

  @Test
  void createCommandShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(commandsService, never()).createCommand(any());
  }

  @Test
  void getCommandByIdShouldReturnOkWhenFound() throws Exception {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    when(commandsService.getCommandById("c1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get(PATH + "/c1")).andExpect(status().isOk());
    verify(commandsService).getCommandById("c1");
  }

  @Test
  void getCommandByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(commandsService.getCommandById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Command ID not found"));
  }

  @Test
  void getCommandByNameShouldReturnOkWhenFound() throws Exception {
    CommandEntity entity = new CommandEntity();
    entity.setId("c1");
    entity.setName("ping");
    entity.setCommandId("cmd-1");
    when(commandsService.getCommandByName("ping")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/name/ping"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("c1"))
      .andExpect(jsonPath("$.name").value("ping"));

    verify(commandsService).getCommandByName("ping");
  }

  @Test
  void getCommandByNameShouldReturnNotFound() throws Exception {
    when(commandsService.getCommandByName("unknown")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/name/unknown"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Command with specified name not found"));
  }

  @Test
  void getCommandsShouldReturnOkWhenNameParamProvided() throws Exception {
    when(commandsService.getCommands("ping")).thenReturn(List.of());

    mockMvc.perform(get(PATH).param("name", "ping")).andExpect(status().isOk());
    verify(commandsService).getCommands("ping");
  }

  @Test
  void getCommandsShouldReturnOkWhenNameParamOmitted() throws Exception {
    when(commandsService.getCommands(null)).thenReturn(List.of());

    mockMvc.perform(get(PATH)).andExpect(status().isOk());
    verify(commandsService).getCommands(null);
  }

  @Test
  void updateCommandByIdShouldReturnAcceptedWhenFound() throws Exception {
    CommandDto dto = new CommandDto("pong", "cmd-2");
    dto.setStatus(StatusDto.ACTIVE);
    CommandEntity updated = new CommandEntity();
    updated.setId("c1");
    updated.setName("pong");
    when(commandsService.updateCommand(eq("c1"), any())).thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(PATH + "/c1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(commandsService).updateCommand(eq("c1"), any());
  }

  @Test
  void updateCommandByIdShouldReturnNotFoundWhenMissing() throws Exception {
    CommandDto dto = new CommandDto("pong", "cmd-2");
    when(commandsService.updateCommand(eq("missing"), any())).thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Command ID not found"));
  }

  @Test
  void updateCommandsShouldReturnAcceptedWhenUpdatesSucceed() throws Exception {
    CommandDto dto = new CommandDto("ping", "cmd-1");
    dto.setStatus(StatusDto.ACTIVE);
    when(commandsService.updateCommands(any())).thenReturn(List.of(dto));

    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(List.of(dto)))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$[0].name").value("ping"));

    verify(commandsService).updateCommands(any());
  }

  @Test
  void updateCommandsShouldReturnNotFoundWhenNoUpdates() throws Exception {
    when(commandsService.updateCommands(any())).thenReturn(List.of());

    mockMvc
      .perform(put(PATH).contentType(MediaType.APPLICATION_JSON).content("[]"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Commands not updated"));
  }

  @Test
  void updateCommandsShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        put(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(commandsService, never()).updateCommands(any());
  }

  @Test
  void deleteCommandShouldReturnNoContentWhenFound() throws Exception {
    when(commandsService.deleteCommand("id1")).thenReturn(Optional.of(new CommandEntity()));

    mockMvc.perform(delete(PATH + "/id1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteCommandShouldReturnNotFoundWhenMissing() throws Exception {
    when(commandsService.deleteCommand("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Command ID not found"));
  }
}
