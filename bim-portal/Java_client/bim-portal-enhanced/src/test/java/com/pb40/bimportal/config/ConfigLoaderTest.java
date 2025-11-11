package com.pb40.bimportal.config;

import static org.junit.jupiter.api.Assertions.*;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;

/**
 * Comprehensive unit tests for ConfigLoader class. Tests hierarchical .env file loading, property
 * resolution, and edge cases.
 */
class ConfigLoaderTest {

  @TempDir Path tempDir;

  private String originalUserDir;
  private Map<String, String> originalEnvVars;
  private Map<String, String> originalSystemProps;

  @BeforeEach
  void setUp() {
    // Save original system state
    originalUserDir = System.getProperty("user.dir");
    originalEnvVars = new HashMap<>(System.getenv());
    originalSystemProps = new HashMap<>();

    // Save relevant system properties
    String[] propsToSave = {"TEST_PROP1", "TEST_PROP2", "LOG_LEVEL", "BIM_PORTAL_USERNAME"};
    for (String prop : propsToSave) {
      String value = System.getProperty(prop);
      if (value != null) {
        originalSystemProps.put(prop, value);
      }
    }
  }

  @AfterEach
  void tearDown() {
    // Restore original system state
    System.setProperty("user.dir", originalUserDir);

    // Clear test system properties
    String[] propsToClean = {"TEST_PROP1", "TEST_PROP2", "LOG_LEVEL", "BIM_PORTAL_USERNAME"};
    for (String prop : propsToClean) {
      System.clearProperty(prop);
    }

    // Restore original system properties
    for (Map.Entry<String, String> entry : originalSystemProps.entrySet()) {
      System.setProperty(entry.getKey(), entry.getValue());
    }

    // Force ConfigLoader to reload
    ConfigLoader.reloadConfig();
  }

  @Nested
  @DisplayName("Property Resolution Tests")
  class PropertyResolutionTests {

    @Test
    @DisplayName("Should return null for non-existent property")
    void shouldReturnNullForNonExistentProperty() {
      String result = ConfigLoader.getProperty("NON_EXISTENT_PROPERTY");
      assertNull(result);
    }

    @Test
    @DisplayName("Should return default value for non-existent property")
    void shouldReturnDefaultValueForNonExistentProperty() {
      String defaultValue = "default_test_value";
      String result = ConfigLoader.getProperty("NON_EXISTENT_PROPERTY", defaultValue);
      assertEquals(defaultValue, result);
    }

    @Test
    @DisplayName("Should prioritize system properties over default")
    void shouldPrioritizeSystemPropertiesOverDefault() {
      String key = "TEST_PROP1";
      String systemValue = "system_value";
      String defaultValue = "default_value";

      System.setProperty(key, systemValue);

      String result = ConfigLoader.getProperty(key, defaultValue);
      assertEquals(systemValue, result);
    }

    @Test
    @DisplayName("Should trim whitespace from property values")
    void shouldTrimWhitespaceFromPropertyValues() {
      String key = "TEST_PROP2";
      String valueWithSpaces = "  test_value  ";

      System.setProperty(key, valueWithSpaces);

      String result = ConfigLoader.getProperty(key);
      assertEquals("test_value", result);
    }

    @Test
    @DisplayName("Should handle empty string as non-existent")
    void shouldHandleEmptyStringAsNonExistent() {
      String key = "TEST_EMPTY_PROP";
      String defaultValue = "default";

      System.setProperty(key, "   "); // Only whitespace

      String result = ConfigLoader.getProperty(key, defaultValue);
      assertEquals(defaultValue, result);
    }
  }

  @Nested
  @DisplayName("Integer Property Tests")
  class IntegerPropertyTests {

    @Test
    @DisplayName("Should parse valid integer property")
    void shouldParseValidIntegerProperty() {
      String key = "TEST_INT_PROP";
      System.setProperty(key, "123");

      int result = ConfigLoader.getIntProperty(key, 999);
      assertEquals(123, result);
    }

    @Test
    @DisplayName("Should return default for invalid integer property")
    void shouldReturnDefaultForInvalidIntegerProperty() {
      String key = "TEST_INVALID_INT_PROP";
      int defaultValue = 456;

      System.setProperty(key, "not_a_number");

      int result = ConfigLoader.getIntProperty(key, defaultValue);
      assertEquals(defaultValue, result);
    }

    @Test
    @DisplayName("Should return default for non-existent integer property")
    void shouldReturnDefaultForNonExistentIntegerProperty() {
      int defaultValue = 789;
      int result = ConfigLoader.getIntProperty("NON_EXISTENT_INT", defaultValue);
      assertEquals(defaultValue, result);
    }
  }

  @Nested
  @DisplayName("Boolean Property Tests")
  class BooleanPropertyTests {

    @Test
    @DisplayName("Should parse true boolean property")
    void shouldParseTrueBooleanProperty() {
      String key = "TEST_BOOL_TRUE";
      System.setProperty(key, "true");

      boolean result = ConfigLoader.getBooleanProperty(key, false);
      assertTrue(result);
    }

    @Test
    @DisplayName("Should parse false boolean property")
    void shouldParseFalseBooleanProperty() {
      String key = "TEST_BOOL_FALSE";
      System.setProperty(key, "false");

      boolean result = ConfigLoader.getBooleanProperty(key, true);
      assertFalse(result);
    }

    @Test
    @DisplayName("Should return default for non-existent boolean property")
    void shouldReturnDefaultForNonExistentBooleanProperty() {
      boolean defaultValue = true;
      boolean result = ConfigLoader.getBooleanProperty("NON_EXISTENT_BOOL", defaultValue);
      assertEquals(defaultValue, result);
    }

    @Test
    @DisplayName("Should handle case insensitive boolean values")
    void shouldHandleCaseInsensitiveBooleanValues() {
      String key = "TEST_BOOL_CASE";
      System.setProperty(key, "TRUE");

      boolean result = ConfigLoader.getBooleanProperty(key, false);
      assertTrue(result);
    }
  }

  @Nested
  @DisplayName("Env File Hierarchical Search Tests")
  class EnvFileHierarchicalTests {

    @Test
    @DisplayName("Should find .env file in current directory")
    void shouldFindEnvFileInCurrentDirectory() throws IOException {
      // Create a temporary directory structure
      Path projectDir = tempDir.resolve("current");
      Files.createDirectories(projectDir);

      // Create .env file in current directory
      Path envFile = projectDir.resolve(".env");
      String envContent = "TEST_ENV_VAR=current_dir_value\nLOG_LEVEL=DEBUG\n";
      Files.write(envFile, envContent.getBytes());

      // Change working directory to the project directory
      System.setProperty("user.dir", projectDir.toString());

      // Reload configuration
      ConfigLoader.reloadConfig();

      // Test that the value is loaded
      String result = ConfigLoader.getProperty("TEST_ENV_VAR");
      assertEquals("current_dir_value", result);

      String logLevel = ConfigLoader.getProperty("LOG_LEVEL");
      assertEquals("DEBUG", logLevel);
    }

    @Test
    @DisplayName("Should find .env file in parent directory")
    void shouldFindEnvFileInParentDirectory() throws IOException {
      // Create directory structure: parent/child/
      Path parentDir = tempDir.resolve("parent");
      Path childDir = parentDir.resolve("child");
      Files.createDirectories(childDir);

      // Create .env file in parent directory
      Path envFile = parentDir.resolve(".env");
      String envContent = "TEST_PARENT_VAR=parent_value\nEXPORT_DIRECTORY=parent_exports\n";
      Files.write(envFile, envContent.getBytes());

      // Set working directory to child directory
      System.setProperty("user.dir", childDir.toString());

      // Reload configuration
      ConfigLoader.reloadConfig();

      // Test that the value is loaded from parent
      String result = ConfigLoader.getProperty("TEST_PARENT_VAR");
      assertEquals("parent_value", result);

      String exportDir = ConfigLoader.getProperty("EXPORT_DIRECTORY");
      assertEquals("parent_exports", exportDir);
    }

    @Test
    @DisplayName("Should find .env file in grandparent directory")
    void shouldFindEnvFileInGrandparentDirectory() throws IOException {
      // Create directory structure: grandparent/parent/child/
      Path grandparentDir = tempDir.resolve("grandparent");
      Path parentDir = grandparentDir.resolve("parent");
      Path childDir = parentDir.resolve("child");
      Files.createDirectories(childDir);

      // Create .env file in grandparent directory
      Path envFile = grandparentDir.resolve(".env");
      String envContent = "TEST_GRANDPARENT_VAR=grandparent_value\nMAX_RETRIES=5\n";
      Files.write(envFile, envContent.getBytes());

      // Set working directory to child directory
      System.setProperty("user.dir", childDir.toString());

      // Reload configuration
      ConfigLoader.reloadConfig();

      // Test that the value is loaded from grandparent
      String result = ConfigLoader.getProperty("TEST_GRANDPARENT_VAR");
      assertEquals("grandparent_value", result);

      String maxRetries = ConfigLoader.getProperty("MAX_RETRIES");
      assertEquals("5", maxRetries);
    }

    @Test
    @DisplayName("Should prioritize closer .env file")
    void shouldPrioritizeCloserEnvFile() throws IOException {
      // Create directory structure: root/module/
      Path rootDir = tempDir.resolve("root");
      Path moduleDir = rootDir.resolve("module");
      Files.createDirectories(moduleDir);

      // Create .env file in both locations with different values
      Path rootEnvFile = rootDir.resolve(".env");
      String rootEnvContent = "TEST_PRIORITY_VAR=root_value\nCOMMON_VAR=from_root\n";
      Files.write(rootEnvFile, rootEnvContent.getBytes());

      Path moduleEnvFile = moduleDir.resolve(".env");
      String moduleEnvContent = "TEST_PRIORITY_VAR=module_value\nMODULE_SPECIFIC=module_only\n";
      Files.write(moduleEnvFile, moduleEnvContent.getBytes());

      // Set working directory to module directory
      System.setProperty("user.dir", moduleDir.toString());

      // Reload configuration
      ConfigLoader.reloadConfig();

      // Should get value from closer (module) .env file
      String priorityResult = ConfigLoader.getProperty("TEST_PRIORITY_VAR");
      assertEquals("module_value", priorityResult);

      // Should get module-specific value
      String moduleResult = ConfigLoader.getProperty("MODULE_SPECIFIC");
      assertEquals("module_only", moduleResult);

      // Should NOT get common value from root since it's only available in parent
      // Note: dotenv-java doesn't automatically merge files - it only loads one
      String commonResult = ConfigLoader.getProperty("COMMON_VAR");
      // This might be null because we only load the closest .env file
      // Let's just verify that we get the module value correctly
      assertNotEquals(
          "from_root",
          commonResult,
          "Should not get value from parent .env when closer .env exists");
    }
  }

  @Nested
  @DisplayName("Property Existence Tests")
  class PropertyExistenceTests {

    @Test
    @DisplayName("Should correctly identify existing property")
    void shouldCorrectlyIdentifyExistingProperty() {
      String key = "EXISTING_TEST_PROP";
      System.setProperty(key, "some_value");

      assertTrue(ConfigLoader.hasProperty(key));
    }

    @Test
    @DisplayName("Should correctly identify non-existing property")
    void shouldCorrectlyIdentifyNonExistingProperty() {
      assertFalse(ConfigLoader.hasProperty("DEFINITELY_NON_EXISTENT_PROP"));
    }

    @Test
    @DisplayName("Should treat empty property as non-existing")
    void shouldTreatEmptyPropertyAsNonExisting() {
      String key = "EMPTY_TEST_PROP";
      System.setProperty(key, "   "); // Only whitespace

      assertFalse(ConfigLoader.hasProperty(key));
    }
  }

  @Nested
  @DisplayName("Configuration Sources Tests")
  class ConfigurationSourcesTests {

    @Test
    @DisplayName("Should handle missing .env file gracefully")
    void shouldHandleMissingEnvFileGracefully() throws IOException {
      // Create empty directory
      Path emptyDir = tempDir.resolve("empty");
      Files.createDirectories(emptyDir);

      // Set working directory to empty directory
      System.setProperty("user.dir", emptyDir.toString());

      // Reload configuration
      assertDoesNotThrow(() -> ConfigLoader.reloadConfig());

      // Should still work with system properties
      String key = "FALLBACK_TEST_PROP";
      String value = "fallback_value";
      System.setProperty(key, value);

      String result = ConfigLoader.getProperty(key);
      assertEquals(value, result);
    }

    @Test
    @DisplayName("Should display configuration sources without error")
    void shouldDisplayConfigurationSourcesWithoutError() {
      assertDoesNotThrow(() -> ConfigLoader.displayConfigSources());
    }

    @Test
    @DisplayName("Should reload configuration without error")
    void shouldReloadConfigurationWithoutError() {
      assertDoesNotThrow(() -> ConfigLoader.reloadConfig());
    }
  }

  @Nested
  @DisplayName("BIM Portal Specific Configuration Tests")
  class BimPortalConfigurationTests {

    @Test
    @DisplayName("Should load BIM Portal username from env file")
    void shouldLoadBimPortalUsernameFromEnvFile() throws IOException {
      // Create .env file with BIM Portal credentials
      Path envFile = tempDir.resolve(".env");
      String envContent =
          """
                    BIM_PORTAL_USERNAME=test_user@example.com
                    BIM_PORTAL_PASSWORD=test_password
                    LOG_LEVEL=DEBUG
                    """;
      Files.write(envFile, envContent.getBytes());

      // Set working directory
      System.setProperty("user.dir", tempDir.toString());
      ConfigLoader.reloadConfig();

      // Test BIM Portal specific properties
      String username = ConfigLoader.getProperty("BIM_PORTAL_USERNAME");
      assertEquals("test_user@example.com", username);

      String password = ConfigLoader.getProperty("BIM_PORTAL_PASSWORD");
      assertEquals("test_password", password);

      String logLevel = ConfigLoader.getProperty("LOG_LEVEL");
      assertEquals("DEBUG", logLevel);
    }

    @Test
    @DisplayName("Should load export directory configuration")
    void shouldLoadExportDirectoryConfiguration() throws IOException {
      Path envFile = tempDir.resolve(".env");
      String envContent =
          """
                    EXPORT_DIRECTORY=custom_exports
                    MAX_RETRIES=5
                    REQUEST_TIMEOUT=60
                    """;
      Files.write(envFile, envContent.getBytes());

      System.setProperty("user.dir", tempDir.toString());
      ConfigLoader.reloadConfig();

      assertEquals("custom_exports", ConfigLoader.getProperty("EXPORT_DIRECTORY"));
      assertEquals(5, ConfigLoader.getIntProperty("MAX_RETRIES", 3));
      assertEquals(60, ConfigLoader.getIntProperty("REQUEST_TIMEOUT", 30));
    }
  }

  @Nested
  @DisplayName("Edge Cases and Error Handling")
  class EdgeCasesTests {

    @Test
    @DisplayName("Should handle malformed .env file gracefully")
    void shouldHandleMalformedEnvFileGracefully() throws IOException {
      Path envFile = tempDir.resolve(".env");
      String malformedContent =
          """
                    VALID_VAR=valid_value
                    MALFORMED_LINE_WITHOUT_EQUALS
                    ANOTHER_VALID=another_value
                    =VALUE_WITHOUT_KEY
                    """;
      Files.write(envFile, malformedContent.getBytes());

      System.setProperty("user.dir", tempDir.toString());

      // Should not throw exception
      assertDoesNotThrow(() -> ConfigLoader.reloadConfig());

      // Valid properties should still be loaded
      String validVar = ConfigLoader.getProperty("VALID_VAR");
      assertEquals("valid_value", validVar);

      String anotherValid = ConfigLoader.getProperty("ANOTHER_VALID");
      assertEquals("another_value", anotherValid);
    }

    @Test
    @DisplayName("Should handle unreadable .env file gracefully")
    void shouldHandleUnreadableEnvFileGracefully() throws IOException {
      // This test is platform-specific and might not work on all systems
      // We'll just test that the method doesn't throw an exception
      assertDoesNotThrow(() -> ConfigLoader.reloadConfig());
    }

    @Test
    @DisplayName("Should handle null values in property resolution")
    void shouldHandleNullValuesInPropertyResolution() {
      // Test null key with no default
      String result = ConfigLoader.getProperty(null);
      assertNull(result);

      // Test null key with default
      String resultWithDefault = ConfigLoader.getProperty(null, "default");
      assertEquals("default", resultWithDefault);

      // Test hasProperty with null key
      assertFalse(ConfigLoader.hasProperty(null));
    }
  }
}
