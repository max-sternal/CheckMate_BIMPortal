package com.bimportal.client.util;

import io.github.cdimascio.dotenv.Dotenv;

public class TestConfig {
  private static final Dotenv dotenv;

  static {
    dotenv =
        Dotenv.configure()
            .directory(System.getProperty("user.dir"))
            .ignoreIfMalformed()
            .ignoreIfMissing()
            .load();
  }

  public static String getUsername() {
    return dotenv.get("BIM_PORTAL_USERNAME", "");
  }

  public static String getPassword() {
    return dotenv.get("BIM_PORTAL_PASSWORD", "");
  }

  public static boolean hasCredentials() {
    String username = getUsername();
    String password = getPassword();
    return username != null
        && !username.trim().isEmpty()
        && password != null
        && !password.trim().isEmpty();
  }
}
