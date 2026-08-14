package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;

public interface CurrenciesService {
  Optional<CurrencyEntity> createCurrency(CurrencyDto currencyDto);

  List<CurrencyEntity> getCurrencies(String ticker);

  Optional<CurrencyEntity> getCurrencyById(String id);

  Optional<CurrencyEntity> getCurrencyByTicker(String ticker);

  Optional<CurrencyEntity> updateCurrency(String id, CurrencyDto currencyDto);

  Optional<CurrencyEntity> deleteCurrency(String id);

  void setCurrencies(Consumer<List<CurrencyDto>> setCurrencies);

  String getCurrencyDecimalValue(String wholeNumber, int precision);

  String getCurrencyDollarValue(String decimalNumber, String dollarNumber);

  CurrencyDto analyzeCurrencies(String inputString);
}
