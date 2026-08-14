package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
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
import com.nanobot.nanobotbackend.dto.TransactionDto;
import com.nanobot.nanobotbackend.entity.TransactionEntity;
import com.nanobot.nanobotbackend.service.TransactionsService;
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
class TransactionsControllerTest {

  private static final String BASE_PATH = "/transactions";

  private MockMvc mockMvc;

  @Mock
  private TransactionsService transactionsService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new TransactionsController(transactionsService))
        .build();
  }

  @Test
  void createTransactionShouldReturnCreatedWhenSuccess() throws Exception {
    TransactionDto dto = new TransactionDto();
    dto.setGuildId("g1");
    TransactionEntity created = new TransactionEntity();
    created.setId("t1");
    created.setGuildId("g1");
    when(transactionsService.createTransaction(any(TransactionDto.class)))
      .thenReturn(created);

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("t1"));

    verify(transactionsService).createTransaction(any(TransactionDto.class));
  }

  @Test
  void createTransactionShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(transactionsService, never()).createTransaction(any());
  }

  @Test
  void getTransactionByIdShouldReturnOkWhenFound() throws Exception {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("t1");
    when(transactionsService.getTransactionById("t1"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/t1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("t1"));

    verify(transactionsService).getTransactionById("t1");
  }

  @Test
  void getTransactionByIdShouldReturnNotFoundWhenEmpty() throws Exception {
    when(transactionsService.getTransactionById("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Transaction ID not found"));

    verify(transactionsService).getTransactionById("missing");
  }

  @Test
  void getTransactionsShouldReturnOkAndDelegateToService() throws Exception {
    List<TransactionEntity> transactions = List.of(new TransactionEntity());
    when(transactionsService.getTransactions(null)).thenReturn(transactions);

    mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk());

    verify(transactionsService).getTransactions(null);
  }

  @Test
  void getTransactionsShouldPassUserIdParam() throws Exception {
    when(transactionsService.getTransactions("user-1")).thenReturn(List.of());

    mockMvc
      .perform(get(BASE_PATH).param("userId", "user-1"))
      .andExpect(status().isOk());

    verify(transactionsService).getTransactions("user-1");
  }

  @Test
  void updateTransactionShouldReturnAcceptedWhenSuccess() throws Exception {
    TransactionDto dto = new TransactionDto();
    dto.setGuildId("g1");
    TransactionEntity updated = new TransactionEntity();
    updated.setId("t1");
    when(transactionsService.updateTransaction(eq("t1"), any(TransactionDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(BASE_PATH + "/t1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.id").value("t1"));

    verify(transactionsService).updateTransaction(eq("t1"), any(TransactionDto.class));
  }

  @Test
  void updateTransactionShouldReturnNotFoundWhenEmpty() throws Exception {
    TransactionDto dto = new TransactionDto();
    when(transactionsService.updateTransaction(eq("missing"), any(TransactionDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Transaction ID not found"));

    verify(transactionsService).updateTransaction(eq("missing"), any(TransactionDto.class));
  }

  @Test
  void deleteTransactionShouldReturnNoContentWhenSuccess() throws Exception {
    TransactionEntity entity = new TransactionEntity();
    entity.setId("t1");
    when(transactionsService.deleteTransaction("t1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/t1")).andExpect(status().isNoContent());

    verify(transactionsService).deleteTransaction("t1");
  }

  @Test
  void deleteTransactionShouldReturnNotFoundWhenEmpty() throws Exception {
    when(transactionsService.deleteTransaction("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Transaction id not found"));

    verify(transactionsService).deleteTransaction("missing");
  }
}
