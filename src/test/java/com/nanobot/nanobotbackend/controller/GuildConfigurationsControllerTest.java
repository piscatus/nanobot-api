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

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.service.GuildConfigurationsService;
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
class GuildConfigurationsControllerTest {

  private static final String BASE_PATH = "/guildConfigurations";

  private MockMvc mockMvc;

  @Mock
  private GuildConfigurationsService guildConfigurationsService;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(
          new GuildConfigurationsController(guildConfigurationsService)
        )
        .build();
  }

  @Test
  void createGuildConfigurationShouldReturnCreatedWhenSuccess() throws Exception {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setGuildId("g1");
    GuildConfigurationsEntity created = new GuildConfigurationsEntity();
    created.setId("gc1");
    created.setGuildId("g1");
    when(guildConfigurationsService.createGuildConfiguration(any()))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\"}")
      )
      .andExpect(status().isCreated());

    verify(guildConfigurationsService).createGuildConfiguration(any());
  }

  @Test
  void createGuildConfigurationShouldReturnBadRequestWhenInvalidJson()
    throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(guildConfigurationsService, never()).createGuildConfiguration(any());
  }

  @Test
  void createGuildConfigurationShouldReturnBadRequestWhenDuplicateGuildId()
    throws Exception {
    when(guildConfigurationsService.createGuildConfiguration(any()))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\"}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(
        jsonPath("$.message").value("Guild Configuration guildId already exists")
      );

    verify(guildConfigurationsService).createGuildConfiguration(any());
  }

  @Test
  void getGuildConfigurationsShouldReturnOkWithList() throws Exception {
    when(guildConfigurationsService.getGuildConfigurations(null))
      .thenReturn(List.of(new GuildConfigurationsEntity()));

    mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk());

    verify(guildConfigurationsService).getGuildConfigurations(null);
  }

  @Test
  void getGuildConfigurationsShouldDelegateGuildIdParam() throws Exception {
    when(guildConfigurationsService.getGuildConfigurations("g1"))
      .thenReturn(List.of(new GuildConfigurationsEntity()));

    mockMvc.perform(get(BASE_PATH).param("guildId", "g1")).andExpect(status().isOk());

    verify(guildConfigurationsService).getGuildConfigurations("g1");
  }

  @Test
  void getGuildConfigurationByIdShouldReturnNotFoundWhenMissing()
    throws Exception {
    when(guildConfigurationsService.getGuildConfigurationById("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("Guild Configuration ID not found")
      );

    verify(guildConfigurationsService).getGuildConfigurationById("missing");
  }

  @Test
  void getGuildConfigurationByIdShouldReturnOkWhenFound() throws Exception {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc1");
    entity.setGuildId("g1");
    when(guildConfigurationsService.getGuildConfigurationById("gc1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(get(BASE_PATH + "/gc1")).andExpect(status().isOk());

    verify(guildConfigurationsService).getGuildConfigurationById("gc1");
  }

  @Test
  void getGuildConfigurationByGuildIdShouldReturnNotFoundWhenMissing()
    throws Exception {
    when(
      guildConfigurationsService.getGuildConfigurationByGuildId("missing")
    )
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/guildId/missing"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message")
          .value("Guild Configuration with specified guildId not found")
      );

    verify(guildConfigurationsService).getGuildConfigurationByGuildId("missing");
  }

  @Test
  void getGuildConfigurationByGuildIdShouldReturnOkWhenFound() throws Exception {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc1");
    entity.setGuildId("g1");
    when(guildConfigurationsService.getGuildConfigurationByGuildId("g1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(get(BASE_PATH + "/guildId/g1")).andExpect(status().isOk());

    verify(guildConfigurationsService).getGuildConfigurationByGuildId("g1");
  }

  @Test
  void updateGuildConfigurationShouldReturnNotFoundWhenIdMissing()
    throws Exception {
    when(
      guildConfigurationsService.updateGuildConfiguration(
        eq("missing"),
        any()
      )
    )
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g2\"}")
      )
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("Guild Configuration ID not found")
      );

    verify(guildConfigurationsService)
      .updateGuildConfiguration(eq("missing"), any());
  }

  @Test
  void updateGuildConfigurationShouldReturnAcceptedWhenSuccess()
    throws Exception {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc1");
    entity.setGuildId("g2");
    when(
      guildConfigurationsService.updateGuildConfiguration(eq("gc1"), any())
    )
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(
        put(BASE_PATH + "/gc1")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g2\"}")
      )
      .andExpect(status().isAccepted());

    verify(guildConfigurationsService)
      .updateGuildConfiguration(eq("gc1"), any());
  }

  @Test
  void deleteGuildConfigurationShouldReturnNotFoundWhenIdMissing()
    throws Exception {
    when(guildConfigurationsService.deleteGuildConfiguration("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("Guild Configuration ID not found")
      );

    verify(guildConfigurationsService).deleteGuildConfiguration("missing");
  }

  @Test
  void deleteGuildConfigurationShouldReturnNoContentWhenSuccess()
    throws Exception {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc1");
    when(guildConfigurationsService.deleteGuildConfiguration("gc1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/gc1")).andExpect(status().isNoContent());

    verify(guildConfigurationsService).deleteGuildConfiguration("gc1");
  }

  @Test
  void getOrCreateGuildConfigurationShouldReturnOk() throws Exception {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("gc1");
    entity.setGuildId("g1");
    when(guildConfigurationsService.getOrCreateGuildConfiguration("g1"))
      .thenReturn(entity);

    mockMvc
      .perform(get(BASE_PATH + "/getOrCreate/g1"))
      .andExpect(status().isOk());

    verify(guildConfigurationsService).getOrCreateGuildConfiguration("g1");
  }
}
