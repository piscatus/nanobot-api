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
import com.nanobot.nanobotbackend.dto.QueueDto;
import com.nanobot.nanobotbackend.entity.QueueEntity;
import com.nanobot.nanobotbackend.service.QueuesService;
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
class QueuesControllerTest {

  private MockMvc mockMvc;
  private ObjectMapper objectMapper;

  @Mock
  private QueuesService queuesService;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc = MockMvcBuilders.standaloneSetup(new QueuesController(queuesService)).build();
  }

  @Test
  void createQueueReturnsCreated() throws Exception {
    QueueDto dto = new QueueDto("u1", null, null, null, null, "100", "XNO", false, null, null, null);
    QueueEntity created = new QueueEntity();
    created.setId("q-1");
    created.setUserId("u1");

    when(queuesService.createQueue(any(QueueDto.class))).thenReturn(created);

    mockMvc
      .perform(
        post("/queues")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("q-1"));

    verify(queuesService).createQueue(any(QueueDto.class));
  }

  @Test
  void getQueuesReturnsOkWithList() throws Exception {
    QueueEntity e1 = new QueueEntity();
    e1.setId("q-1");
    when(queuesService.getQueues()).thenReturn(List.of(e1));

    mockMvc
      .perform(get("/queues"))
      .andExpect(status().isOk());

    verify(queuesService).getQueues();
  }

  @Test
  void getQueueByIdReturnsOkWhenFound() throws Exception {
    QueueEntity entity = new QueueEntity();
    entity.setId("q-1");
    entity.setUserId("u1");
    when(queuesService.getQueueById("q-1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get("/queues/q-1"))
      .andExpect(status().isOk());

    verify(queuesService).getQueueById("q-1");
  }

  @Test
  void getQueueByIdReturnsNotFoundWhenMissing() throws Exception {
    when(queuesService.getQueueById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get("/queues/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Queue ID not found"));

    verify(queuesService).getQueueById("missing");
  }

  @Test
  void updateQueueReturnsAcceptedWhenSuccess() throws Exception {
    QueueDto dto = new QueueDto("u1", null, null, null, null, "100", "XNO", false, null, null, null);
    QueueEntity updated = new QueueEntity();
    updated.setId("q-1");
    when(queuesService.updateQueue(eq("q-1"), any(QueueDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put("/queues/q-1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted());

    verify(queuesService).updateQueue(eq("q-1"), any(QueueDto.class));
  }

  @Test
  void updateQueueReturnsNotFoundWhenMissing() throws Exception {
    QueueDto dto = new QueueDto("u1", null, null, null, null, "100", "XNO", false, null, null, null);
    when(queuesService.updateQueue(eq("missing"), any(QueueDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put("/queues/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Queue ID not found"));

    verify(queuesService).updateQueue(eq("missing"), any(QueueDto.class));
  }

  @Test
  void deleteQueueReturnsNoContentWhenSuccess() throws Exception {
    QueueEntity deleted = new QueueEntity();
    deleted.setId("q-1");
    when(queuesService.deleteQueue("q-1")).thenReturn(Optional.of(deleted));

    mockMvc
      .perform(delete("/queues/q-1"))
      .andExpect(status().isNoContent());

    verify(queuesService).deleteQueue("q-1");
  }

  @Test
  void deleteQueueReturnsNotFoundWhenMissing() throws Exception {
    when(queuesService.deleteQueue("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete("/queues/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Queue id not found"));

    verify(queuesService).deleteQueue("missing");
  }
}
