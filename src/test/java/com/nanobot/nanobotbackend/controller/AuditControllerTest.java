package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.AuditResponseDto;
import com.nanobot.nanobotbackend.service.AuditServices;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class AuditControllerTest {

  private static final String PATH = "/requests/audit";

  private MockMvc mockMvc;

  @Mock
  private AuditServices auditServices;

  @BeforeEach
  void setUp() {
    mockMvc = MockMvcBuilders.standaloneSetup(new AuditController(auditServices)).build();
  }

  @Test
  void auditShouldReturnOkAndDelegateToService() throws Exception {
    AuditResponseDto response = new AuditResponseDto();
    when(auditServices.audit(any())).thenReturn(response);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk());

    verify(auditServices).audit(any());
  }

  @Test
  void auditShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("invalid json"))
      .andExpect(status().isBadRequest());

    verify(auditServices, never()).audit(any());
  }

  @Test
  void auditShouldReturnOkWithErrorMessageWhenServiceReturnsError() throws Exception {
    AuditResponseDto errorResponse = new AuditResponseDto("Audit failed");
    when(auditServices.audit(any())).thenReturn(errorResponse);

    mockMvc
      .perform(post(PATH).contentType(MediaType.APPLICATION_JSON).content("{}"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("Audit failed"));

    verify(auditServices).audit(any());
  }

  @Test
  void auditShouldPassRequestBodyToService() throws Exception {
    AuditResponseDto response = new AuditResponseDto();
    when(auditServices.audit(any())).thenReturn(response);

    String requestBody = "{\"guildId\":\"g1\",\"userId\":\"u1\",\"channelId\":\"c1\"}";
    mockMvc
      .perform(
        post(PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(requestBody)
      )
      .andExpect(status().isOk());

    verify(auditServices).audit(argThat(req ->
      "g1".equals(req.getGuildId())
        && "u1".equals(req.getUserId())
        && "c1".equals(req.getChannelId())
    ));
  }
}
