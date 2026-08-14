package com.nanobot.nanobotbackend.util;

import com.nanobot.nanobotbackend.dto.ItemDto;
import com.nanobot.nanobotbackend.dto.RequestDto;
import com.nanobot.nanobotbackend.dto.WalletDto;
import com.nanobot.nanobotbackend.entity.UserItemsEntity;
import com.nanobot.nanobotbackend.entity.UserWalletsEntity;
import com.nanobot.nanobotbackend.entity.GuildConfigurationsEntity;
import com.nanobot.nanobotbackend.entity.GuildWalletsEntity;
import com.nanobot.nanobotbackend.task.FileLogger;
import java.io.StringWriter;
import java.io.PrintWriter;
import java.math.BigInteger;
import java.util.List;
import java.util.Map;
import java.util.Set;

public class LoggingUtil {

    private static final FileLogger fileLogger = 
        new FileLogger("LoggingUtil");

    public static void error(String message) {
        fileLogger.error(message);
    }

    public static void info(String message) {
        fileLogger.info(message);
    }

    public static void warn(String message) {
        fileLogger.warn(message);
    }

    public static void requestLogging(String requestName, RequestDto requestDto) {
        fileLogger.info("---RequestsService : REQUEST (" + requestName + ")");
        fileLogger.info("  --RequestDto : " + (requestDto != null ? requestDto.toJson(false) : "null"));

    }

    public static void errorLogging(String command, Exception e) {
        StringWriter sw = new StringWriter();
        PrintWriter pw = new PrintWriter(sw);
        e.printStackTrace(pw);
        String stackTrace = sw.toString();
        fileLogger.error("Error executing " + command + " request: " + stackTrace);
    }

    public static void logCurrentGuildWallets(Set<GuildWalletsEntity> currentGuildWallets) {
        fileLogger.info("  -Guild Wallets Status");
        logGuildWalletsEntities(currentGuildWallets);
    }

    public static void logCurrentItems(Set<UserItemsEntity> currentUserItems) {
        fileLogger.info("  -User Items Status");
        logUserItemsEntities(currentUserItems);
    }

    public static void logCurrentWallets(Set<UserWalletsEntity> currentUserWallets) {
        fileLogger.info("  -User Wallets Status");
        logUserWalletsEntities(currentUserWallets);
    }

    public static void logGuildWalletsEntities(Set<GuildWalletsEntity> currentWallets) {
        if (currentWallets != null) {
            for (GuildWalletsEntity currentWallet : currentWallets) {
                fileLogger.info("Guild: " + currentWallet.getGuildId());
                List<WalletDto> currentWalletlist = currentWallet.getWallets();
                for (int i = 0; i < currentWalletlist.size(); i++) {
                    fileLogger.info(currentWalletlist.get(i).toJson(true));
                }
            }
        }
    }

    private static void logUserItemsEntities(Set<UserItemsEntity> currentItems) {
        if (currentItems != null) {
            for (UserItemsEntity currentItem : currentItems) {
                fileLogger.info("User: " + currentItem.getUserId());
                List<ItemDto> currentItemlist = currentItem.getItems();
                for (int i = 0; i < currentItemlist.size(); i++) {
                    fileLogger.info(currentItemlist.get(i).toJson(false));
                }
            }
        }
    }

    private static void logUserWalletsEntities(Set<UserWalletsEntity> currentWallets) {
        if (currentWallets != null) {
            for (UserWalletsEntity currentWallet : currentWallets) {
                fileLogger.info("User: " + currentWallet.getUserId());
                List<WalletDto> currentWalletlist = currentWallet.getWallets();
                for (int i = 0; i < currentWalletlist.size(); i++) {
                    fileLogger.info(currentWalletlist.get(i).toJson(false));
                }
            }
        }
    }
}
