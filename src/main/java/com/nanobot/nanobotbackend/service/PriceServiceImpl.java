package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.repository.CurrenciesRepository;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.math.BigDecimal;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.time.Duration;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

@Service
public class PriceServiceImpl implements PriceService {

  private final FileLogger fileLogger;

  private final CurrenciesRepository currenciesRepository;

  private final HttpClient client = HttpClient.newHttpClient();

  @Autowired
  public PriceServiceImpl(CurrenciesRepository currenciesRepository) {
    this.currenciesRepository = currenciesRepository;
    this.fileLogger = new FileLogger("PriceService");
  }

  @Override
  public void checkPrices() {
    final int MAX_RETRIES = 3;

    for (int attempt = 1; attempt <= MAX_RETRIES; attempt++) {
      try {
        HttpRequest request = HttpRequest.newBuilder()
          .uri(
            URI.create(
              "https://api.coingecko.com/api/v3/coins/markets" +
              "?vs_currency=usd&ids=nano%2Cbanano" +
              "&order=market_cap_desc&per_page=100&page=1" +
              "&sparkline=false&locale=en"
            )
          )
          .timeout(Duration.ofSeconds(10))
          .build();

        HttpResponse<String> response = client.send(
          request,
          HttpResponse.BodyHandlers.ofString()
        );

        if (response.statusCode() == 200) {
          processPriceData(response.body());
          return; // success
        } else {
          fileLogger.error(
            "Failed to retrieve prices. HTTP status: " + response.statusCode()
          );
        }
      } catch (Exception e) {
        fileLogger.warn("Attempt " + attempt + " failed: " + e.getMessage());
        if (attempt == MAX_RETRIES) {
          fileLogger.error("All retries failed for price check.");
        } else {
          try {
            Thread.sleep(2000L * attempt); // exponential backoff
          } catch (InterruptedException ignored) {}
        }
      }
    }
  }

  public void processPriceData(String data) {
    try {
      JSONArray jsonArray = new JSONArray(data);
      for (int i = 0; i < jsonArray.length(); i++) {
        JSONObject coin = jsonArray.getJSONObject(i);
        String symbol = coin.getString("symbol");

        BigDecimal currentPrice = new BigDecimal(
          coin.get("current_price").toString()
        );

        updateCurrencyEntity(symbol, currentPrice);
      }
    } catch (JSONException e) {
      fileLogger.error("Error parsing JSON: " + e.getMessage());
    }
  }

  public void updateCurrencyEntity(String symbol, BigDecimal currentPrice) {
    List<CurrencyEntity> existingTickers = currenciesRepository.findByTicker(
      symbol.toUpperCase()
    );

    if (existingTickers.size() == 1) {
      CurrencyEntity entity = existingTickers.get(0);

      String formattedPrice = currentPrice.stripTrailingZeros().toPlainString();

      entity.setValue(formattedPrice);
      currenciesRepository.save(entity);

      fileLogger.info(
        "Symbol: " + symbol.toUpperCase() + ", Set Price: " + formattedPrice
      );
    } else if (existingTickers.size() > 1) {
      fileLogger.error(
        "Multiple currencies found with the same ticker: " +
        symbol.toUpperCase()
      );
    } else {
      fileLogger.warn(
        "No currency entity found for symbol: " + symbol.toUpperCase()
      );
    }
  }
}
