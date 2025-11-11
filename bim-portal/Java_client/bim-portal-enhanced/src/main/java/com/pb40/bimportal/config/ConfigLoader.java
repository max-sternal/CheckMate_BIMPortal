package com.pb40.bimportal.config;

import io.github.cdimascio.dotenv.Dotenv;
import java.io.File;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Configuration loader for environment variables and .env files.
 *
 * <p>This class handles loading configuration from multiple sources with hierarchical search: 1.
 * Environment variables (highest priority) 2. .env file in current directory 3. .env file in parent
 * directory (for subprojects) 4. .env file in project root 5. System properties 6. Default values
 * (lowest priority)
 */
public class ConfigLoader {

  private static final Logger logger = LoggerFactory.getLogger(ConfigLoader.class);
  private static Dotenv dotenv;
  private static File foundEnvFile;

  static {
    initializeDotenv();
  }

  /** Initialize the dotenv loader with hierarchical search. */
  private static void initializeDotenv() {
    try {
      foundEnvFile = findEnvFile();

      if (foundEnvFile != null && foundEnvFile.exists()) {
        String parentDir = foundEnvFile.getParent();
        String fileName = foundEnvFile.getName();

        dotenv =
            Dotenv.configure()
                .directory(parentDir)
                .filename(fileName)
                .ignoreIfMalformed()
                .ignoreIfMissing()
                .load();

        logger.info("Loaded configuration from: {}", foundEnvFile.getAbsolutePath());
      } else {
        // Create empty dotenv if no file exists
        dotenv = Dotenv.configure().ignoreIfMissing().load();
        logger.debug("No .env file found, using environment variables only");
      }
    } catch (Exception e) {
      logger.warn("Failed to load .env file: " + e.getMessage());
      dotenv = null;
    }
  }

  /**
   * Find .env file using hierarchical search strategy. Searches in this order: 1. Current working
   * directory 2. Parent directory (one level up) 3. Two levels up (project root for subprojects) 4.
   * Three levels up (just in case)
   *
   * @return File object for .env file, or null if not found
   */
  private static File findEnvFile() {
    Path currentPath = Paths.get(System.getProperty("user.dir"));

    // Define search locations in order of preference
    List<Path> searchPaths = new ArrayList<>();
    searchPaths.add(currentPath); // Current directory
    searchPaths.add(currentPath.getParent()); // One level up
    if (currentPath.getParent() != null) {
      Path grandParent = currentPath.getParent().getParent();
      if (grandParent != null) {
        searchPaths.add(grandParent); // Two levels up (likely project root)
        Path greatGrandParent = grandParent.getParent();
        if (greatGrandParent != null) {
          searchPaths.add(greatGrandParent); // Three levels up
        }
      }
    }

    // Search for .env file in each location
    for (Path searchPath : searchPaths) {
      if (searchPath == null) continue;

      File envFile = searchPath.resolve(".env").toFile();
      if (envFile.exists() && envFile.isFile() && envFile.canRead()) {
        logger.debug("Found .env file at: {}", envFile.getAbsolutePath());
        return envFile;
      }
    }

    logger.debug("No .env file found in search paths: {}", searchPaths);
    return null;
  }

  /**
   * Get a property value from various sources (environment, .env file, system properties).
   *
   * <p>Priority order: 1. Environment variables (highest priority) 2. .env file (hierarchical
   * search) 3. System properties 4. Default value (lowest priority)
   *
   * @param key Property key
   * @return Property value or null if not found
   */
  public static String getProperty(String key) {
    return getProperty(key, null);
  }

  /**
   * Get a property value with a default fallback.
   *
   * @param key Property key
   * @param defaultValue Default value if property not found
   * @return Property value or default value
   */
  public static String getProperty(String key, String defaultValue) {
    // 1. Check environment variables first (highest priority)
    if (key == null || key.trim().isEmpty()) {
      return defaultValue;
    }

    String value = System.getenv(key);
    if (value != null && !value.trim().isEmpty()) {
      logger.trace("Found {} in environment variables", key);
      return value.trim();
    }

    // 2. Check .env file
    if (dotenv != null) {
      value = dotenv.get(key);
      if (value != null && !value.trim().isEmpty()) {
        logger.trace(
            "Found {} in .env file: {}",
            key,
            foundEnvFile != null ? foundEnvFile.getAbsolutePath() : "unknown");
        return value.trim();
      }
    }

    // 3. Check system properties
    value = System.getProperty(key);
    if (value != null && !value.trim().isEmpty()) {
      logger.trace("Found {} in system properties", key);
      return value.trim();
    }

    // 4. Return default value
    if (defaultValue != null) {
      logger.trace("Using default value for {}: {}", key, defaultValue);
    }
    return defaultValue;
  }

  /**
   * Get an integer property with default value.
   *
   * @param key Property key
   * @param defaultValue Default value if property not found or invalid
   * @return Property value as integer
   */
  public static int getIntProperty(String key, int defaultValue) {
    String value = getProperty(key);
    if (value != null) {
      try {
        return Integer.parseInt(value);
      } catch (NumberFormatException e) {
        logger.warn(
            "Invalid integer value for {}: {}. Using default: {}", key, value, defaultValue);
      }
    }
    return defaultValue;
  }

  /**
   * Get a boolean property with default value.
   *
   * @param key Property key
   * @param defaultValue Default value if property not found
   * @return Property value as boolean
   */
  public static boolean getBooleanProperty(String key, boolean defaultValue) {
    String value = getProperty(key);
    if (value != null) {
      return Boolean.parseBoolean(value);
    }
    return defaultValue;
  }

  /**
   * Check if a property exists in any configuration source.
   *
   * @param key Property key
   * @return True if property exists
   */
  public static boolean hasProperty(String key) {
    return getProperty(key) != null;
  }

  /** Reload the .env file (useful for testing or dynamic configuration). */
  public static void reloadConfig() {
    logger.info("Reloading configuration...");
    initializeDotenv();
  }

  /**
   * Get the path of the currently loaded .env file.
   *
   * @return Path to .env file or null if none found
   */
  public static String getEnvFilePath() {
    return foundEnvFile != null ? foundEnvFile.getAbsolutePath() : null;
  }

  /** Display available configuration sources for debugging. */
  public static void displayConfigSources() {
    System.out.println("--- Configuration Sources ---");

    if (foundEnvFile != null && foundEnvFile.exists()) {
      System.out.println(".env file:         Found");
      System.out.println(".env file path:    " + foundEnvFile.getAbsolutePath());
    } else {
      System.out.println(".env file:         Not found");

      // Show search paths for debugging
      Path currentPath = Paths.get(System.getProperty("user.dir"));
      System.out.println("Searched locations:");
      System.out.println("  - " + currentPath.resolve(".env"));
      if (currentPath.getParent() != null) {
        System.out.println("  - " + currentPath.getParent().resolve(".env"));
        if (currentPath.getParent().getParent() != null) {
          System.out.println("  - " + currentPath.getParent().getParent().resolve(".env"));
        }
      }
    }

    System.out.println("Environment vars:  Available");
    System.out.println("System properties: Available");
    System.out.println("Current directory: " + System.getProperty("user.dir"));
    System.out.println("-----------------------------");
  }

  /**
   * Create a sample .env file in the project root for reference.
   *
   * @param projectRootPath Path to project root directory
   * @return True if file was created successfully
   */
  public static boolean createSampleEnvFile(String projectRootPath) {
    try {
      Path envPath = Paths.get(projectRootPath, ".env");
      File envFile = envPath.toFile();

      if (envFile.exists()) {
        logger.warn(".env file already exists at: {}", envFile.getAbsolutePath());
        return false;
      }

      // Create sample content
      String sampleContent =
          """
                    # BIM Portal API Credentials
                    # Fill in your credentials below

                    # Required credentials
                    BIM_PORTAL_USERNAME=your_username_here
                    BIM_PORTAL_PASSWORD=your_password_here

                    # Optional configuration
                    BIM_PORTAL_BASE_URL=https://via.bund.de/bmdv/bim-portal/edu/bim
                    EXPORT_DIRECTORY=exports
                    REQUEST_TIMEOUT=30
                    MAX_RETRIES=3
                    LOG_LEVEL=INFO

                    # Advanced configuration
                    MAX_PROJECTS_TO_TEST=10
                    VERIFY_SSL=true
                    """;

      java.nio.file.Files.write(envPath, sampleContent.getBytes());
      logger.info("Created sample .env file at: {}", envFile.getAbsolutePath());
      return true;

    } catch (Exception e) {
      logger.error("Failed to create sample .env file: {}", e.getMessage());
      return false;
    }
  }
}
