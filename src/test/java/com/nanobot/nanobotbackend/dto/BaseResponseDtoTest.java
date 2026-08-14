package com.nanobot.nanobotbackend.dto;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.util.List;
import org.junit.jupiter.api.Test;

class BaseResponseDtoTest {

  @Test
  void defaultConstructorSetsNullErrorMessage() {
    BaseResponseDto dto = new BaseResponseDto();
    assertNull(dto.getErrorMessage());
  }

  @Test
  void errorConstructorSetsErrorMessage() {
    BaseResponseDto dto = new BaseResponseDto("Something went wrong");
    assertEquals("Something went wrong", dto.getErrorMessage());
  }

  @Test
  void setErrorMessageUpdatesValue() {
    BaseResponseDto dto = new BaseResponseDto();
    dto.setErrorMessage("Updated error");
    assertEquals("Updated error", dto.getErrorMessage());
  }

  @Test
  void setCommandsAndGetCommands() {
    BaseResponseDto dto = new BaseResponseDto();
    List<CommandDto> commands = List.of(new CommandDto("wallet", "wallet-123"));
    dto.setCommands(commands);
    assertEquals(commands, dto.getCommands());
  }

  @Test
  void toJsonExcludesNullsWhenIncludeNullsFalse() {
    BaseResponseDto dto = new BaseResponseDto("error");
    String json = dto.toJson(false);
    assertNotNull(json);
    assertEquals("{\"errorMessage\":\"error\"}", json);
  }

  @Test
  void toJsonIncludesNullsWhenIncludeNullsTrue() {
    BaseResponseDto dto = new BaseResponseDto();
    String json = dto.toJson(true);
    assertNotNull(json);
    assertTrue(json.contains("errorMessage"));
    assertTrue(json.contains("null"));
  }
}
