package com.nanobot.nanobotbackend.controller;

import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.nanobot.nanobotbackend.dto.FishingReminderResponseDto;
import com.nanobot.nanobotbackend.service.FishingReminderService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class FishingReminderControllerTest {

  private static final String PATH = "/reminders";

  private MockMvc mockMvc;

  @Mock
  private FishingReminderService fishingReminderService;

  @BeforeEach
  void setUp() {
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new FishingReminderController(fishingReminderService))
        .build();
  }

  @Test
  void getFishingRemindersShouldReturnOkAndDelegateToService() throws Exception {
    FishingReminderResponseDto response = new FishingReminderResponseDto();
    when(fishingReminderService.getFishingReminder()).thenReturn(response);

    mockMvc.perform(get(PATH)).andExpect(status().isOk());

    verify(fishingReminderService).getFishingReminder();
  }

  @Test
  void getFishingRemindersShouldReturnResponseBodyWhenServiceReturnsData()
    throws Exception {
    FishingReminderResponseDto response = new FishingReminderResponseDto();
    response.setErrorMessage(null);
    when(fishingReminderService.getFishingReminder()).thenReturn(response);

    mockMvc.perform(get(PATH)).andExpect(status().isOk());

    verify(fishingReminderService).getFishingReminder();
  }

  @Test
  void getFishingRemindersShouldReturnErrorWhenServiceReturnsError()
    throws Exception {
    FishingReminderResponseDto errorResponse = new FishingReminderResponseDto();
    errorResponse.setErrorMessage("No reminders");
    when(fishingReminderService.getFishingReminder()).thenReturn(errorResponse);

    mockMvc
      .perform(get(PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.errorMessage").value("No reminders"));

    verify(fishingReminderService).getFishingReminder();
  }
}
