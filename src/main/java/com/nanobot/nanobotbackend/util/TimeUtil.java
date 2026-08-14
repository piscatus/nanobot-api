package com.nanobot.nanobotbackend.util;

public class TimeUtil {

    public static String formatTimeRemaining(long remaining) {
        long days = remaining / 86400000L; // 24 * 60 * 60 * 1000
        long hours = (remaining % 86400000L) / 3600000L;
        long minutes = (remaining % 3600000L) / 60000L;
        long seconds = (remaining % 60000L) / 1000L;

        StringBuilder timeLeft = new StringBuilder();

        if (days > 0) {
            timeLeft.append(days).append(" day");
            if (days > 1) {
                timeLeft.append("s");
            }
            timeLeft.append(hours > 0 || minutes > 0 || seconds > 0 ? ", " : "");
        }

        if (hours > 0) {
            timeLeft.append(hours).append(" hour");
            if (hours > 1) {
                timeLeft.append("s");
            }
            timeLeft.append(minutes > 0 || seconds > 0 ? ", " : "");
        }

        if (minutes > 0) {
            timeLeft.append(minutes).append(" minute");
            if (minutes > 1) {
                timeLeft.append("s");
            }
            timeLeft.append(seconds > 0 ? " and " : "");
        }

        if (seconds > 0) {
            timeLeft.append(seconds).append(" second");
            if (seconds > 1) {
                timeLeft.append("s");
            }
        }

        // Edge case: everything is zero (rounding)
        if (days == 0 && hours == 0 && minutes == 0 && seconds == 0) {
            timeLeft.append("1 second");
        }

        return timeLeft.toString();
    }
}
