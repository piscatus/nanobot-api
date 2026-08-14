package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import com.nanobot.nanobotbackend.repository.AliasesRepository;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.function.Consumer;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class AliasesServiceImplTest {

  @Mock
  private AliasesRepository aliasesRepository;

  private AliasesServiceImpl aliasesService;

  @BeforeEach
  void setUp() {
    aliasesService = new AliasesServiceImpl(aliasesRepository);
  }

  @Test
  void analyzeAliasesReturnsNullWhenInputStringEmpty() {
    assertNull(aliasesService.analyzeAliases("guild1", ""));
    assertNull(aliasesService.analyzeAliases("guild1", null));
    verify(aliasesRepository, never()).findByEitherGuildId(any(), any());
  }

  @Test
  void analyzeAliasesReturnsAliasDtoWhenSingularMatches() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByEitherGuildId("guild1", "GLOBAL"))
      .thenReturn(List.of(entity));

    AliasDto result = aliasesService.analyzeAliases("guild1", "big");

    assertNotNull(result);
    assertEquals("big", result.getSingular());
    assertEquals("bigs", result.getPlural());
    assertEquals("XNO", result.getTicker());
    assertEquals("100", result.getValue());
  }

  @Test
  void analyzeAliasesReturnsAliasDtoWhenPluralMatches() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByEitherGuildId("guild1", "GLOBAL"))
      .thenReturn(List.of(entity));

    AliasDto result = aliasesService.analyzeAliases("guild1", "bigs");

    assertNotNull(result);
    assertEquals("big", result.getSingular());
  }

  @Test
  void analyzeAliasesReturnsAliasDtoWhenEmojiMatches() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByEitherGuildId("guild1", "GLOBAL"))
      .thenReturn(List.of(entity));

    AliasDto result = aliasesService.analyzeAliases("guild1", ":big:");

    assertNotNull(result);
    assertEquals("big", result.getSingular());
  }

  @Test
  void analyzeAliasesReturnsNullWhenNoMatch() {
    when(aliasesRepository.findByEitherGuildId("guild1", "GLOBAL"))
      .thenReturn(List.of());

    assertNull(aliasesService.analyzeAliases("guild1", "unknown"));
  }

  @Test
  void analyzeAliasesIsCaseInsensitive() {
    AliasEntity entity = createAliasEntity("g1", "Big", "Bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByEitherGuildId("guild1", "GLOBAL"))
      .thenReturn(List.of(entity));

    assertNotNull(aliasesService.analyzeAliases("guild1", "BIG"));
    assertNotNull(aliasesService.analyzeAliases("guild1", "BIGS"));
  }

  @Test
  void createAliasReturnsEntityWhenSuccess() {
    AliasDto dto = new AliasDto("g1", "big", "bigs", "XNO", "100", ":big:");
    AliasEntity saved = new AliasEntity(dto);
    saved.setId("alias-123");
    when(aliasesRepository.insert(any(AliasEntity.class))).thenReturn(saved);

    Optional<AliasEntity> result = aliasesService.createAlias(dto);

    assertTrue(result.isPresent());
    assertEquals("alias-123", result.get().getId());
    verify(aliasesRepository).insert(any(AliasEntity.class));
  }

  @Test
  void createAliasReturnsEmptyWhenDuplicateKey() {
    AliasDto dto = new AliasDto("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.insert(any(AliasEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<AliasEntity> result = aliasesService.createAlias(dto);

    assertTrue(result.isEmpty());
  }

  @Test
  void getAliasesReturnsAllWhenBothNull() {
    List<AliasEntity> all = List.of(new AliasEntity());
    when(aliasesRepository.findAll()).thenReturn(all);

    List<AliasEntity> result = aliasesService.getAliases(null, null);

    assertEquals(1, result.size());
    verify(aliasesRepository).findAll();
  }

  @Test
  void getAliasesReturnsByGuildIdWhenSingularNull() {
    List<AliasEntity> guildAliases = List.of(new AliasEntity());
    when(aliasesRepository.findByGuildId("g1")).thenReturn(guildAliases);

    List<AliasEntity> result = aliasesService.getAliases("g1", null);

    assertEquals(1, result.size());
    verify(aliasesRepository).findByGuildId("g1");
  }

  @Test
  void getAliasesReturnsByGuildIdAndSingular() {
    List<AliasEntity> specific = List.of(createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:"));
    when(aliasesRepository.findByGuildIdAndSingular("g1", "big")).thenReturn(specific);

    List<AliasEntity> result = aliasesService.getAliases("g1", "big");

    assertEquals(1, result.size());
    assertEquals("big", result.get(0).getSingular());
  }

  @Test
  void getAliasByIdReturnsEmptyWhenIdNull() {
    assertTrue(aliasesService.getAliasById(null).isEmpty());
    verify(aliasesRepository, never()).findById(any());
  }

  @Test
  void getAliasByIdReturnsEntityWhenFound() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    entity.setId("id-1");
    when(aliasesRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<AliasEntity> result = aliasesService.getAliasById("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
  }

  @Test
  void getAliasByGuildIdAndSingularReturnsEmptyWhenParamsNull() {
    assertTrue(aliasesService.getAliasByGuildIdAndSingular(null, "big").isEmpty());
    assertTrue(aliasesService.getAliasByGuildIdAndSingular("g1", null).isEmpty());
  }

  @Test
  void getAliasByGuildIdAndSingularReturnsFirstWhenFound() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByGuildIdAndSingular("g1", "big"))
      .thenReturn(List.of(entity));

    Optional<AliasEntity> result = aliasesService.getAliasByGuildIdAndSingular("g1", "big");

    assertTrue(result.isPresent());
    assertEquals("big", result.get().getSingular());
  }

  @Test
  void updateAliasReturnsEmptyWhenIdNull() {
    AliasDto dto = new AliasDto("g1", "big", "bigs", "XNO", "100", ":big:");
    assertTrue(aliasesService.updateAlias(null, dto).isEmpty());
    verify(aliasesRepository, never()).findById(any());
  }

  @Test
  void updateAliasReturnsEmptyWhenNotFound() {
    AliasDto dto = new AliasDto("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findById("missing")).thenReturn(Optional.empty());

    assertTrue(aliasesService.updateAlias("missing", dto).isEmpty());
  }

  @Test
  void updateAliasReturnsUpdatedWhenFound() {
    AliasEntity existing = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    existing.setId("id-1");
    AliasDto dto = new AliasDto("g1", "bigger", "biggers", "XNO", "200", ":bigger:");
    when(aliasesRepository.findById("id-1")).thenReturn(Optional.of(existing));
    when(aliasesRepository.save(any(AliasEntity.class))).thenAnswer(inv -> inv.getArgument(0));

    Optional<AliasEntity> result = aliasesService.updateAlias("id-1", dto);

    assertTrue(result.isPresent());
    assertEquals("bigger", result.get().getSingular());
    assertEquals("200", result.get().getValue());
  }

  @Test
  void deleteAliasReturnsEmptyWhenIdNull() {
    assertTrue(aliasesService.deleteAlias(null).isEmpty());
    verify(aliasesRepository, never()).deleteById(any());
  }

  @Test
  void deleteAliasReturnsEntityWhenFound() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    entity.setId("id-1");
    when(aliasesRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<AliasEntity> result = aliasesService.deleteAlias("id-1");

    assertTrue(result.isPresent());
    verify(aliasesRepository).deleteById("id-1");
  }

  @Test
  void deleteAliasReturnsEmptyWhenNotFound() {
    when(aliasesRepository.findById("missing")).thenReturn(Optional.empty());

    assertTrue(aliasesService.deleteAlias("missing").isEmpty());
  }

  @Test
  void setAliasesPassesDtosToConsumer() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByGuildId("g1")).thenReturn(List.of(entity));

    List<AliasDto> captured = new ArrayList<>();
    Consumer<List<AliasDto>> consumer = captured::addAll;
    aliasesService.setAliases(consumer, "g1");

    assertEquals(1, captured.size());
    assertEquals("big", captured.get(0).getSingular());
  }

  @Test
  void getAliasesByEitherGuildIdReturnsAllWhenBothNull() {
    List<AliasEntity> all = List.of(new AliasEntity());
    when(aliasesRepository.findAll()).thenReturn(all);

    List<AliasEntity> result = aliasesService.getAliasesByEitherGuildId(null, null);

    assertEquals(1, result.size());
    verify(aliasesRepository).findAll();
  }

  @Test
  void getAliasesByEitherGuildIdReturnsByEitherGuild() {
    AliasEntity entity = createAliasEntity("g1", "big", "bigs", "XNO", "100", ":big:");
    when(aliasesRepository.findByEitherGuildId("g1", "GLOBAL"))
      .thenReturn(List.of(entity));

    List<AliasEntity> result = aliasesService.getAliasesByEitherGuildId("g1", "GLOBAL");

    assertEquals(1, result.size());
    verify(aliasesRepository).findByEitherGuildId("g1", "GLOBAL");
  }

  private AliasEntity createAliasEntity(
    String guildId,
    String singular,
    String plural,
    String ticker,
    String value,
    String emoji
  ) {
    AliasEntity e = new AliasEntity();
    e.setGuildId(guildId);
    e.setSingular(singular);
    e.setPlural(plural);
    e.setTicker(ticker);
    e.setValue(value);
    e.setEmoji(emoji);
    return e;
  }
}
