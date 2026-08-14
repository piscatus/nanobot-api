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

import com.nanobot.nanobotbackend.dto.GuildWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.service.GuildWalletsService;
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
class GuildWalletsControllerTest {

  private MockMvc mockMvc;

  @Mock
  private GuildWalletsService guildWalletsService;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new GuildWalletsController(guildWalletsService))
        .build();
  }

  @Test
  void createGuildWalletsShouldReturnCreatedWhenSuccess() throws Exception {
    GuildWalletsDto dto = new GuildWalletsDto();
    dto.setGuildId("g1");
    dto.setWallets(List.of(new WalletDto("XNO", "100")));
    GuildWalletsEntity created = new GuildWalletsEntity("g1");
    created.setId("gw1");
    created.setWallets(dto.getWallets());
    when(guildWalletsService.createGuildWallets(any())).thenReturn(
      Optional.of(created)
    );

    mockMvc
      .perform(
        post("/guildWallets")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\",\"wallets\":[{\"ticker\":\"XNO\",\"raw\":\"100\"}]}")
      )
      .andExpect(status().isCreated());

    verify(guildWalletsService).createGuildWallets(any());
  }

  @Test
  void createGuildWalletsShouldReturnBadRequestWhenDuplicateGuildId()
    throws Exception {
    when(guildWalletsService.createGuildWallets(any())).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(
        post("/guildWallets")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g1\",\"wallets\":[]}")
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Guild Wallets guildId already exists"));

    verify(guildWalletsService).createGuildWallets(any());
  }

  @Test
  void getGuildsWalletsShouldReturnOkWithList() throws Exception {
    when(guildWalletsService.getGuildsWallets(null)).thenReturn(
      List.of(new GuildWalletsEntity("g1"))
    );

    mockMvc.perform(get("/guildWallets")).andExpect(status().isOk());

    verify(guildWalletsService).getGuildsWallets(null);
  }

  @Test
  void getGuildWalletsByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(guildWalletsService.getGuildWalletsById("missing")).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(get("/guildWallets/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Wallets ID not found"));

    verify(guildWalletsService).getGuildWalletsById("missing");
  }

  @Test
  void getGuildWalletsByIdShouldReturnOkWhenFound() throws Exception {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    when(guildWalletsService.getGuildWalletsById("gw1")).thenReturn(
      Optional.of(entity)
    );

    mockMvc.perform(get("/guildWallets/gw1")).andExpect(status().isOk());

    verify(guildWalletsService).getGuildWalletsById("gw1");
  }

  @Test
  void updateGuildWalletShouldReturnNotFoundWhenIdMissing() throws Exception {
    when(guildWalletsService.updateGuildWallets(eq("missing"), any())).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(
        put("/guildWallets/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content("{\"guildId\":\"g2\",\"wallets\":[]}")
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Wallets ID not found"));

    verify(guildWalletsService).updateGuildWallets(eq("missing"), any());
  }

  @Test
  void deleteWalletShouldReturnNotFoundWhenIdMissing() throws Exception {
    when(guildWalletsService.deleteGuildWallets("missing")).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(delete("/guildWallets/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Wallets ID not found"));

    verify(guildWalletsService).deleteGuildWallets("missing");
  }

  @Test
  void deleteWalletShouldReturnNoContentWhenSuccess() throws Exception {
    GuildWalletsEntity entity = new GuildWalletsEntity("g1");
    entity.setId("gw1");
    entity.setWallets(List.of(new WalletDto("XNO", "0")));
    when(guildWalletsService.deleteGuildWallets("gw1")).thenReturn(
      Optional.of(entity)
    );

    mockMvc.perform(delete("/guildWallets/gw1")).andExpect(status().isNoContent());

    verify(guildWalletsService).deleteGuildWallets("gw1");
  }
}
