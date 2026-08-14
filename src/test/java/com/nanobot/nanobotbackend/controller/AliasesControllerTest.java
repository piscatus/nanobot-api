package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import com.nanobot.nanobotbackend.service.AliasesService;
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
class AliasesControllerTest {

  private static final String PATH = "/aliases";
  private static final String GUILD_1 = "guild1";

  private MockMvc mockMvc;

  @Mock
  private AliasesService aliasesService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new AliasesController(aliasesService)).build();
  }

  @Test
  void createAliasShouldReturnCreatedWhenServiceReturnsEntity() throws Exception {
    AliasDto dto = new AliasDto(GUILD_1, "owner1", "cat", "cats", "CAT", "1", ":cat:");
    when(aliasesService.createAlias(any())).thenReturn(Optional.of(new AliasEntity(dto)));

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isCreated());

    verify(aliasesService).createAlias(any());
  }

  @Test
  void createAliasShouldReturnBadRequestWhenDuplicate() throws Exception {
    when(aliasesService.createAlias(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Alias guildId/singular already exists"));
  }

  @Test
  void getAliasesShouldReturnOk() throws Exception {
    when(aliasesService.getAliases(GUILD_1, "cat")).thenReturn(List.of());

    mockMvc
      .perform(get(PATH).param("guildId", GUILD_1).param("singular", "cat"))
      .andExpect(status().isOk());

    verify(aliasesService).getAliases(GUILD_1, "cat");
  }

  @Test
  void getAliasByIdShouldReturnNotFound() throws Exception {
    when(aliasesService.getAliasById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Alias ID not found"));
  }

  @Test
  void updateAliasShouldReturnAcceptedWhenFound() throws Exception {
    AliasDto dto = new AliasDto(GUILD_1, "cat", "cats", "CAT", "1", ":cat:");
    when(aliasesService.updateAlias(any(), any())).thenReturn(Optional.of(new AliasEntity(dto)));

    mockMvc
      .perform(put(PATH + "/id1").contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isAccepted());
  }

  @Test
  void deleteAliasShouldReturnNoContentWhenFound() throws Exception {
    when(aliasesService.deleteAlias("id1")).thenReturn(Optional.of(new AliasEntity()));

    mockMvc.perform(delete(PATH + "/id1")).andExpect(status().isNoContent());
  }
}
