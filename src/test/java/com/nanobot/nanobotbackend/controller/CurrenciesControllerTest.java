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
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.service.CurrenciesService;
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
class CurrenciesControllerTest {

  private static final String BASE_PATH = "/currencies";

  private MockMvc mockMvc;

  @Mock
  private CurrenciesService currenciesService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders
        .standaloneSetup(new CurrenciesController(currenciesService))
        .build();
  }

  @Test
  void createCurrencyShouldReturnCreatedWhenSuccess() throws Exception {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker("NANO");
    dto.setName("Nano");
    CurrencyEntity created = new CurrencyEntity();
    created.setId("c1");
    created.setTicker("NANO");
    when(currenciesService.createCurrency(any(CurrencyDto.class)))
      .thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("c1"))
      .andExpect(jsonPath("$.ticker").value("NANO"));

    verify(currenciesService).createCurrency(any(CurrencyDto.class));
  }

  @Test
  void createCurrencyShouldReturnBadRequestWhenDuplicate() throws Exception {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker("NANO");
    when(currenciesService.createCurrency(any(CurrencyDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Currency ticker already exists!"));

    verify(currenciesService).createCurrency(any(CurrencyDto.class));
  }

  @Test
  void createCurrencyShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(currenciesService, never()).createCurrency(any());
  }

  @Test
  void getCurrenciesShouldReturnOkAndDelegateToService() throws Exception {
    List<CurrencyEntity> currencies = List.of(new CurrencyEntity());
    when(currenciesService.getCurrencies(null)).thenReturn(currencies);

    mockMvc.perform(get(BASE_PATH)).andExpect(status().isOk());

    verify(currenciesService).getCurrencies(null);
  }

  @Test
  void getCurrenciesShouldPassTickerParamWhenProvided() throws Exception {
    List<CurrencyEntity> currencies = List.of(new CurrencyEntity());
    when(currenciesService.getCurrencies("NANO")).thenReturn(currencies);

    mockMvc
      .perform(get(BASE_PATH).param("ticker", "NANO"))
      .andExpect(status().isOk());

    verify(currenciesService).getCurrencies("NANO");
  }

  @Test
  void getCurrencyByIdShouldReturnOkWhenFound() throws Exception {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setId("c1");
    entity.setTicker("NANO");
    when(currenciesService.getCurrencyById("c1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/c1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("c1"))
      .andExpect(jsonPath("$.ticker").value("NANO"));

    verify(currenciesService).getCurrencyById("c1");
  }

  @Test
  void getCurrencyByIdShouldReturnNotFoundWhenEmpty() throws Exception {
    when(currenciesService.getCurrencyById("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Currency ID not found"));

    verify(currenciesService).getCurrencyById("missing");
  }

  @Test
  void getCurrencyByTickerShouldReturnOkWhenFound() throws Exception {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setTicker("NANO");
    when(currenciesService.getCurrencyByTicker("NANO"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/ticker/NANO"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.ticker").value("NANO"));

    verify(currenciesService).getCurrencyByTicker("NANO");
  }

  @Test
  void getCurrencyByTickerShouldReturnNotFoundWhenEmpty() throws Exception {
    when(currenciesService.getCurrencyByTicker("MISSING"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/ticker/MISSING"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("Currency with specified ticker not found")
      );

    verify(currenciesService).getCurrencyByTicker("MISSING");
  }

  @Test
  void updateCurrencyShouldReturnAcceptedWhenSuccess() throws Exception {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker("NANO");
    dto.setName("Nano Updated");
    CurrencyEntity updated = new CurrencyEntity();
    updated.setId("c1");
    updated.setTicker("NANO");
    when(currenciesService.updateCurrency(eq("c1"), any(CurrencyDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(BASE_PATH + "/c1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.id").value("c1"));

    verify(currenciesService).updateCurrency(eq("c1"), any(CurrencyDto.class));
  }

  @Test
  void updateCurrencyShouldReturnNotFoundWhenEmpty() throws Exception {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker("NANO");
    when(currenciesService.updateCurrency(eq("missing"), any(CurrencyDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Currency ID not found"));

    verify(currenciesService).updateCurrency(eq("missing"), any(CurrencyDto.class));
  }

  @Test
  void deleteCurrencyShouldReturnNoContentWhenSuccess() throws Exception {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setId("c1");
    when(currenciesService.deleteCurrency("c1"))
      .thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/c1")).andExpect(status().isNoContent());

    verify(currenciesService).deleteCurrency("c1");
  }

  @Test
  void deleteCurrencyShouldReturnNotFoundWhenEmpty() throws Exception {
    when(currenciesService.deleteCurrency("missing"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Currency ID not found"));

    verify(currenciesService).deleteCurrency("missing");
  }
}
