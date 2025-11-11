package com.bimportal.client;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;

/**
 * Helper class to configure Jackson ObjectMapper with proper JSR310 support for handling Java 8
 * date/time types like OffsetDateTime.
 */
public class JacksonConfiguration {

  /**
   * Creates a properly configured ObjectMapper with JSR310 support
   *
   * @return ObjectMapper configured for Java 8 date/time types
   */
  public static ObjectMapper createConfiguredObjectMapper() {
    ObjectMapper mapper = new ObjectMapper();

    // Register JSR310 module for Java 8 date/time support
    mapper.registerModule(new JavaTimeModule());

    // Disable writing dates as timestamps (use ISO-8601 strings instead)
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    // Enable reading of unknown enum values as null instead of failing
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        true);

    // Ignore unknown properties instead of failing
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return mapper;
  }

  /**
   * Configures an existing ObjectMapper with JSR310 support
   *
   * @param mapper The ObjectMapper to configure
   * @return The same ObjectMapper instance (for method chaining)
   */
  public static ObjectMapper configureObjectMapper(ObjectMapper mapper) {
    if (mapper == null) {
      return createConfiguredObjectMapper();
    }

    // Register JSR310 module if not already registered
    mapper.registerModule(new JavaTimeModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.READ_UNKNOWN_ENUM_VALUES_AS_NULL,
        true);
    mapper.configure(
        com.fasterxml.jackson.databind.DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false);

    return mapper;
  }
}
