package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.ArgumentMatchers.isNull;
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
import com.nanobot.nanobotbackend.dto.VerificationDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.entity.VerificationEntity;
import com.nanobot.nanobotbackend.service.EmojisService;
import com.nanobot.nanobotbackend.service.VerificationsService;
import java.util.ArrayList;
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
class VerificationsControllerTest {

  private static final String PATH = "/verifications";
  private static final String NOT_FOUND_MESSAGE = "Verification ID not found";

  private MockMvc mockMvc;

  @Mock
  private VerificationsService verificationsService;

  @Mock
  private EmojisService emojisService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new VerificationsController(verificationsService, emojisService))
        .build();
  }

  @Test
  void createVerificationShouldReturnCreatedWhenNew() throws Exception {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("u1");
    when(verificationsService.createVerification(any())).thenReturn(Optional.of(new VerificationEntity()));

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content(objectMapper.writeValueAsString(dto)))
      .andExpect(status().isCreated());
  }

  @Test
  void createVerificationShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(verificationsService, never()).createVerification(any());
  }

  @Test
  void createVerificationShouldReturnBadRequestWhenDuplicate() throws Exception {
    when(verificationsService.createVerification(any())).thenReturn(Optional.empty());

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Verification userId already exists"));
  }

  @Test
  void checkShouldReturnOkWhenVerificationStillValid() throws Exception {
    VerificationEntity verification = new VerificationEntity();
    verification.setTimestamp(new Date());
    when(verificationsService.getVerificationByUserId("u1")).thenReturn(Optional.of(verification));

    mockMvc.perform(get(PATH + "/userId/u1/check")).andExpect(status().isOk());

    verify(emojisService, never()).getEmojis(isNull(), isNull());
  }

  @Test
  void checkShouldReturnUnauthorizedAndSortedEmojisWhenVerificationMissing() throws Exception {
    when(verificationsService.getVerificationByUserId("u1")).thenReturn(Optional.empty());
    EmojiEntity b = new EmojiEntity();
    b.setCategory("b");
    EmojiEntity a = new EmojiEntity();
    a.setCategory("a");
    when(emojisService.getEmojis(isNull(), isNull())).thenReturn(new ArrayList<>(List.of(b, a)));

    mockMvc
      .perform(get(PATH + "/userId/u1/check"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$[0].category").value("a"))
      .andExpect(jsonPath("$[1].category").value("b"));

    verify(verificationsService).getVerificationByUserId("u1");
    verify(emojisService).getEmojis(isNull(), isNull());
  }

  @Test
  void checkShouldReturnUnauthorizedAndSortedEmojisWhenVerificationExpired() throws Exception {
    VerificationEntity verification = new VerificationEntity();
    verification.setTimestamp(new Date(System.currentTimeMillis() - (13L * 60 * 60 * 1000)));
    EmojiEntity b = new EmojiEntity();
    b.setCategory("b");
    EmojiEntity a = new EmojiEntity();
    a.setCategory("a");

    when(verificationsService.getVerificationByUserId("u1")).thenReturn(Optional.of(verification));
    when(emojisService.getEmojis(isNull(), isNull())).thenReturn(new ArrayList<>(List.of(b, a)));

    mockMvc
      .perform(get(PATH + "/userId/u1/check"))
      .andExpect(status().isUnauthorized())
      .andExpect(jsonPath("$[0].category").value("a"))
      .andExpect(jsonPath("$[1].category").value("b"));
  }

  @Test
  void updateVerificationShouldReturnNotFoundWhenMissing() throws Exception {
    when(verificationsService.updateVerification(any(), any())).thenReturn(Optional.empty());

    mockMvc
      .perform(put(PATH + "/missing").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Verification ID not found"));
  }

  @Test
  void deleteVerificationShouldReturnNoContentWhenFound() throws Exception {
    when(verificationsService.deleteVerification("id1")).thenReturn(Optional.of(new VerificationEntity()));

    mockMvc.perform(delete(PATH + "/id1")).andExpect(status().isNoContent());
  }

  @Test
  void deleteVerificationShouldReturnNotFoundWhenMissing() throws Exception {
    when(verificationsService.deleteVerification("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
  }

  @Test
  void getVerificationsShouldReturnOkAndDelegateToService() throws Exception {
    List<VerificationEntity> verifications = List.of(new VerificationEntity());
    when(verificationsService.getVerifications(null)).thenReturn(verifications);

    mockMvc.perform(get(PATH)).andExpect(status().isOk());

    verify(verificationsService).getVerifications(isNull());
  }

  @Test
  void getVerificationByIdShouldReturnOkWhenFound() throws Exception {
    VerificationEntity entity = new VerificationEntity();
    entity.setId("v1");
    when(verificationsService.getVerificationById("v1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/v1"))
      .andExpect(status().isOk());

    verify(verificationsService).getVerificationById("v1");
  }

  @Test
  void getVerificationByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(verificationsService.getVerificationById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value(NOT_FOUND_MESSAGE));
  }

  @Test
  void getVerificationByUserIdShouldReturnOkWhenFound() throws Exception {
    VerificationEntity entity = new VerificationEntity();
    entity.setUserId("u1");
    when(verificationsService.getVerificationByUserId("u1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(PATH + "/userId/u1"))
      .andExpect(status().isOk());

    verify(verificationsService).getVerificationByUserId("u1");
  }

  @Test
  void getVerificationByUserIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(verificationsService.getVerificationByUserId("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(PATH + "/userId/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Verification with specified userId not found"));
  }

  @Test
  void updateVerificationShouldReturnAcceptedWhenFound() throws Exception {
    VerificationDto dto = new VerificationDto();
    dto.setUserId("u1");
    VerificationEntity updated = new VerificationEntity();
    updated.setId("v1");
    when(verificationsService.updateVerification(eq("v1"), any())).thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(PATH + "/v1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(verificationsService).updateVerification(eq("v1"), any());
  }
}
