package com.nanobot.nanobotbackend.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.util.List;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.data.mongo.DataMongoTest;
import org.springframework.test.context.ActiveProfiles;

@DataMongoTest
@ActiveProfiles("test")
class CurrenciesRepositoryTest {

  @Autowired
  private CurrenciesRepository currenciesRepository;

  @BeforeEach
  void setUp() {
    currenciesRepository.deleteAll();
  }

  @Test
  void findAllReturnsEmptyWhenNoCurrencies() {
    List<CurrencyEntity> result = currenciesRepository.findAll();
    assertTrue(result.isEmpty());
  }

  @Test
  void insertAndFindAllReturnsSavedCurrency() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XNO");
    currency.setName("Nano");
    currency.setEnabled(true);

    CurrencyEntity saved = currenciesRepository.insert(currency);
    assertTrue(saved.getId() != null && !saved.getId().isEmpty());

    List<CurrencyEntity> all = currenciesRepository.findAll();
    assertEquals(1, all.size());
    assertEquals("XNO", all.get(0).getTicker());
  }

  @Test
  void findByTickerReturnsMatchingCurrencies() {
    CurrencyEntity c1 = new CurrencyEntity();
    c1.setTicker("XNO");
    currenciesRepository.insert(c1);

    CurrencyEntity c2 = new CurrencyEntity();
    c2.setTicker("XNO");
    currenciesRepository.insert(c2);

    CurrencyEntity c3 = new CurrencyEntity();
    c3.setTicker("BAN");
    currenciesRepository.insert(c3);

    List<CurrencyEntity> byTicker = currenciesRepository.findByTicker("XNO");
    assertEquals(2, byTicker.size());
    assertTrue(byTicker.stream().allMatch(c -> "XNO".equals(c.getTicker())));
  }

  @Test
  void findByTickerWithNullReturnsAll() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XNO");
    currenciesRepository.insert(currency);

    List<CurrencyEntity> result = currenciesRepository.findByTicker(null);
    assertEquals(1, result.size());
  }

  @Test
  void findByIdReturnsEntityWhenExists() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XNO");
    CurrencyEntity saved = currenciesRepository.insert(currency);

    var found = currenciesRepository.findById(saved.getId());
    assertTrue(found.isPresent());
    assertEquals("XNO", found.get().getTicker());
  }

  @Test
  void findByIdReturnsEmptyWhenNotExists() {
    var found = currenciesRepository.findById("nonexistent-id");
    assertFalse(found.isPresent());
  }

  @Test
  void saveUpdatesExistingEntity() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XNO");
    currency.setEnabled(true);
    CurrencyEntity saved = currenciesRepository.insert(currency);

    saved.setTicker("NANO");
    saved.setEnabled(false);
    CurrencyEntity updated = currenciesRepository.save(saved);

    assertEquals("NANO", updated.getTicker());
    assertFalse(updated.getEnabled());
  }

  @Test
  void deleteByIdRemovesEntity() {
    CurrencyEntity currency = new CurrencyEntity();
    currency.setTicker("XNO");
    CurrencyEntity saved = currenciesRepository.insert(currency);

    currenciesRepository.deleteById(saved.getId());

    var found = currenciesRepository.findById(saved.getId());
    assertFalse(found.isPresent());
  }
}
