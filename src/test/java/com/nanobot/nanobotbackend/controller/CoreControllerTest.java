package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.BaseResponseDto;
import com.nanobot.nanobotbackend.dto.BonusesResponseDto;
import com.nanobot.nanobotbackend.service.CoreServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class CoreControllerTest {

  private static final String BASE_PATH = "/requests";
  private static final String BONUSES_PATH = BASE_PATH + "/bonuses";

  private MockMvc mockMvc;

  @Mock
  private CoreServices coreServices;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new CoreController(coreServices)).build();
  }

  @Test
  void bonusesShouldReturnOkAndDelegateToService() throws Exception {
    BonusesResponseDto response = new BonusesResponseDto();
    when(coreServices.bonuses(any())).thenReturn(response);

    mockMvc
      .perform(post(BONUSES_PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(coreServices).bonuses(any());
  }

  @Test
  void bonusesShouldPassRequestBodyToService() throws Exception {
    BonusesResponseDto response = new BonusesResponseDto();
    when(coreServices.bonuses(any())).thenReturn(response);

    String requestBody = "{\"guildId\":\"g1\",\"userId\":\"u1\",\"channelId\":\"c1\"}";
    mockMvc
      .perform(
        post(BONUSES_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk());

    verify(coreServices).bonuses(any());
  }

  @Test
  void cuteShouldReturnOkAndDelegateToService() throws Exception {
    BaseResponseDto response = new BaseResponseDto();
    when(coreServices.cute(any())).thenReturn(response);

    mockMvc
      .perform(post(BASE_PATH + "/cute").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(coreServices).cute(any());
  }

  @Test
  void rolesShouldReturnOkAndDelegateToService() throws Exception {
    BaseResponseDto response = new BaseResponseDto();
    when(coreServices.roles(any())).thenReturn(response);

    mockMvc
      .perform(post(BASE_PATH + "/roles").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(coreServices).roles(any());
  }

  @Test
  void rulesShouldReturnOkAndDelegateToService() throws Exception {
    BaseResponseDto response = new BaseResponseDto();
    when(coreServices.rules(any())).thenReturn(response);

    mockMvc
      .perform(post(BASE_PATH + "/rules").contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(coreServices).rules(any());
  }

  @Test
  void bonusesShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BONUSES_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(coreServices, never()).bonuses(any());
  }

  @Test
  void bonusesShouldReturnOkWithErrorMessageWhenServiceReturnsError() throws Exception {
    BonusesResponseDto errorResponse = new BonusesResponseDto("Bonuses unavailable");
    when(coreServices.bonuses(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(BONUSES_PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Bonuses unavailable"));

    verify(coreServices).bonuses(any());
  }
}
