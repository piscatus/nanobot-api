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
import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.UserItemsDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.service.UserItemsService;
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
class UserItemsControllerTest {

  private MockMvc mockMvc;

  @Mock
  private UserItemsService userItemsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new UserItemsController(userItemsService))
        .build();
  }

  @Test
  void getItemsShouldReturnOkWithList() throws Exception {
    UserItemsEntity entity = new UserItemsEntity("user1");
    entity.setItems(List.of(new ItemDto("SHRIMP", 5)));
    when(userItemsService.getUsersItems(null)).thenReturn(List.of(entity));

    mockMvc
      .perform(get("/userItems"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].userId").value("user1"));

    verify(userItemsService).getUsersItems(null);
  }

  @Test
  void getItemsWithUserIdParamShouldDelegate() throws Exception {
    when(userItemsService.getUsersItems("user123")).thenReturn(Collections.emptyList());

    mockMvc.perform(get("/userItems").param("userId", "user123")).andExpect(status().isOk());

    verify(userItemsService).getUsersItems("user123");
  }

  @Test
  void getItemByIdShouldReturnOkWhenFound() throws Exception {
    UserItemsEntity entity = new UserItemsEntity("user1");
    entity.setId("id-1");
    when(userItemsService.getUserItemsById("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get("/userItems/id-1")).andExpect(status().isOk());

    verify(userItemsService).getUserItemsById("id-1");
  }

  @Test
  void getItemByIdShouldReturn404WhenNotFound() throws Exception {
    when(userItemsService.getUserItemsById("missing")).thenReturn(Optional.empty());

    mockMvc.perform(get("/userItems/missing")).andExpect(status().isNotFound());

    verify(userItemsService).getUserItemsById("missing");
  }

  @Test
  void getUserItemsByUserIdShouldReturnOkWhenFound() throws Exception {
    UserItemsEntity entity = new UserItemsEntity("user1");
    when(userItemsService.getUserItemsByUserId("user1")).thenReturn(Optional.of(entity));

    mockMvc.perform(get("/userItems/userId/user1")).andExpect(status().isOk());

    verify(userItemsService).getUserItemsByUserId("user1");
  }

  @Test
  void getUserItemsByUserIdShouldReturn404WhenNotFound() throws Exception {
    when(userItemsService.getUserItemsByUserId("unknown")).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/userItems/userId/unknown"))
      .andExpect(status().isNotFound());

    verify(userItemsService).getUserItemsByUserId("unknown");
  }

  @Test
  void createItemShouldReturn201WhenSuccess() throws Exception {
    UserItemsDto dto = new UserItemsDto("user1", List.of(new ItemDto("SHRIMP", 10)));
    UserItemsEntity created = new UserItemsEntity(dto);
    created.setId("new-id");
    when(userItemsService.createUserItems(any(UserItemsDto.class)))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post("/userItems")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());

    verify(userItemsService).createUserItems(any(UserItemsDto.class));
  }

  @Test
  void createItemShouldReturn400WhenUserIdAlreadyExists() throws Exception {
    UserItemsDto dto = new UserItemsDto("user1", List.of(new ItemDto("SHRIMP", 10)));
    when(userItemsService.createUserItems(any(UserItemsDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post("/userItems")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest());

    verify(userItemsService).createUserItems(any(UserItemsDto.class));
  }

  @Test
  void updateItemShouldReturn202WhenSuccess() throws Exception {
    UserItemsDto dto = new UserItemsDto("user1", List.of(new ItemDto("SHRIMP", 20)));
    UserItemsEntity updated = new UserItemsEntity(dto);
    updated.setId("id-1");
    when(userItemsService.updateUserItems(eq("id-1"), any(UserItemsDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put("/userItems/id-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(userItemsService).updateUserItems(eq("id-1"), any(UserItemsDto.class));
  }

  @Test
  void updateItemShouldReturn404WhenNotFound() throws Exception {
    UserItemsDto dto = new UserItemsDto("user1", List.of(new ItemDto("SHRIMP", 20)));
    when(userItemsService.updateUserItems(eq("missing"), any(UserItemsDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put("/userItems/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound());

    verify(userItemsService).updateUserItems(eq("missing"), any(UserItemsDto.class));
  }

  @Test
  void deleteUserItemsShouldReturn204WhenSuccess() throws Exception {
    UserItemsEntity entity = new UserItemsEntity("user1");
    when(userItemsService.deleteUserItems("id-1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete("/userItems/id-1")).andExpect(status().isNoContent());

    verify(userItemsService).deleteUserItems("id-1");
  }

  @Test
  void deleteUserItemsShouldReturn404WhenNotFound() throws Exception {
    when(userItemsService.deleteUserItems("missing")).thenReturn(Optional.empty());

    mockMvc.perform(delete("/userItems/missing")).andExpect(status().isNotFound());

    verify(userItemsService).deleteUserItems("missing");
  }
}
