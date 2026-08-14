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
import com.nanobot.nanobotbackend.dto.UserWalletsDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.service.UserWalletsService;
import java.util.Collections;
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
class UserWalletsControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserWalletsService userWalletsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new UserWalletsController(userWalletsService))
        .build();
  }

  @Test
  void getUsersWalletsShouldReturnOkWithList() throws Exception {
    UserWalletsEntity entity = new UserWalletsEntity("user1");
    entity.setWallets(List.of(new WalletDto("XNO", "100")));
    when(userWalletsService.getUsersWallets(null)).thenReturn(List.of(entity));

    mockMvc
      .perform(get("/userWallets"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].userId").value("user1"));

    verify(userWalletsService).getUsersWallets(null);
  }

  @Test
  void getUsersWalletsWithUserIdParamShouldDelegate() throws Exception {
    when(userWalletsService.getUsersWallets("user123")).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/userWallets").param("userId", "user123")).andExpect(status().isOk());

    verify(userWalletsService).getUsersWallets("user123");
  }

  @Test
  void getUserWalletsByIdShouldReturnOkWhenFound() throws Exception {
    UserWalletsEntity entity = new UserWalletsEntity("user1");
    entity.setId("id-1");
    when(userWalletsService.getUserWalletsById("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get("/userWallets/id-1")).andExpect(status().isOk());

    verify(userWalletsService).getUserWalletsById("id-1");
  }

  @Test
  void getUserWalletsByIdShouldReturn404WhenNotFound() throws Exception {
    when(userWalletsService.getUserWalletsById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/userWallets/missing"))
      .andExpect(status().isNotFound());

    verify(userWalletsService).getUserWalletsById("missing");
  }

  @Test
  void getUserWalletsByUserIdShouldReturnOkWhenFound() throws Exception {
    UserWalletsEntity entity = new UserWalletsEntity("user1");
    when(userWalletsService.getUserWalletsByUserId("user1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get("/userWallets/userId/user1"))
      .andExpect(status().isOk());

    verify(userWalletsService).getUserWalletsByUserId("user1");
  }

  @Test
  void getUserWalletsByUserIdShouldReturn404WhenNotFound() throws Exception {
    when(userWalletsService.getUserWalletsByUserId("unknown")).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/userWallets/userId/unknown"))
      .andExpect(status().isNotFound());

    verify(userWalletsService).getUserWalletsByUserId("unknown");
  }

  @Test
  void createUserWalletsShouldReturn201WhenSuccess() throws Exception {
    UserWalletsDto dto = new UserWalletsDto("user1", List.of(new WalletDto("XNO", "100")));
    UserWalletsEntity created = new UserWalletsEntity(dto);
    created.setId("new-id");
    when(userWalletsService.createUserWallets(any(UserWalletsDto.class)))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post("/userWallets")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());

    verify(userWalletsService).createUserWallets(any(UserWalletsDto.class));
  }

  @Test
  void createUserWalletsShouldReturn400WhenUserIdAlreadyExists() throws Exception {
    UserWalletsDto dto = new UserWalletsDto("user1", List.of(new WalletDto("XNO", "100")));
    when(userWalletsService.createUserWallets(any(UserWalletsDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post("/userWallets")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest());

    verify(userWalletsService).createUserWallets(any(UserWalletsDto.class));
  }

  @Test
  void updateUserWalletShouldReturn202WhenSuccess() throws Exception {
    UserWalletsDto dto = new UserWalletsDto("user1", List.of(new WalletDto("XNO", "200")));
    UserWalletsEntity updated = new UserWalletsEntity(dto);
    updated.setId("id-1");
    when(userWalletsService.updateUserWallets(eq("id-1"), any(UserWalletsDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put("/userWallets/id-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(userWalletsService).updateUserWallets(eq("id-1"), any(UserWalletsDto.class));
  }

  @Test
  void updateUserWalletShouldReturn404WhenNotFound() throws Exception {
    UserWalletsDto dto = new UserWalletsDto("user1", List.of(new WalletDto("XNO", "200")));
    when(userWalletsService.updateUserWallets(eq("missing"), any(UserWalletsDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put("/userWallets/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound());

    verify(userWalletsService).updateUserWallets(eq("missing"), any(UserWalletsDto.class));
  }

  @Test
  void deleteWalletShouldReturn204WhenSuccess() throws Exception {
    UserWalletsEntity entity = new UserWalletsEntity("user1");
    when(userWalletsService.deleteUserWallets("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete("/userWallets/id-1")).andExpect(status().isNoContent());

    verify(userWalletsService).deleteUserWallets("id-1");
  }

  @Test
  void deleteWalletShouldReturn404WhenNotFound() throws Exception {
    when(userWalletsService.deleteUserWallets("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete("/userWallets/missing"))
      .andExpect(status().isNotFound());

    verify(userWalletsService).deleteUserWallets("missing");
  }
}
