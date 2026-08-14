package com.nanobot.nanobotbackend.util;

public class StringUtil {

    public static boolean isValidString(String variable) {
      return variable != null 
          && !variable.equalsIgnoreCase("null") 
          && !variable.equals("") 
          && !variable.equals(" ");
    }
}
