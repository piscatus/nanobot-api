package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.dto.UserDetailsDto;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.service.CurrenciesService;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * Resolves a user's deposit address for a ticker through the owning
 * {@link ChainAdapter}.
 *
 * <p>Exists so callers holding only currency DTOs do not need the registry or
 * the currency entity themselves. The entity is required because wallet
 * credentials are deliberately kept off the DTO, which is serialized to the
 * Discord frontend.
 */
@Component
public class DepositAddressResolver {

  private final ChainAdapterRegistry chainAdapterRegistry;
  private final CurrenciesService currenciesService;
  private final FileLogger fileLogger;

  public DepositAddressResolver(
    ChainAdapterRegistry chainAdapterRegistry,
    CurrenciesService currenciesService
  ) {
    this.chainAdapterRegistry = chainAdapterRegistry;
    this.currenciesService = currenciesService;
    this.fileLogger = new FileLogger("DepositAddressResolver");
  }

  /**
   * Returns null when the currency is unknown, has no adapter, or the adapter
   * could not allocate an address, for example because a wallet is unreachable.
   * Callers omit the currency rather than showing an address that cannot receive
   * funds.
   */
  public String resolve(UserDetailsDto userDetails, String ticker) {
    Optional<CurrencyEntity> currency = currenciesService.getCurrencyByTicker(
      ticker
    );
    if (currency.isEmpty()) {
      fileLogger.error("No currency found for ticker " + ticker);
      return null;
    }

    Optional<ChainAdapter> adapter = chainAdapterRegistry.get(currency.get());
    if (adapter.isEmpty()) {
      fileLogger.error(
        "No chain adapter registered for protocol " +
        currency.get().getProtocol() +
        " (" +
        ticker +
        ")"
      );
      return null;
    }

    return adapter
      .get()
      .resolveDepositAddress(
        new UserDetailsEntity(userDetails),
        currency.get()
      );
  }
}
