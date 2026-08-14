package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.repository.CurrenciesRepository;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class CurrenciesServiceImplTest {

  @Mock
  private CurrenciesRepository currenciesRepository;

  @InjectMocks
  private CurrenciesServiceImpl currenciesService;

  private CurrencyEntity banEntity;
  private CurrencyEntity xnoEntity;

  @BeforeEach
  void setUp() {
    banEntity = currencyEntity("ban-id", "BAN", "Banano", "🍌");
    xnoEntity = currencyEntity("xno-id", "XNO", "Nano", "<:xno:>");
  }

  @Test
  void getCurrencyDecimalValueConvertsRawAndStripsTrailingZeros() {
    String result = currenciesService.getCurrencyDecimalValue("1500", 3);
    assertEquals("1.5", result);
  }

  @Test
  void getCurrencyDollarValueMultipliesAndRoundsDownToEightDecimals() {
    String result = currenciesService.getCurrencyDollarValue("1.234567899", "3");
    assertEquals("3.70370369", result);
  }

  @Test
  void analyzeCurrenciesMatchesTickerNameAndEmoji() {
    when(currenciesRepository.findAll()).thenReturn(List.of(banEntity, xnoEntity));

    CurrencyDto byTicker = currenciesService.analyzeCurrencies("ban");
    CurrencyDto byName = currenciesService.analyzeCurrencies("Nano");
    CurrencyDto byEmoji = currenciesService.analyzeCurrencies("🍌");

    assertNotNull(byTicker);
    assertEquals("BAN", byTicker.getTicker());
    assertNotNull(byName);
    assertEquals("XNO", byName.getTicker());
    assertNotNull(byEmoji);
    assertEquals("BAN", byEmoji.getTicker());
    verify(currenciesRepository, times(3)).findAll();
  }

  @Test
  void analyzeCurrenciesReturnsNullForInvalidInput() {
    assertNull(currenciesService.analyzeCurrencies(null));
    assertNull(currenciesService.analyzeCurrencies(" "));
    verify(currenciesRepository, never()).findAll();
  }

  @Test
  void createCurrencyReturnsEmptyWhenDuplicateTickerExists() {
    CurrencyDto dto = currencyDto("BAN", "Banano", "🍌");
    when(currenciesRepository.insert(any(CurrencyEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<CurrencyEntity> created = currenciesService.createCurrency(dto);

    assertTrue(created.isEmpty());
    verify(currenciesRepository).insert(any(CurrencyEntity.class));
  }

  @Test
  void createCurrencyReturnsCreatedEntityWhenInsertSucceeds() {
    CurrencyDto dto = currencyDto("BAN", "Banano", "🍌");
    CurrencyEntity inserted = currencyEntity("created-id", "BAN", "Banano", "🍌");
    when(currenciesRepository.insert(any(CurrencyEntity.class))).thenReturn(inserted);

    Optional<CurrencyEntity> created = currenciesService.createCurrency(dto);

    assertTrue(created.isPresent());
    assertEquals("created-id", created.get().getId());
    verify(currenciesRepository).insert(any(CurrencyEntity.class));
  }

  @Test
  void setCurrenciesMapsEntitiesToDtosAndPassesToConsumer() {
    when(currenciesRepository.findAll()).thenReturn(List.of(banEntity, xnoEntity));

    AtomicReference<List<CurrencyDto>> captured = new AtomicReference<>();
    currenciesService.setCurrencies(captured::set);

    assertNotNull(captured.get());
    assertEquals(2, captured.get().size());
    assertEquals("BAN", captured.get().get(0).getTicker());
    assertEquals("XNO", captured.get().get(1).getTicker());
    verify(currenciesRepository).findAll();
  }

  @Test
  void getCurrenciesReturnsFilteredResultWhenTickerProvided() {
    when(currenciesRepository.findByTicker("BAN")).thenReturn(List.of(banEntity));

    List<CurrencyEntity> result = currenciesService.getCurrencies("BAN");

    assertEquals(1, result.size());
    assertEquals("BAN", result.get(0).getTicker());
    verify(currenciesRepository).findByTicker("BAN");
    verify(currenciesRepository, never()).findAll();
  }

  @Test
  void getCurrencyByIdReturnsEmptyWhenIdIsNull() {
    Optional<CurrencyEntity> result = currenciesService.getCurrencyById(null);

    assertTrue(result.isEmpty());
    verify(currenciesRepository, never()).findById(anyString());
  }

  @Test
  void getCurrencyByTickerReturnsEmptyWhenMultipleFound() {
    when(currenciesRepository.findByTicker("BAN"))
      .thenReturn(List.of(currencyEntity("1", "BAN", "Banano", "🍌"), currencyEntity("2", "BAN", "Banano2", "🍌")));

    Optional<CurrencyEntity> result = currenciesService.getCurrencyByTicker("BAN");

    assertTrue(result.isEmpty());
    verify(currenciesRepository).findByTicker("BAN");
  }

  @Test
  void getCurrencyByTickerReturnsEntityWhenSingleMatchFound() {
    when(currenciesRepository.findByTicker("BAN")).thenReturn(List.of(banEntity));

    Optional<CurrencyEntity> result = currenciesService.getCurrencyByTicker("BAN");

    assertTrue(result.isPresent());
    assertEquals("ban-id", result.get().getId());
  }

  @Test
  void updateCurrencyUpdatesAllMutableFieldsWhenEntityExists() {
    CurrencyEntity existing = currencyEntity("id-1", "OLD", "OldName", "🙂");
    CurrencyDto update = currencyDto("NEW", "NewName", "🆕");
    update.setAddress("new-address");
    update.setNodeUrl("https://new-node");
    update.setEnabled(false);
    update.setProcessDeposits(false);
    update.setProcessWithdrawals(false);
    update.setOpenDifficulty("11");
    update.setReceiveDifficulty("12");
    update.setSendDifficulty("13");
    update.setUpdateDifficulty("14");
    update.setColor("ffaa00");
    update.setPrecision("6");
    update.setLiquidity("123");
    update.setValue("42");
    update.setMinimumDeposit("2");
    update.setMinimumWithdraw("3");
    update.setMinimumDrop("4");
    update.setMinimumGift("5");
    update.setMinimumRain("6");

    when(currenciesRepository.findById("id-1")).thenReturn(Optional.of(existing));
    when(currenciesRepository.save(any(CurrencyEntity.class)))
      .thenAnswer(invocation -> invocation.getArgument(0));

    Optional<CurrencyEntity> result = currenciesService.updateCurrency("id-1", update);

    assertTrue(result.isPresent());
    CurrencyEntity updated = result.get();
    assertEquals("NEW", updated.getTicker());
    assertEquals("NewName", updated.getName());
    assertEquals("new-address", updated.getAddress());
    assertEquals("https://new-node", updated.getNodeUrl());
    assertFalse(updated.getEnabled());
    assertEquals("5", updated.getMinimumGift());
    verify(currenciesRepository).save(existing);
  }

  @Test
  void updateCurrencyReturnsEmptyWhenCurrencyIsMissing() {
    when(currenciesRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<CurrencyEntity> result = currenciesService.updateCurrency(
      "missing",
      currencyDto("BAN", "Banano", "🍌")
    );

    assertTrue(result.isEmpty());
    verify(currenciesRepository, never()).save(any());
  }

  @Test
  void deleteCurrencyDeletesAndReturnsEntityWhenFound() {
    when(currenciesRepository.findById("ban-id")).thenReturn(Optional.of(banEntity));

    Optional<CurrencyEntity> result = currenciesService.deleteCurrency("ban-id");

    assertTrue(result.isPresent());
    assertEquals("ban-id", result.get().getId());
    verify(currenciesRepository).deleteById("ban-id");
  }

  @Test
  void deleteCurrencyReturnsEmptyWhenEntityMissing() {
    when(currenciesRepository.findById("missing-id")).thenReturn(Optional.empty());

    Optional<CurrencyEntity> result = currenciesService.deleteCurrency("missing-id");

    assertTrue(result.isEmpty());
    verify(currenciesRepository, never()).deleteById(anyString());
  }

  private CurrencyDto currencyDto(String ticker, String name, String emoji) {
    CurrencyDto dto = new CurrencyDto();
    dto.setTicker(ticker);
    dto.setName(name);
    dto.setEmoji(emoji);
    dto.setEnabled(true);
    dto.setProcessDeposits(true);
    dto.setProcessWithdrawals(true);
    dto.setOpenDifficulty("1");
    dto.setReceiveDifficulty("2");
    dto.setSendDifficulty("3");
    dto.setUpdateDifficulty("4");
    dto.setColor("00ff00");
    dto.setPrecision("29");
    dto.setLiquidity("0");
    dto.setValue("1");
    dto.setMinimumDeposit("1");
    dto.setMinimumWithdraw("1");
    dto.setMinimumDrop("1");
    dto.setMinimumGift("1");
    dto.setMinimumRain("1");
    return dto;
  }

  private CurrencyEntity currencyEntity(
    String id,
    String ticker,
    String name,
    String emoji
  ) {
    CurrencyEntity entity = new CurrencyEntity();
    entity.setId(id);
    entity.setTicker(ticker);
    entity.setName(name);
    entity.setEmoji(emoji);
    entity.setPrecision("29");
    entity.setEnabled(true);
    return entity;
  }
}
