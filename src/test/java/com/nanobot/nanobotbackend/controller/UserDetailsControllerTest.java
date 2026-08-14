package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.service.UserDetailsService;
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
class UserDetailsControllerTest {

  private static final String USER_DETAILS_PATH = "/userDetails";
  private static final String MISSING_ID = "missing";
  private static final String USER_1 = "user1";
  private static final String JSON_MESSAGE_PATH = "$.message";

  private MockMvc mockMvc;

  @Mock
  private UserDetailsService userDetailsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new UserDetailsController(userDetailsService))
        .build();
  }

  @Test
  void testGetUsers() throws Exception {
    when(userDetailsService.getUsersDetails(USER_1))
      .thenReturn(Collections.emptyList());

    mockMvc
      .perform(get(USER_DETAILS_PATH).param("userId", USER_1))
      .andExpect(status().isOk());

    verify(userDetailsService).getUsersDetails(USER_1);
  }

  @Test
  void testCreateUserSuccess() throws Exception {
    UserDetailsDto dto = new UserDetailsDto(
      USER_1,
      "sub1",
      StatusDto.ACTIVE,
      "seed1"
    );
    when(userDetailsService.createUserDetails(any())).thenReturn(
      Optional.of(new UserDetailsEntity(dto))
    );

    mockMvc
      .perform(
        post(USER_DETAILS_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated());

    verify(userDetailsService).createUserDetails(any());
  }

  @Test
  void testCreateUserAlreadyExists() throws Exception {
    UserDetailsDto dto = new UserDetailsDto();
    dto.setUserId("existingUser");

    when(userDetailsService.createUserDetails(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(USER_DETAILS_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("User Details userId already exists"));

    verify(userDetailsService).createUserDetails(any());
  }

  @Test
  void testGetUserDetailsByIdNotFound() throws Exception {
    when(userDetailsService.getUserDetailsById(MISSING_ID)).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(get(USER_DETAILS_PATH + "/" + MISSING_ID))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Details ID not found"));

    verify(userDetailsService).getUserDetailsById(MISSING_ID);
  }

  @Test
  void testGetUserDetailsByIdFound() throws Exception {
    UserDetailsEntity user = new UserDetailsEntity();
    user.setUserId("u1");
    when(userDetailsService.getUserDetailsById("id1")).thenReturn(
      Optional.of(user)
    );

    mockMvc.perform(get(USER_DETAILS_PATH + "/id1")).andExpect(status().isOk());

    verify(userDetailsService).getUserDetailsById("id1");
  }

  @Test
  void testGetUserDetailsByUserIdNotFound() throws Exception {
    when(userDetailsService.getUserDetailsByUserId(MISSING_ID)).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(get(USER_DETAILS_PATH + "/userId/" + MISSING_ID))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("User with specified userId not found"));

    verify(userDetailsService).getUserDetailsByUserId(MISSING_ID);
  }

  @Test
  void testGetOrCreateUserDetails() throws Exception {
    UserDetailsEntity entity = new UserDetailsEntity();
    entity.setUserId("abc");
    when(userDetailsService.getOrCreateUserDetails("abc")).thenReturn(entity);

    mockMvc
      .perform(get("/userDetails/getOrCreate/abc"))
      .andExpect(status().isOk());

    verify(userDetailsService).getOrCreateUserDetails("abc");
  }

  @Test
  void testUpdateUserDetailsFound() throws Exception {
    UserDetailsDto dto = new UserDetailsDto(
      "updatedUser",
      "sub1",
      StatusDto.ACTIVE,
      "seed2"
    );
    when(userDetailsService.updateUserDetails(any(), any())).thenReturn(
      Optional.of(new UserDetailsEntity(dto))
    );

    mockMvc
      .perform(
        put(USER_DETAILS_PATH + "/id1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(userDetailsService).updateUserDetails(any(), any());
  }

  @Test
  void testUpdateUserDetailsNotFound() throws Exception {
    UserDetailsDto dto = new UserDetailsDto();
    when(userDetailsService.updateUserDetails(any(), any())).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(
        put(USER_DETAILS_PATH + "/" + MISSING_ID)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("Details ID not found"));

    verify(userDetailsService).updateUserDetails(any(), any());
  }

  @Test
  void testDeleteUserDetailsNotFound() throws Exception {
    when(userDetailsService.deleteUserDetails(MISSING_ID)).thenReturn(
      Optional.empty()
    );

    mockMvc
      .perform(delete(USER_DETAILS_PATH + "/" + MISSING_ID))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath(JSON_MESSAGE_PATH).value("User Details ID not found"));

    verify(userDetailsService).deleteUserDetails(MISSING_ID);
  }

  @Test
  void testDeleteUserDetailsSuccess() throws Exception {
    when(userDetailsService.deleteUserDetails("id1")).thenReturn(
      Optional.of(new UserDetailsEntity())
    );

    mockMvc.perform(delete(USER_DETAILS_PATH + "/id1")).andExpect(status().isNoContent());

    verify(userDetailsService).deleteUserDetails("id1");
  }

  @Test
  void testGetActiveUsers() throws Exception {
    when(userDetailsService.getActiveUsersDetails()).thenReturn(List.of());

    mockMvc.perform(get("/userDetails/active")).andExpect(status().isOk());
    verify(userDetailsService).getActiveUsersDetails();
  }

  @Test
  void testGetLockedUsers() throws Exception {
    when(userDetailsService.getLockedUsersDetails()).thenReturn(List.of());

    mockMvc.perform(get("/userDetails/locked")).andExpect(status().isOk());
    verify(userDetailsService).getLockedUsersDetails();
  }

  @Test
  void testGetBannedUsers() throws Exception {
    when(userDetailsService.getBannedUsersDetails()).thenReturn(List.of());

    mockMvc.perform(get("/userDetails/banned")).andExpect(status().isOk());
    verify(userDetailsService).getBannedUsersDetails();
  }
}
