package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AnglerDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.FishingReminderResponseDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.util.List;
import java.util.stream.Collectors;
import org.springframework.stereotype.Service;

@Service
public class FishingReminderServiceImpl implements FishingReminderService {

  private AnglersService anglersService;

  private CommandsService commandsService;

  private final FileLogger fileLogger;

  private GuildConfigurationsService guildConfigurationsService;

  public FishingReminderServiceImpl(
    AnglersService anglersService,
    CommandsService commandsService,
    GuildConfigurationsService guildConfigurationsService
  ) {
    this.anglersService = anglersService;
    this.commandsService = commandsService;
    this.fileLogger = new FileLogger("FishingReminderService");
    this.guildConfigurationsService = guildConfigurationsService;
  }

  @Override
  public FishingReminderResponseDto getFishingReminder() {
    try {
      FishingReminderResponseDto fishingReminderResponseDto =
        new FishingReminderResponseDto();

      // Convert CommandEntity -> CommandDto
      List<CommandDto> commandDtos = commandsService
        .getCommands(null)
        .stream()
        .map(CommandDto::new)
        .collect(Collectors.toList());

      // Convert GuildConfigurationsEntity -> GuildConfigurationsDto
      List<GuildConfigurationsDto> guildConfigDtos = guildConfigurationsService
        .getGuildConfigurations(null)
        .stream()
        .map(GuildConfigurationsDto::new)
        .collect(Collectors.toList());

      // Convert AnglerEntity -> AnglerDto
      List<AnglerDto> anglerDtos = anglersService
        .getRestingAnglers()
        .stream()
        .map(AnglerDto::new)
        .collect(Collectors.toList());

      // Set converted DTOs
      fishingReminderResponseDto.setCommands(commandDtos);
      fishingReminderResponseDto.setAllGuildConfigurations(guildConfigDtos);
      fishingReminderResponseDto.setAnglers(anglerDtos);

      return fishingReminderResponseDto;
    } catch (Exception e) {
      fileLogger.error("Error fetching fishing reminders: " + e.getMessage());
      throw e;
    }
  }
}
