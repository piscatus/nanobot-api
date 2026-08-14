package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.repository.CurrenciesRepository;
import java.math.BigDecimal;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class PriceServiceImplTest {

  @Mock
  private CurrenciesRepository currenciesRepository;

  private PriceServiceImpl priceService;

  @BeforeEach
  void setUp() {
    priceService = new PriceServiceImpl(currenciesRepository);
  }

  @Test
  void processPriceDataShouldUpdateCurrencyEntityForNano() {
    CurrencyEntity nanoEntity = new CurrencyEntity();
    nanoEntity.setId("c1");
    nanoEntity.setTicker("NANO");
    nanoEntity.setValue("0");
    when(currenciesRepository.findByTicker("NANO")).thenReturn(List.of(nanoEntity));
    when(currenciesRepository.save(any(CurrencyEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    String json =
      "[{\"id\":\"nano\",\"symbol\":\"nano\",\"current_price\":1.23}]";
    priceService.processPriceData(json);

    ArgumentCaptor<CurrencyEntity> captor =
      ArgumentCaptor.forClass(CurrencyEntity.class);
    verify(currenciesRepository).findByTicker("NANO");
    verify(currenciesRepository).save(captor.capture());
    assertEquals("1.23", captor.getValue().getValue());
  }

  @Test
  void processPriceDataShouldUpdateCurrencyEntityForBanano() {
    CurrencyEntity banEntity = new CurrencyEntity();
    banEntity.setId("c2");
    banEntity.setTicker("BAN");
    banEntity.setValue("0");
    when(currenciesRepository.findByTicker("BAN")).thenReturn(List.of(banEntity));
    when(currenciesRepository.save(any(CurrencyEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    String json =
      "[{\"id\":\"banano\",\"symbol\":\"ban\",\"current_price\":0.0045}]";
    priceService.processPriceData(json);

    ArgumentCaptor<CurrencyEntity> captor =
      ArgumentCaptor.forClass(CurrencyEntity.class);
    verify(currenciesRepository).findByTicker("BAN");
    verify(currenciesRepository).save(captor.capture());
    assertEquals("0.0045", captor.getValue().getValue());
  }

  @Test
  void processPriceDataShouldHandleInvalidJsonGracefully() {
    priceService.processPriceData("invalid json");
    verify(currenciesRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void updateCurrencyEntityShouldUpdateWhenSingleMatch() {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setId("c1");
    entity.setTicker("NANO");
    entity.setValue("0");
    when(currenciesRepository.findByTicker("NANO")).thenReturn(List.of(entity));
    when(currenciesRepository.save(any(CurrencyEntity.class)))
      .thenAnswer(inv -> inv.getArgument(0));

    priceService.updateCurrencyEntity("nano", new BigDecimal("2.50"));

    ArgumentCaptor<CurrencyEntity> captor =
      ArgumentCaptor.forClass(CurrencyEntity.class);
    verify(currenciesRepository).findByTicker("NANO");
    verify(currenciesRepository).save(captor.capture());
    assertEquals("2.5", captor.getValue().getValue());
  }

  @Test
  void updateCurrencyEntityShouldNotSaveWhenNoMatch() {
    when(currenciesRepository.findByTicker("UNKNOWN")).thenReturn(List.of());

    priceService.updateCurrencyEntity("unknown", new BigDecimal("1.0"));

    verify(currenciesRepository).findByTicker("UNKNOWN");
    verify(currenciesRepository, org.mockito.Mockito.never()).save(any());
  }

  @Test
  void updateCurrencyEntityShouldNotSaveWhenMultipleMatches() {
    CurrencyEntity e1 = new CurrencyEntity();
    e1.setTicker("NANO");
    CurrencyEntity e2 = new CurrencyEntity();
    e2.setTicker("NANO");
    when(currenciesRepository.findByTicker("NANO")).thenReturn(List.of(e1, e2));

    priceService.updateCurrencyEntity("nano", new BigDecimal("1.0"));

    verify(currenciesRepository).findByTicker("NANO");
    verify(currenciesRepository, org.mockito.Mockito.never()).save(any());
  }
}
