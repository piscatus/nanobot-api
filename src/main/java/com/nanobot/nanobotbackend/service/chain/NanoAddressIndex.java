package com.nanobot.nanobotbackend.service.chain;

import com.nanobot.nanobotbackend.entity.UserDetailsEntity;
import com.nanobot.nanobotbackend.util.CryptoUtil;
import java.util.ArrayList;
import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import org.springframework.stereotype.Component;

/**
 * Which user owns a given Nano-family deposit address.
 *
 * <p>Nano addresses are derived from the user's seed rather than stored, so the
 * only way to answer "who does this address belong to" used to be deriving
 * every user's address again. That is fine at the top of a batch scan but not
 * per websocket notification, so the mapping is kept here instead.
 *
 * <p>Only the user id is retained, never the seed or private key. A deposit is
 * rare enough that reloading the signing material by id costs one indexed read,
 * which is a better trade than keeping every user's secret resident in memory
 * for the life of the process.
 */
@Component
public class NanoAddressIndex {

  /** ticker to (address to userId). */
  private final Map<String, Map<String, String>> byTicker =
    new ConcurrentHashMap<>();

  /**
   * Rebuilds the mapping for one ticker and returns the address to user map the
   * caller needs anyway. Derivation is the expensive part, so the reconciliation
   * sweep and the index share a single pass over the users.
   */
  public Map<String, UserDetailsEntity> rebuild(
    String ticker,
    List<UserDetailsEntity> users
  ) {
    Map<String, UserDetailsEntity> addressMap = new LinkedHashMap<>();
    Map<String, String> owners = new ConcurrentHashMap<>();

    for (UserDetailsEntity userDetails : users) {
      if (
        !CryptoUtil.canResolvePrivateKey(
          userDetails.getSeed(),
          userDetails.getPrivateKey()
        )
      ) {
        continue;
      }
      String address = CryptoUtil.deriveAddress(userDetails, ticker);
      addressMap.put(address, userDetails);
      if (userDetails.getUserId() != null) {
        owners.put(address, userDetails.getUserId());
      }
    }

    byTicker.put(ticker, owners);
    return addressMap;
  }

  /** Null when the address belongs to nobody the bot custodies. */
  public String ownerOf(String ticker, String address) {
    if (ticker == null || address == null) {
      return null;
    }
    Map<String, String> owners = byTicker.get(ticker);
    return owners == null ? null : owners.get(address);
  }

  public boolean isKnown(String ticker, String address) {
    return ownerOf(ticker, address) != null;
  }

  /** Empty until the first rebuild, which the startup sweep performs. */
  public List<String> addresses(String ticker) {
    Map<String, String> owners = byTicker.get(ticker);
    if (owners == null) {
      return Collections.emptyList();
    }
    return new ArrayList<>(owners.keySet());
  }

  public boolean isPopulated(String ticker) {
    Map<String, String> owners = byTicker.get(ticker);
    return owners != null && !owners.isEmpty();
  }

  /**
   * Registers a single address, returning false when it was already known so
   * callers can skip pushing a redundant subscription update to the node.
   */
  public boolean add(String ticker, String address, String userId) {
    if (ticker == null || address == null || userId == null) {
      return false;
    }
    Map<String, String> owners = byTicker.computeIfAbsent(
      ticker,
      key -> new ConcurrentHashMap<>()
    );
    return owners.put(address, userId) == null;
  }

  public void clear() {
    byTicker.clear();
  }
}
