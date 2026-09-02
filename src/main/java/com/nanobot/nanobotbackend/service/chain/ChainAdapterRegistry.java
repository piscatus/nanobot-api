package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.stereotype.Service;

/** Resolves the {@link ChainAdapter} for a currency by its protocol. */
@Service
public class ChainAdapterRegistry {

  /**
   * Adapters are fetched through a provider instead of being injected as a
   * List. An adapter may legitimately need to dispatch to other adapters, and
   * injecting the collection directly makes that a constructor cycle, which
   * Spring rejects outright. The provider defers resolution until first use, by
   * which point every adapter is fully constructed.
   */
  private final ObjectProvider<ChainAdapter> adapterProvider;

  private final FileLogger fileLogger;

  private volatile Map<String, ChainAdapter> adaptersByProtocol;

  public ChainAdapterRegistry(ObjectProvider<ChainAdapter> adapterProvider) {
    this.adapterProvider = adapterProvider;
    this.fileLogger = new FileLogger("ChainAdapterRegistry");
  }

  private Map<String, ChainAdapter> adapters() {
    Map<String, ChainAdapter> resolved = adaptersByProtocol;
    if (resolved != null) {
      return resolved;
    }
    synchronized (this) {
      if (adaptersByProtocol == null) {
        Map<String, ChainAdapter> built = new HashMap<>();
        adapterProvider.stream().forEach(adapter -> {
          ChainAdapter previous = built.put(
            adapter.protocol().toUpperCase(),
            adapter
          );
          if (previous != null) {
            fileLogger.error(
              "Two adapters claim protocol " +
              adapter.protocol() +
              ": " +
              previous.getClass().getSimpleName() +
              " and " +
              adapter.getClass().getSimpleName()
            );
          }
        });
        fileLogger.info("Registered chain adapters: " + built.keySet());
        adaptersByProtocol = built;
      }
      return adaptersByProtocol;
    }
  }

  /**
   * Empty when a currency names a protocol with no adapter, which callers treat
   * as a configuration error rather than a silent no-op.
   */
  public Optional<ChainAdapter> get(CurrencyEntity currencyEntity) {
    if (currencyEntity == null) {
      return Optional.empty();
    }
    return getByProtocol(currencyEntity.getProtocol());
  }

  public Optional<ChainAdapter> getByProtocol(String protocol) {
    if (protocol == null || protocol.isBlank()) {
      return Optional.empty();
    }
    return Optional.ofNullable(adapters().get(protocol.toUpperCase()));
  }
}
