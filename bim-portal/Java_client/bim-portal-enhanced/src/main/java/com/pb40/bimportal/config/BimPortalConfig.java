package com.pb40.bimportal.config;

import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;

/**
 * Centralized configuration for BIM Portal API enhanced client.
 *
 * <p>This class provides configuration management for the enhanced client that wraps the generated
 * OpenAPI client code.
 */
public class BimPortalConfig {

  // === API Configuration ===
  private static String baseUrl = "https://via.bund.de/bim";

  // Authentication endpoints (relative to base URL)
  public static final String LOGIN_ENDPOINT = "/infrastruktur/api/v1/public/auth/login";
  public static final String REFRESH_ENDPOINT = "/infrastruktur/api/v1/public/auth/refresh-token";
  public static final String LOGOUT_ENDPOINT = "/infrastruktur/api/v1/public/auth/logout";

  // === Authentication Configuration ===
  public static final String USERNAME_ENV_VAR = "BIM_PORTAL_USERNAME";
  public static final String PASSWORD_ENV_VAR = "BIM_PORTAL_PASSWORD";
  public static final Duration TOKEN_REFRESH_MARGIN = Duration.ofSeconds(20);
  public static final int AUTH_RETRY_LIMIT = 1;

  // === HTTP Client Configuration ===
  public static final Duration REQUEST_TIMEOUT = Duration.ofSeconds(30);
  public static final Duration CONNECT_TIMEOUT = Duration.ofSeconds(10);
  public static final boolean VERIFY_SSL = true;

  // === Application Configuration ===
  public static final String DEFAULT_EXPORT_DIRECTORY = "exports";
  public static final int MAX_RETRIES = 3;
  public static final int MAX_PROJECTS_TO_TEST = 10;

  // === Static Configuration Access ===

  /**
   * Get the base URL for the BIM Portal API.
   *
   * @return The base URL
   */
  public static String getBaseUrl() {
    return ConfigLoader.getProperty("BIM_PORTAL_BASE_URL", baseUrl);
  }

  /**
   * Set a custom base URL for the BIM Portal API.
   *
   * @param url New base URL
   */
  public static void setBaseUrl(String url) {
    baseUrl = url;
  }

  /**
   * Get the complete login URL.
   *
   * @return Login URL
   */
  public static String getLoginUrl() {
    return getBaseUrl() + LOGIN_ENDPOINT;
  }

  /**
   * Get the complete refresh token URL.
   *
   * @return Refresh token URL
   */
  public static String getRefreshUrl() {
    return getBaseUrl() + REFRESH_ENDPOINT;
  }

  /**
   * Get the complete logout URL.
   *
   * @return Logout URL
   */
  public static String getLogoutUrl() {
    return getBaseUrl() + LOGOUT_ENDPOINT;
  }

  /**
   * Combine base URL with endpoint to create full URL.
   *
   * @param endpoint API endpoint (with or without leading slash)
   * @return Complete URL
   */
  public static String getFullUrl(String endpoint) {
    if (endpoint == null || endpoint.isEmpty()) {
      return getBaseUrl();
    }

    String normalizedEndpoint = endpoint.startsWith("/") ? endpoint : "/" + endpoint;
    return getBaseUrl() + normalizedEndpoint;
  }

  /**
   * Get username from environment variables.
   *
   * @return Username or null if not found
   */
  public static String getUsername() {
    return ConfigLoader.getProperty(USERNAME_ENV_VAR);
  }

  /**
   * Get password from environment variables.
   *
   * @return Password or null if not found
   */
  public static String getPassword() {
    return ConfigLoader.getProperty(PASSWORD_ENV_VAR);
  }

  /**
   * Check if credentials are available.
   *
   * @return True if both username and password are configured
   */
  public static boolean hasCredentials() {
    return getUsername() != null
        && getPassword() != null
        && !getUsername().trim().isEmpty()
        && !getPassword().trim().isEmpty();
  }

  /**
   * Get the export directory path.
   *
   * @return Path to export directory
   */
  public static Path getExportPath() {
    String exportDir = ConfigLoader.getProperty("EXPORT_DIRECTORY", DEFAULT_EXPORT_DIRECTORY);
    return Paths.get(exportDir);
  }

  /**
   * Get the full path for an export file.
   *
   * @param filename Name of the file to export
   * @return Path to the export file
   */
  public static Path getExportFilePath(String filename) {
    Path exportDir = getExportPath();
    exportDir.toFile().mkdirs(); // Create directory if it doesn't exist
    return exportDir.resolve(filename);
  }

  /**
   * Get request timeout configuration.
   *
   * @return Request timeout duration
   */
  public static Duration getRequestTimeout() {
    String timeoutStr = ConfigLoader.getProperty("REQUEST_TIMEOUT");
    if (timeoutStr != null) {
      try {
        return Duration.ofSeconds(Long.parseLong(timeoutStr));
      } catch (NumberFormatException e) {
        // Fall back to default
      }
    }
    return REQUEST_TIMEOUT;
  }

  /**
   * Validate the current configuration.
   *
   * @return List of configuration issues (empty if valid)
   */
  public static List<String> validateConfig() {
    List<String> issues = new ArrayList<>();

    String baseUrlValue = getBaseUrl();
    if (baseUrlValue == null || baseUrlValue.trim().isEmpty()) {
      issues.add("BASE_URL is not set");
    } else if (!baseUrlValue.startsWith("http://") && !baseUrlValue.startsWith("https://")) {
      issues.add("BASE_URL must start with http:// or https://");
    }

    Duration timeout = getRequestTimeout();
    if (timeout.getSeconds() < 1) {
      issues.add("REQUEST_TIMEOUT must be at least 1 second");
    }

    String maxRetriesStr = ConfigLoader.getProperty("MAX_RETRIES");
    if (maxRetriesStr != null) {
      try {
        int maxRetries = Integer.parseInt(maxRetriesStr);
        if (maxRetries < 0 || maxRetries > 10) {
          issues.add("MAX_RETRIES should be between 0 and 10");
        }
      } catch (NumberFormatException e) {
        issues.add("MAX_RETRIES must be a valid integer");
      }
    }

    return issues;
  }

  /**
   * Display current configuration for debugging.
   *
   * @param showCredentials Whether to show credential status
   */
  public static void displayConfig(boolean showCredentials) {
    System.out.println("--- BIM Portal Enhanced Client Configuration ---");
    System.out.println("Base URL:          " + getBaseUrl());
    System.out.println("Login URL:         " + getLoginUrl());
    System.out.println("Export Directory:  " + getExportPath());
    System.out.println("Request Timeout:   " + getRequestTimeout().getSeconds() + "s");
    System.out.println("SSL Verification:  " + VERIFY_SSL);

    if (showCredentials) {
      if (hasCredentials()) {
        System.out.println("Username:          " + getUsername());
        System.out.println("Password:          [configured]"); //  + getPassword()
      } else {
        System.out.println("Credentials:       Not configured");
      }
    }

    // Check for configuration issues
    List<String> issues = validateConfig();
    if (!issues.isEmpty()) {
      System.out.println("\nConfiguration Issues:");
      for (String issue : issues) {
        System.out.println("  - " + issue);
      }
    } else {
      System.out.println("\nConfiguration: Valid");
    }
    System.out.println("-----------------------------------------------");
  }

  /** Display configuration without credential details. */
  public static void displayConfig() {
    displayConfig(false);
  }
}
