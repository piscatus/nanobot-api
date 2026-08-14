package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.AliasesResponseDto;
import com.nanobot.nanobotbackend.service.AliasesServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AliasesRequestsControllerTest {

  private static final String PATH = "/requests/aliases";

  private MockMvc mockMvc;

  @Mock
  private AliasesServices aliasesServices;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new AliasesRequestsController(aliasesServices))
        .build();
  }

  @Test
  void aliasesShouldReturnOkAndDelegateToService() throws Exception {
    AliasesResponseDto response = new AliasesResponseDto();
    when(aliasesServices.aliases(any())).thenReturn(response);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(aliasesServices).aliases(any());
  }

  @Test
  void aliasesShouldPassRequestBodyAndReturnResponseBody() throws Exception {
    String requestBody = "{\"guildId\":\"g1\",\"userId\":\"u1\",\"channelId\":\"c1\"}";
    AliasesResponseDto response = new AliasesResponseDto();
    response.setAliases(java.util.List.of());
    when(aliasesServices.aliases(any())).thenReturn(response);

    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.aliases").isArray());

    verify(aliasesServices).aliases(any());
  }

  @Test
  void aliasesShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(aliasesServices, never()).aliases(any());
  }

  @Test
  void aliasesShouldReturnOkWithErrorMessageWhenServiceReturnsError() throws Exception {
    AliasesResponseDto errorResponse = new AliasesResponseDto("Aliases unavailable");
    when(aliasesServices.aliases(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Aliases unavailable"));

    verify(aliasesServices).aliases(any());
  }
}
