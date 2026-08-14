package com.nanobot.nanobotbackend.service;

import com.nanobot.nanobotbackend.dto.AliasDto;
import com.nanobot.nanobotbackend.dto.CommandDto;
import com.nanobot.nanobotbackend.dto.ConfigResponseDto;
import com.nanobot.nanobotbackend.dto.CurrencyDto;
import com.nanobot.nanobotbackend.dto.GuildConfigurationsDto;
import com.nanobot.nanobotbackend.dto.StatusDto;
import com.nanobot.nanobotbackend.entity.AliasEntity;
import com.nanobot.nanobotbackend.entity.CreatureEntity;
import com.nanobot.nanobotbackend.entity.CurrencyEntity;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.entity.ProfanityEntity;
import com.nanobot.nanobotbackend.util.Constants;
import com.nanobot.nanobotbackend.util.LoggingUtil;
import com.nanobot.nanobotbackend.util.NumberUtil;
import com.nanobot.nanobotbackend.util.StringUtil;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import java.util.stream.Collectors;
import org.bson.types.ObjectId;
import org.springframework.dao.DuplicateKeyException;
import org.springframework.stereotype.Service;

@Service
public class ConfigServicesImpl implements ConfigServices {

  public final CoreServices coreServices;

  public final AliasesService aliasesService;

  public final CurrenciesService currenciesService;

  public final CreaturesService creaturesService;

  public final ProfanitiesService profanitiesService;

  public final TransferService transferService;

  public ConfigServicesImpl(
    CoreServices coreServices,
    AliasesService aliasesService,
    CurrenciesService currenciesService,
    CreaturesService creaturesService,
    ProfanitiesService profanitiesService,
    TransferService transferService
  ) {
    this.coreServices = coreServices;
    this.aliasesService = aliasesService;
    this.currenciesService = currenciesService;
    this.creaturesService = creaturesService;
    this.profanitiesService = profanitiesService;
    this.transferService = transferService;
  }

  @Override
  public ConfigResponseDto config(GuildConfigurationsDto requestDto) {
    try {
      LoggingUtil.requestLogging(Constants.COMMAND_NAME_CONFIG, null);
      LoggingUtil.info(
        " --GuildConfigurationsDto: " + requestDto.toJson(false)
      );

      ConfigResponseDto configResponseDto = new ConfigResponseDto();

      if (
        !coreServices.guildConfigurationsService.setGuildConfigurations(
          requestDto.getGuildId(),
          configResponseDto::setGuildConfigurations
        )
      ) {
        return configResponseDto;
      }

      if (
        !coreServices.userDetailsService.setUserDetails(
          requestDto.getUserId(),
          configResponseDto::setUserDetails,
          true
        )
      ) {
        return configResponseDto;
      }

      if (
        !coreServices.commandsService.setCommands(
          Constants.COMMAND_NAME_CONFIG,
          requestDto.getUserId(),
          configResponseDto::setCommands
        )
      ) {
        return configResponseDto;
      }

      Map<String, String> commandMap = configResponseDto
        .getCommands()
        .stream()
        .collect(
          Collectors.toMap(CommandDto::getName, CommandDto::getCommandId)
        );

      currenciesService.setCurrencies(configResponseDto::setCurrencies);

      creaturesService.setCreatures(configResponseDto::setCreatures);

      if (requestDto.getAliasData() != null) {
        if (
          requestDto.getAliasData().getSingular() != null &&
          requestDto.getAliasData().getPlural() == null
        ) {
          // see if alias exists
          List<AliasEntity> serverAliases =
            aliasesService.getAliasesByEitherGuildId(
              null,
              requestDto.getGuildId()
            );

          for (int i = 0; i < serverAliases.size(); i++) {
            AliasEntity currentEnt = serverAliases.get(i);
            if (
              currentEnt
                .getSingular()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getSingular().trim()
                ) ||
              currentEnt
                .getPlural()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getSingular().trim()
                )
            ) {
              Optional<AliasEntity> aliasEntityOptional =
                aliasesService.deleteAlias(currentEnt.getId());

              if (aliasEntityOptional.isPresent()) {
                AliasEntity aliasEntity = aliasEntityOptional.get();
                configResponseDto.setAliasDetails(new AliasDto(aliasEntity));
              }

              return configResponseDto;
            }
          }

          configResponseDto.setErrorMessage(
            "Server Alias Not Found: " + requestDto.getAliasData().getSingular()
          );

          return configResponseDto;
        } else {
          // see if server aliases will exceed 25
          List<AliasEntity> serverAliases =
            aliasesService.getAliasesByEitherGuildId(
              null,
              requestDto.getGuildId()
            );
          if (serverAliases.size() == 25) {
            configResponseDto.setErrorMessage(
              "Servers Cannot Exceed 25 Custom Aliases!"
            );
            return configResponseDto;
          }

          String aliasValueRaw = requestDto.getAliasData().getValue();
          if (aliasValueRaw != null && aliasValueRaw.contains("+")) {
            configResponseDto.setErrorMessage(
              "Alias values cannot combine amounts with `+`. Use a single amount, such as `$2 ban` or `.1 nano`."
            );
            return configResponseDto;
          }

          String inputError1 = "Your alias name was flagged as a ";
          String inputError2 = ", the alias cannot be created.";
          String inputError3 = " and is against our terms of service";
          // see if singular and plural are not currencies
          List<CurrencyEntity> allCurrencies = currenciesService.getCurrencies(
            null
          );
          for (int i = 0; i < allCurrencies.size(); i++) {
            CurrencyEntity currency = allCurrencies.get(i);
            if (
              requestDto
                .getAliasData()
                .getSingular()
                .trim()
                .equalsIgnoreCase(currency.getTicker()) ||
              requestDto
                .getAliasData()
                .getSingular()
                .trim()
                .equalsIgnoreCase(currency.getName()) ||
              requestDto
                .getAliasData()
                .getPlural()
                .trim()
                .equalsIgnoreCase(currency.getTicker()) ||
              requestDto
                .getAliasData()
                .getPlural()
                .trim()
                .equalsIgnoreCase(currency.getName())
            ) {
              configResponseDto.setErrorMessage(
                inputError1 + "currency" + inputError2
              );
              return configResponseDto;
            }
          }

          // see if singular and plural are not numbers
          if (
            requestDto.getAliasData().getSingular().trim().equals("0") ||
            NumberUtil.validateNumber(
              requestDto.getAliasData().getSingular().trim(),
              null,
              0,
              new AtomicReference<>()::set,
              false
            ) ||
            NumberUtil.validateNumber(
              requestDto.getAliasData().getPlural().trim(),
              null,
              0,
              new AtomicReference<>()::set,
              false
            )
          ) {
            configResponseDto.setErrorMessage(
              inputError1 + "number" + inputError2
            );
            return configResponseDto;
          }

          // see if singular and plural are not creatures
          List<CreatureEntity> allCreatures = creaturesService.getCreatures();
          for (int i = 0; i < allCreatures.size(); i++) {
            CreatureEntity creature = allCreatures.get(i);
            if (
              requestDto
                .getAliasData()
                .getSingular()
                .trim()
                .equalsIgnoreCase(creature.getName()) ||
              requestDto
                .getAliasData()
                .getSingular()
                .trim()
                .equalsIgnoreCase(creature.getPluralization()) ||
              requestDto
                .getAliasData()
                .getPlural()
                .trim()
                .equalsIgnoreCase(creature.getName()) ||
              requestDto
                .getAliasData()
                .getPlural()
                .trim()
                .equalsIgnoreCase(creature.getPluralization())
            ) {
              configResponseDto.setErrorMessage(
                inputError1 + "creature" + inputError2
              );
              return configResponseDto;
            }
          }

          // see if singular, plural, and emoji pass the profanity check
          List<ProfanityEntity> allProfanities =
            profanitiesService.getProfanities(null);

          for (int i = 0; i < allProfanities.size(); i++) {
            ProfanityEntity currentEnt = allProfanities.get(i);
            if (currentEnt.getContains()) {
              if (
                requestDto
                  .getAliasData()
                  .getSingular()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getPlural()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getEmoji()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getSingular()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getPlural()) ||
                requestDto
                  .getAliasData()
                  .getPlural()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getPlural()) ||
                requestDto
                  .getAliasData()
                  .getEmoji()
                  .trim()
                  .toLowerCase()
                  .contains(currentEnt.getPlural())
              ) {
                configResponseDto.setErrorMessage(
                  inputError1 + "profanity" + inputError3 + inputError2
                );
                return configResponseDto;
              }
            } else {
              if (
                requestDto
                  .getAliasData()
                  .getSingular()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getPlural()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getEmoji()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getSingular()) ||
                requestDto
                  .getAliasData()
                  .getSingular()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getPlural()) ||
                requestDto
                  .getAliasData()
                  .getPlural()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getPlural()) ||
                requestDto
                  .getAliasData()
                  .getEmoji()
                  .trim()
                  .equalsIgnoreCase(currentEnt.getPlural())
              ) {
                configResponseDto.setErrorMessage(
                  inputError1 + "profanity" + inputError3 + inputError2
                );
                return configResponseDto;
              }
            }
          }

          configResponseDto.setTransfer(
            transferService.processInputs(
              Constants.COMMAND_NAME_CONFIG,
              configResponseDto::setErrorMessage,
              commandMap,
              requestDto.getGuildId(),
              requestDto.getUserId(),
              requestDto.getAliasData().getValue(),
              true
            )
          );

          if (configResponseDto.getErrorMessage() != null) {
            return configResponseDto;
          }

          // see if there is only one currency in the input and no items
          if (configResponseDto.getTransfer().getItems().size() > 0) {
            configResponseDto.setErrorMessage(
              "Aliases can only be configured as a single amount of currency!"
            );
            return configResponseDto;
          } else if (configResponseDto.getTransfer().getWallets().size() > 1) {
            configResponseDto.setErrorMessage(
              "Aliases can only be configured as a single amount of currency!"
            );
            return configResponseDto;
          }

          // see if alias is already taken
          List<AliasEntity> allAliases =
            aliasesService.getAliasesByEitherGuildId(
              "GLOBAL",
              requestDto.getGuildId()
            );

          for (int i = 0; i < allAliases.size(); i++) {
            AliasEntity currentEnt = allAliases.get(i);
            if (
              currentEnt
                .getSingular()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getSingular().trim()
                ) ||
              currentEnt
                .getSingular()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getPlural().trim()
                ) ||
              currentEnt
                .getPlural()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getSingular().trim()
                ) ||
              currentEnt
                .getPlural()
                .equalsIgnoreCase(
                  requestDto.getAliasData().getPlural().trim()
                ) ||
              currentEnt
                .getEmoji()
                .equals(requestDto.getAliasData().getEmoji().trim())
            ) {
              configResponseDto.setErrorMessage(
                "Duplicate Alias Found: " +
                currentEnt.getSingular() +
                " (" +
                currentEnt.getPlural() +
                ")"
              );
              return configResponseDto;
            }
          }

          String ticker =
            configResponseDto.getTransfer().getWallets().get(0).getTicker();
          String aliasValueTrimmed =
            requestDto.getAliasData().getValue().trim();
          String storedRaw =
            configResponseDto.getTransfer().getWallets().get(0).getRaw();
          String storedInput = null;
          if (isDollarBasedAliasInput(aliasValueTrimmed, ticker)) {
            storedInput = aliasValueTrimmed;
            storedRaw = null;
          }
          AliasDto aliasDto = new AliasDto(
            requestDto.getGuildId(),
            requestDto.getUserId(),
            requestDto.getAliasData().getSingular().trim().toLowerCase(),
            requestDto.getAliasData().getPlural().trim().toLowerCase(),
            ticker,
            storedRaw,
            requestDto.getAliasData().getEmoji()
          );
          aliasDto.setInput(storedInput);

          Optional<AliasEntity> aliasEntityOptional =
            aliasesService.createAlias(aliasDto);

          if (aliasEntityOptional.isPresent()) {
            AliasEntity aliasEntity = aliasEntityOptional.get();
            configResponseDto.setAliasDetails(new AliasDto(aliasEntity));
          }

          return configResponseDto;
        }
      } else {
        List<GuildConfigurationsEntity> existingGuildConfiguration =
          coreServices.guildConfigurationsService.getGuildConfigurations(
            requestDto.getGuildId()
          );
        if (existingGuildConfiguration.size() > 1) {
          throw new Error(
            "Multiple guild configurations found with the same guildId"
          );
        } else if (!existingGuildConfiguration.isEmpty()) {
          GuildConfigurationsEntity guildConfig =
            existingGuildConfiguration.get(0);
          if (requestDto.getGuildId() != null) {
            guildConfig.setGuildId(requestDto.getGuildId());
          }
          if (requestDto.getFishingLoggingChannelId() != null) {
            guildConfig.setFishingLoggingChannelId(
              requestDto.getFishingLoggingChannelId()
            );
          }
          if (requestDto.getTransferLoggingChannelId() != null) {
            guildConfig.setTransferLoggingChannelId(
              requestDto.getTransferLoggingChannelId()
            );
          }
          if (requestDto.getFishingChannelId() != null) {
            guildConfig.setFishingChannelId(requestDto.getFishingChannelId());
          }
          if (requestDto.getFishingRole() != null) {
            guildConfig.setFishingRole(requestDto.getFishingRole());
          }
          if (requestDto.getFishingBypassRoles() != null) {
            List<String> existingBypassRoles =
              guildConfig.getFishingBypassRoles();
            String newRole = requestDto.getFishingBypassRoles().get(0);
            boolean isNewRoleEmpty = newRole.equals("0");

            // Scenario: The list exists
            if (existingBypassRoles != null) {
              // Check for the maximum number of configured bypass roles
              if (existingBypassRoles.size() > 9 && !isNewRoleEmpty) {
                configResponseDto.setGuildConfigurations(null);
                configResponseDto.setErrorMessage(
                  "The maximum number of configured bypass roles has already been reached!"
                );
                return configResponseDto;
              }

              // Check if the new role already exists
              if (!isNewRoleEmpty && existingBypassRoles.contains(newRole)) {
                configResponseDto.setGuildConfigurations(null);
                configResponseDto.setErrorMessage(
                  "Bypass role <@&" + newRole + "> already set!"
                );
                return configResponseDto;
              }

              // Update existing roles based on the new role
              if (isNewRoleEmpty) {
                // If an empty role is passed, set the bypass roles to contain only an empty string
                guildConfig.setFishingBypassRoles(Arrays.asList("0"));
              } else if (
                existingBypassRoles.size() == 1 &&
                existingBypassRoles.get(0).equals("0")
              ) {
                // If the existing list contains only role "0", replace it with the new role
                guildConfig.setFishingBypassRoles(Arrays.asList(newRole));
              } else {
                // Add the new role to the existing list
                existingBypassRoles.add(newRole);
                guildConfig.setFishingBypassRoles(existingBypassRoles);
              }
            } else {
              // Scenario: The list does not exist
              guildConfig.setFishingBypassRoles(Arrays.asList(newRole));
            }
          }

          if (requestDto.getFishingError() != null) {
            guildConfig.setFishingError(requestDto.getFishingError());
          }
          if (
            requestDto.getFishingFrequency() != null &&
            requestDto.getFishingFrequency() >= 0
          ) {
            guildConfig.setFishingFrequency(requestDto.getFishingFrequency());
          }
          if (
            requestDto.getMaximumMinutesActive() != null &&
            requestDto.getMaximumMinutesActive() >= 0
          ) {
            guildConfig.setMaximumMinutesActive(
              requestDto.getMaximumMinutesActive()
            );
          }
          if (
            requestDto.getMaximumActiveUsers() != null &&
            requestDto.getMaximumActiveUsers() >= 0
          ) {
            guildConfig.setMaximumActiveUsers(
              requestDto.getMaximumActiveUsers()
            );
          }

          GuildConfigurationsEntity updatedGuildConfiguration =
            coreServices.guildConfigurationsService.saveGuildConfigurations(
              guildConfig
            );
          configResponseDto.setGuildConfigurations(
            new GuildConfigurationsDto(updatedGuildConfiguration)
          );
        } else {
          GuildConfigurationsEntity guildConfigurationsEntity =
            new GuildConfigurationsEntity();
          guildConfigurationsEntity.setId(new ObjectId().toHexString());
          guildConfigurationsEntity.setStatus(StatusDto.ACTIVE);
          guildConfigurationsEntity.setGuildId(requestDto.getGuildId());
          guildConfigurationsEntity.setFishingLoggingChannelId(
            requestDto.getFishingLoggingChannelId()
          );
          guildConfigurationsEntity.setFishingChannelId(
            requestDto.getFishingChannelId()
          );
          guildConfigurationsEntity.setFishingRole(requestDto.getFishingRole());
          guildConfigurationsEntity.setFishingBypassRoles(
            requestDto.getFishingBypassRoles()
          );
          guildConfigurationsEntity.setFishingError(
            requestDto.getFishingError()
          );
          guildConfigurationsEntity.setFishingFrequency(
            requestDto.getFishingFrequency()
          );
          guildConfigurationsEntity.setMaximumActiveUsers(
            requestDto.getMaximumActiveUsers()
          );
          guildConfigurationsEntity.setMaximumMinutesActive(
            requestDto.getMaximumMinutesActive()
          );
          guildConfigurationsEntity.setTransferLoggingChannelId(
            requestDto.getTransferLoggingChannelId()
          );
          try {
            GuildConfigurationsEntity createdGuildConfiguration =
              coreServices.guildConfigurationsService.insertGuildConfigurations(
                guildConfigurationsEntity
              );
            configResponseDto.setGuildConfigurations(
              new GuildConfigurationsDto(createdGuildConfiguration)
            );
          } catch (DuplicateKeyException e) {
            throw new Error(
              "Guild Configuration already exists with specified guildId: " +
              requestDto.getGuildId()
            );
          }
        }
      }
      return configResponseDto;
    } catch (Exception e) {
      LoggingUtil.errorLogging(Constants.COMMAND_NAME_CONFIG, e);
      return new ConfigResponseDto(Constants.unknownError);
    }
  }

  private boolean isDollarBasedAliasInput(
    String aliasValue,
    String resolvedTicker
  ) {
    if (!StringUtil.isValidString(aliasValue) || resolvedTicker == null) {
      return false;
    }
    String[] tokens = aliasValue.trim().split(" ");
    if (tokens.length < 2) {
      return false;
    }
    AtomicReference<String> dollarAmount = new AtomicReference<>();
    if (!NumberUtil.validateDollarNumber(tokens[0], dollarAmount::set)) {
      return false;
    }
    CurrencyDto currency = currenciesService.analyzeCurrencies(tokens[1]);
    return currency != null
      && currency.getTicker().equalsIgnoreCase(resolvedTicker);
  }
}
