package com.nanobot.nanobotbackend.util;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;

import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.dto.ItemDto;
import java.util.HashSet;
import java.util.Set;
import org.junit.jupiter.api.Test;

/**
 * Smoke tests for LoggingUtil. Verifies that logging methods do not throw
 * when invoked with valid or edge-case inputs. LoggingUtil delegates to
 * FileLogger; we do not assert on output.
 */
class LoggingUtilTest {

  @Test
  void errorShouldNotThrow() {
    assertDoesNotThrow(() -> LoggingUtil.error("test error message"));
  }

  @Test
  void infoShouldNotThrow() {
    assertDoesNotThrow(() -> LoggingUtil.info("test info message"));
  }

  @Test
  void warnShouldNotThrow() {
    assertDoesNotThrow(() -> LoggingUtil.warn("test warn message"));
  }

  @Test
  void requestLoggingShouldNotThrowWithNullRequestDto() {
    assertDoesNotThrow(() -> LoggingUtil.requestLogging("fish", null));
  }

  @Test
  void requestLoggingShouldNotThrowWithValidRequestDto() {
    RequestDto dto = new RequestDto(
      null,
      "channel-1",
      false,
      null,
      null,
      false,
      "guild-1",
      null,
      null,
      null,
      null,
      null,
      "user-1",
      null,
      null,
      null
    );
    assertDoesNotThrow(() -> LoggingUtil.requestLogging("gift", dto));
  }

  @Test
  void errorLoggingShouldNotThrow() {
    Exception ex = new RuntimeException("test exception");
    assertDoesNotThrow(() -> LoggingUtil.errorLogging("fish", ex));
  }

  @Test
  void logCurrentGuildWalletsShouldNotThrowWithNull() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentGuildWallets(null));
  }

  @Test
  void logCurrentGuildWalletsShouldNotThrowWithEmptySet() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentGuildWallets(new HashSet<>()));
  }

  @Test
  void logCurrentGuildWalletsShouldNotThrowWithValidData() {
    GuildWalletsEntity entity = new GuildWalletsEntity("guild1");
    entity.setWallets(java.util.List.of(new WalletDto("XNO", "100")));
    Set<GuildWalletsEntity> set = new HashSet<>();
    set.add(entity);
    assertDoesNotThrow(() -> LoggingUtil.logCurrentGuildWallets(set));
  }

  @Test
  void logCurrentItemsShouldNotThrowWithNull() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentItems(null));
  }

  @Test
  void logCurrentItemsShouldNotThrowWithEmptySet() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentItems(new HashSet<>()));
  }

  @Test
  void logCurrentItemsShouldNotThrowWithValidData() {
    UserItemsEntity entity = new UserItemsEntity("user1");
    entity.setItems(java.util.List.of(new ItemDto("SHRIMP", 5)));
    Set<UserItemsEntity> set = new HashSet<>();
    set.add(entity);
    assertDoesNotThrow(() -> LoggingUtil.logCurrentItems(set));
  }

  @Test
  void logCurrentWalletsShouldNotThrowWithNull() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentWallets(null));
  }

  @Test
  void logCurrentWalletsShouldNotThrowWithEmptySet() {
    assertDoesNotThrow(() -> LoggingUtil.logCurrentWallets(new HashSet<>()));
  }

  @Test
  void logCurrentWalletsShouldNotThrowWithValidData() {
    UserWalletsEntity entity = new UserWalletsEntity("user1");
    entity.setWallets(java.util.List.of(new WalletDto("XNO", "100")));
    Set<UserWalletsEntity> set = new HashSet<>();
    set.add(entity);
    assertDoesNotThrow(() -> LoggingUtil.logCurrentWallets(set));
  }
}
