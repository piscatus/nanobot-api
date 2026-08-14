package com.nanobot.nanobotbackend.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.nanobot.nanobotbackend.dto.EmojiDto;
import com.nanobot.nanobotbackend.entity.EmojiEntity;
import com.nanobot.nanobotbackend.service.EmojisService;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

@ExtendWith(MockitoExtension.class)
class EmojisControllerTest {

  private static final String BASE_PATH = "/emojis";

  private MockMvc mockMvc;

  @Mock
  private EmojisService emojisService;

  private ObjectMapper objectMapper;

  @BeforeEach
  void setUp() {
    objectMapper = new ObjectMapper();
    mockMvc =
      MockMvcBuilders.standaloneSetup(new EmojisController(emojisService)).build();
  }

  @Test
  void createEmojiShouldReturnCreatedWhenSuccess() throws Exception {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    dto.setEmoji("🐟");
    EmojiEntity created = new EmojiEntity("fish", "trout", "🐟");
    created.setId("e1");
    when(emojisService.createEmoji(any(EmojiDto.class))).thenReturn(Optional.of(created));

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isCreated())
      .andExpect(jsonPath("$.id").value("e1"))
      .andExpect(jsonPath("$.category").value("fish"))
      .andExpect(jsonPath("$.name").value("trout"));

    verify(emojisService).createEmoji(any(EmojiDto.class));
  }

  @Test
  void createEmojiShouldReturnBadRequestWhenDuplicate() throws Exception {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    dto.setEmoji("🐟");
    when(emojisService.createEmoji(any(EmojiDto.class))).thenReturn(Optional.empty());

    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isBadRequest())
      .andExpect(jsonPath("$.message").value("Emoji category/name already exists"));

    verify(emojisService).createEmoji(any(EmojiDto.class));
  }

  @Test
  void createEmojiShouldReturnBadRequestWhenInvalidJson() throws Exception {
    mockMvc
      .perform(
        post(BASE_PATH)
          .contentType(MediaType.APPLICATION_JSON)
          .content("invalid json")
      )
      .andExpect(status().isBadRequest());

    verify(emojisService, never()).createEmoji(any());
  }

  @Test
  void getEmojisShouldReturnOkAndDelegateToService() throws Exception {
    List<EmojiEntity> emojis = List.of(new EmojiEntity("fish", "trout", "🐟"));
    when(emojisService.getEmojis(null, null)).thenReturn(emojis);

    mockMvc
      .perform(get(BASE_PATH))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$[0].category").value("fish"))
      .andExpect(jsonPath("$[0].name").value("trout"));

    verify(emojisService).getEmojis(null, null);
  }

  @Test
  void getEmojisShouldPassCategoryAndNameParams() throws Exception {
    when(emojisService.getEmojis("fish", "trout")).thenReturn(List.of());

    mockMvc
      .perform(get(BASE_PATH).param("category", "fish").param("name", "trout"))
      .andExpect(status().isOk());

    verify(emojisService).getEmojis("fish", "trout");
  }

  @Test
  void getEmojiByIdShouldReturnOkWhenFound() throws Exception {
    EmojiEntity entity = new EmojiEntity("fish", "trout", "🐟");
    entity.setId("e1");
    when(emojisService.getEmojiById("e1")).thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/e1"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.id").value("e1"));

    verify(emojisService).getEmojiById("e1");
  }

  @Test
  void getEmojiByIdShouldReturnNotFoundWhenEmpty() throws Exception {
    when(emojisService.getEmojiById("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Emoji ID not found"));

    verify(emojisService).getEmojiById("missing");
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnOkWhenFound() throws Exception {
    EmojiEntity entity = new EmojiEntity("fish", "trout", "🐟");
    when(emojisService.getEmojiByCategoryAndName("fish", "trout"))
      .thenReturn(Optional.of(entity));

    mockMvc
      .perform(get(BASE_PATH + "/category/fish/name/trout"))
      .andExpect(status().isOk())
      .andExpect(jsonPath("$.category").value("fish"))
      .andExpect(jsonPath("$.name").value("trout"));

    verify(emojisService).getEmojiByCategoryAndName("fish", "trout");
  }

  @Test
  void getEmojiByCategoryAndNameShouldReturnNotFoundWhenEmpty() throws Exception {
    when(emojisService.getEmojiByCategoryAndName("fish", "trout"))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(get(BASE_PATH + "/category/fish/name/trout"))
      .andExpect(status().isNotFound())
      .andExpect(
        jsonPath("$.message").value("Emoji with specified category/name not found")
      );

    verify(emojisService).getEmojiByCategoryAndName("fish", "trout");
  }

  @Test
  void updateEmojiShouldReturnAcceptedWhenSuccess() throws Exception {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("salmon");
    dto.setEmoji("🐟");
    EmojiEntity updated = new EmojiEntity("fish", "salmon", "🐟");
    updated.setId("e1");
    when(emojisService.updateEmoji(eq("e1"), any(EmojiDto.class)))
      .thenReturn(Optional.of(updated));

    mockMvc
      .perform(
        put(BASE_PATH + "/e1")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isAccepted())
      .andExpect(jsonPath("$.name").value("salmon"));

    verify(emojisService).updateEmoji(eq("e1"), any(EmojiDto.class));
  }

  @Test
  void updateEmojiShouldReturnNotFoundWhenEmpty() throws Exception {
    EmojiDto dto = new EmojiDto();
    dto.setCategory("fish");
    dto.setName("trout");
    when(emojisService.updateEmoji(eq("missing"), any(EmojiDto.class)))
      .thenReturn(Optional.empty());

    mockMvc
      .perform(
        put(BASE_PATH + "/missing")
          .contentType(MediaType.APPLICATION_JSON)
          .content(objectMapper.writeValueAsString(dto))
      )
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Emoji ID not found"));

    verify(emojisService).updateEmoji(eq("missing"), any(EmojiDto.class));
  }

  @Test
  void deleteEmojiShouldReturnNoContentWhenSuccess() throws Exception {
    EmojiEntity entity = new EmojiEntity("fish", "trout", "🐟");
    entity.setId("e1");
    when(emojisService.deleteEmoji("e1")).thenReturn(Optional.of(entity));

    mockMvc.perform(delete(BASE_PATH + "/e1")).andExpect(status().isNoContent());

    verify(emojisService).deleteEmoji("e1");
  }

  @Test
  void deleteEmojiShouldReturnNotFoundWhenEmpty() throws Exception {
    when(emojisService.deleteEmoji("missing")).thenReturn(Optional.empty());

    mockMvc
      .perform(delete(BASE_PATH + "/missing"))
      .andExpect(status().isNotFound())
      .andExpect(jsonPath("$.message").value("Emoji ID not found"));

    verify(emojisService).deleteEmoji("missing");
  }
}
