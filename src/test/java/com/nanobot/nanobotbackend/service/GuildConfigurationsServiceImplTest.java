package com.nanobot.nanobotbackend.service;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.repository.GuildConfigurationsRepository;
import java.util.Collections;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DuplicateKeyException;

@ExtendWith(MockitoExtension.class)
class GuildConfigurationsServiceImplTest {

  @Mock
  private GuildConfigurationsRepository guildConfigurationsRepository;

  private GuildConfigurationsServiceImpl guildConfigurationsService;

  @BeforeEach
  void setUp() {
    guildConfigurationsService = new GuildConfigurationsServiceImpl(guildConfigurationsRepository);
  }

  @Test
  void setGuildConfigurationsShouldReturnTrueWhenGuildIdValidAndActive() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setGuildId("guild1");
    entity.setStatus(StatusDto.ACTIVE);
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(List.of(entity));

    boolean[] accepted = { false };
    boolean result = guildConfigurationsService.setGuildConfigurations(
      "guild1",
      dto -> {
        accepted[0] = true;
        assertEquals(StatusDto.ACTIVE, dto.getStatus());
      }
    );

    assertTrue(result);
    assertTrue(accepted[0]);
    verify(guildConfigurationsRepository).findByGuildId("guild1");
  }

  @Test
  void setGuildConfigurationsShouldReturnTrueWhenGuildIdNull() {
    boolean result = guildConfigurationsService.setGuildConfigurations(null, dto -> {});

    assertTrue(result);
    verify(guildConfigurationsRepository, never()).findByGuildId(any());
  }

  @Test
  void createGuildConfigurationShouldReturnPresentWhenSuccess() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setGuildId("guild1");
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity(dto);
    entity.setId("id-1");
    when(guildConfigurationsRepository.insert(any(GuildConfigurationsEntity.class))).thenReturn(entity);

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.createGuildConfiguration(dto);

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(guildConfigurationsRepository).insert(any(GuildConfigurationsEntity.class));
  }

  @Test
  void createGuildConfigurationShouldReturnEmptyWhenDuplicateKey() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setGuildId("guild1");
    when(guildConfigurationsRepository.insert(any(GuildConfigurationsEntity.class)))
      .thenThrow(new DuplicateKeyException("duplicate"));

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.createGuildConfiguration(dto);

    assertTrue(result.isEmpty());
    verify(guildConfigurationsRepository).insert(any(GuildConfigurationsEntity.class));
  }

  @Test
  void getGuildConfigurationsShouldReturnListWhenGuildIdProvided() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("id-1");
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(List.of(entity));

    List<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurations("guild1");

    assertFalse(result.isEmpty());
    assertEquals("id-1", result.get(0).getId());
    verify(guildConfigurationsRepository).findByGuildId("guild1");
  }

  @Test
  void getGuildConfigurationsShouldReturnAllWhenGuildIdNull() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    when(guildConfigurationsRepository.findAll()).thenReturn(List.of(entity));

    List<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurations(null);

    assertFalse(result.isEmpty());
    verify(guildConfigurationsRepository).findAll();
  }

  @Test
  void getGuildConfigurationByIdShouldReturnPresentWhenFound() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("id-1");
    when(guildConfigurationsRepository.findById("id-1")).thenReturn(Optional.of(entity));

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurationById("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(guildConfigurationsRepository).findById("id-1");
  }

  @Test
  void getGuildConfigurationByIdShouldReturnEmptyWhenNotFound() {
    when(guildConfigurationsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurationById("missing");

    assertTrue(result.isEmpty());
    verify(guildConfigurationsRepository).findById("missing");
  }

  @Test
  void getGuildConfigurationByGuildIdShouldReturnPresentWhenFound() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("id-1");
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(List.of(entity));

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurationByGuildId("guild1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(guildConfigurationsRepository).findByGuildId("guild1");
  }

  @Test
  void getGuildConfigurationByGuildIdShouldReturnEmptyWhenNotFound() {
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(Collections.emptyList());

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.getGuildConfigurationByGuildId("guild1");

    assertTrue(result.isEmpty());
    verify(guildConfigurationsRepository).findByGuildId("guild1");
  }

  @Test
  void updateGuildConfigurationShouldReturnUpdatedWhenFound() {
    GuildConfigurationsEntity existing = new GuildConfigurationsEntity();
    existing.setId("id-1");
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    dto.setFishingChannelId("ch1");
    when(guildConfigurationsRepository.findById("id-1")).thenReturn(Optional.of(existing));
    when(guildConfigurationsRepository.save(any(GuildConfigurationsEntity.class))).thenReturn(existing);

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.updateGuildConfiguration("id-1", dto);

    assertTrue(result.isPresent());
    verify(guildConfigurationsRepository).findById("id-1");
    verify(guildConfigurationsRepository).save(any(GuildConfigurationsEntity.class));
  }

  @Test
  void updateGuildConfigurationShouldReturnEmptyWhenNotFound() {
    GuildConfigurationsDto dto = new GuildConfigurationsDto();
    when(guildConfigurationsRepository.findById("missing")).thenReturn(Optional.empty());

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.updateGuildConfiguration("missing", dto);

    assertTrue(result.isEmpty());
    verify(guildConfigurationsRepository).findById("missing");
    verify(guildConfigurationsRepository, never()).save(any());
  }

  @Test
  void deleteGuildConfigurationShouldReturnDeletedWhenFound() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("id-1");
    when(guildConfigurationsRepository.findById("id-1")).thenReturn(Optional.of(entity));
    doNothing().when(guildConfigurationsRepository).deleteById("id-1");

    Optional<GuildConfigurationsEntity> result = guildConfigurationsService.deleteGuildConfiguration("id-1");

    assertTrue(result.isPresent());
    assertEquals("id-1", result.get().getId());
    verify(guildConfigurationsRepository).findById("id-1");
    verify(guildConfigurationsRepository).deleteById("id-1");
  }

  @Test
  void getOrCreateGuildConfigurationShouldReturnExistingWhenFound() {
    GuildConfigurationsEntity entity = new GuildConfigurationsEntity();
    entity.setId("id-1");
    entity.setGuildId("guild1");
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(List.of(entity));

    GuildConfigurationsEntity result = guildConfigurationsService.getOrCreateGuildConfiguration("guild1");

    assertEquals("id-1", result.getId());
    assertEquals("guild1", result.getGuildId());
    verify(guildConfigurationsRepository).findByGuildId("guild1");
    verify(guildConfigurationsRepository, never()).save(any());
  }

  @Test
  void getOrCreateGuildConfigurationShouldCreateWhenNotFound() {
    when(guildConfigurationsRepository.findByGuildId("guild1")).thenReturn(Collections.emptyList());
    GuildConfigurationsEntity created = new GuildConfigurationsEntity();
    created.setId("new-id");
    created.setGuildId("guild1");
    when(guildConfigurationsRepository.save(any(GuildConfigurationsEntity.class))).thenReturn(created);

    GuildConfigurationsEntity result = guildConfigurationsService.getOrCreateGuildConfiguration("guild1");

    assertNotNull(result);
    assertEquals("new-id", result.getId());
    verify(guildConfigurationsRepository).findByGuildId("guild1");
    verify(guildConfigurationsRepository).save(any(GuildConfigurationsEntity.class));
  }
}
