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
import com.nanobot.nanobotbackend.dto.MessageDto;
import com.nanobot.nanobotbackend.entity.MessageEntity;
import com.nanobot.nanobotbackend.service.MessagesService;
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
class MessagesControllerTest {

  private static final String BASE_PATH = "/messages";

  private MockMvc mockMvc;

  @Mock
  private MessagesService messagesService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new MessagesController(messagesService)).build();
  }

  @Test
  void createMessageShouldReturnCreatedAndDelegateToService() throws Exception {
    MessageEntity created = new MessageEntity();
    created.setId("msg-1");
    created.setContent("Hello");
    created.setChannelId("ch1");
    created.setGuildId("g1");
    when(messagesService.createMessage(any(MessageDto.class))).thenReturn(created);

    MessageDto dto = new MessageDto(
      "user1",
      "g1",
      "ch1",
      null,
      null,
      null,
      "Hello",
      new Date(),
      null,
      null,
      null
    );

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("msg-1"))
      .andExpect(jsonPath("$.content").value("Hello"));

    verify(messagesService).createMessage(any(MessageDto.class));
  }

  @Test
  void getMessagesShouldReturnOkAndDelegateToService() throws Exception {
    MessageEntity msg = new MessageEntity();
    msg.setId("msg-1");
    msg.setContent("Test");
    when(messagesService.getMessages()).thenReturn(List.of(msg));

    mockMvc
      .perform(get(BASE_PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].id").value("msg-1"))
      .andExpect(jsonPath("$[0].content").value("Test"));

    verify(messagesService).getMessages();
  }

  @Test
  void getMessageByIdShouldReturnOkWhenFound() throws Exception {
    MessageEntity msg = new MessageEntity();
    msg.setId("msg-1");
    msg.setContent("Found");
    when(messagesService.getMessageById("msg-1")).thenReturn(Optional.of(msg));

    mockMvc
      .perform(get(BASE_PATH + "/msg-1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("msg-1"))
      .andExpect(jsonPath("$.content").value("Found"));

    verify(messagesService).getMessageById("msg-1");
  }

  @Test
  void getMessageByIdShouldReturnNotFoundWhenMissing() throws Exception {
    when(messagesService.getMessageById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Message ID not found"));

    verify(messagesService).getMessageById("missing");
  }

  @Test
  void updateMessageShouldReturnAcceptedWhenFound() throws Exception {
    MessageEntity existing = new MessageEntity();
    existing.setId("msg-1");
    existing.setContent("Old");
    MessageEntity updated = new MessageEntity();
    updated.setId("msg-1");
    updated.setContent("Updated");
    when(messagesService.updateMessage(eq("msg-1"), any(MessageDto.class)))
      .thenReturn(Optional.of(updated));

    MessageDto dto = new MessageDto(
      null,
      null,
      null,
      null,
      null,
      null,
      "Updated",
      null,
      null,
      null,
      null
    );

    mockMvc
      .perform(
        put(BASE_PATH + "/msg-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(messagesService).updateMessage(eq("msg-1"), any(MessageDto.class));
  }

  @Test
  void updateMessageShouldReturnNotFoundWhenMissing() throws Exception {
    when(messagesService.updateMessage(eq("missing"), any(MessageDto.class)))
      .thenReturn(Optional.empty());

    MessageDto dto = new MessageDto(null, null, null, null, null, null, "Updated", null, null, null, null);

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Message ID not found"));

    verify(messagesService).updateMessage(eq("missing"), any(MessageDto.class));
  }

  @Test
  void deleteMessageShouldReturnNoContentWhenFound() throws Exception {
    MessageEntity msg = new MessageEntity();
    msg.setId("msg-1");
    when(messagesService.deleteMessage("msg-1")).thenReturn(Optional.of(msg));

    mockMvc.perform(delete(BASE_PATH + "/msg-1")).andExpect(status().isNoContent());

    verify(messagesService).deleteMessage("msg-1");
  }

  @Test
  void deleteMessageShouldReturnNotFoundWhenMissing() throws Exception {
    when(messagesService.deleteMessage("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Message ID not found"));

    verify(messagesService).deleteMessage("missing");
  }
}
