package com.nanobot.nanobotbackend.controller;

import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.ErrorDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import java.util.Date;
import java.util.List;
import java.util.Optional;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("currencies")
public class CurrenciesController {

  private CurrenciesService currenciesService;

  public CurrenciesController(CurrenciesService currenciesService) {
    this.currenciesService = currenciesService;
  }

  @PostMapping
  public ResponseEntity<Optional<Object>> createCurrency(
    @RequestBody CurrencyDto newCurrencyDto
  ) {
    Optional<CurrencyEntity> currency = currenciesService.createCurrency(
      newCurrencyDto
    );

    if (currency.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        400,
        "Currency ticker already exists!",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.BAD_REQUEST);
    }
    return new ResponseEntity<>(Optional.of(currency), HttpStatus.CREATED);
  }

  @GetMapping
  public ResponseEntity<List<CurrencyEntity>> getCurrencies(
    @RequestParam(required = false) String ticker
  ) {
    return new ResponseEntity<>(
      currenciesService.getCurrencies(ticker),
      HttpStatus.OK
    );
  }

  @GetMapping("/{id}")
  public ResponseEntity<Optional<Object>> getCurrencyById(
    @PathVariable String id
  ) {
    Optional<CurrencyEntity> currency = currenciesService.getCurrencyById(id);

    if (currency.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Currency ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(currency), HttpStatus.OK);
  }

  @GetMapping("/ticker/{ticker}")
  public ResponseEntity<Optional<Object>> getCurrencyByTicker(
    @PathVariable String ticker
  ) {
    Optional<CurrencyEntity> currency = currenciesService.getCurrencyByTicker(
      ticker
    );

    if (currency.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Currency with specified ticker not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(currency), HttpStatus.OK);
  }

  @PutMapping("/{id}")
  public ResponseEntity<Optional<Object>> updateCurrency(
    @PathVariable String id,
    @RequestBody CurrencyDto updatedCurrencyDto
  ) {
    Optional<CurrencyEntity> currency = currenciesService.updateCurrency(
      id,
      updatedCurrencyDto
    );

    if (currency.isEmpty()) {
      ErrorDto error = new ErrorDto(
        new Date(),
        404,
        "Currency ID not found",
        ""
      );
      return new ResponseEntity<>(Optional.of(error), HttpStatus.NOT_FOUND);
    }
    return new ResponseEntity<>(Optional.of(currency), HttpStatus.ACCEPTED);
  }

  @DeleteMapping("/{id}")
  public ResponseEntity<Optional<Object>> deleteCurrency(
    @PathVariable String id
  ) {
    Optional<CurrencyEntity> currency = currenciesService.deleteCurrency(id);

    if (currency.isEmpty()) {
      return new ResponseEntity<>(
        Optional.of(new ErrorDto(new Date(), 404, "Currency ID not found", "")),
        HttpStatus.NOT_FOUND
      );
    }
    return ResponseEntity.noContent().build();
  }
}
