package com.pb40.bimportal.client;

import com.bimportal.client.ApiClient;
import com.bimportal.client.LenientOffsetDateTimeDeserializer;
import com.bimportal.client.api.InfrastrukturApi;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.pb40.bimportal.auth.AuthService;
import com.pb40.bimportal.config.BimPortalConfig;
import java.time.OffsetDateTime;
import org.openapitools.jackson.nullable.JsonNullableModule;

public final class ApiClientFactory {
  private ApiClientFactory() {}

  // Remove static field - create auth service when needed

  public static ApiClient createConfiguredApiClient() {
    // Create ApiClient with bearerAuth already configured
    ApiClient apiClient = new ApiClient("bearerAuth");

    // Set the correct base path for the edu environment
    apiClient.setBasePath(BimPortalConfig.getBaseUrl());

    // Configure ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
    mapper.registerModule(javaTimeModule);
    mapper.registerModule(new JsonNullableModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    apiClient.setObjectMapper(mapper);

    return apiClient;
  }

  public static InfrastrukturApi createInfrastrukturApi() {
    // Create ApiClient WITHOUT bearer token (for use by AuthService itself)
    ApiClient apiClient = new ApiClient();
    apiClient.setBasePath(BimPortalConfig.getBaseUrl());

    // Configure ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
    mapper.registerModule(javaTimeModule);
    mapper.registerModule(new JsonNullableModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    apiClient.setObjectMapper(mapper);

    return apiClient.buildClient(InfrastrukturApi.class);
  }

  // Add new method for creating authenticated APIs
  public static <T extends ApiClient.Api> T createAuthenticatedApi(
      Class<T> apiClass, AuthService authService) {
    ApiClient apiClient = new ApiClient("bearerAuth");
    apiClient.setBasePath(BimPortalConfig.getBaseUrl());

    // Configure ObjectMapper
    ObjectMapper mapper = new ObjectMapper();
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
    mapper.registerModule(javaTimeModule);
    mapper.registerModule(new JsonNullableModule());
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    apiClient.setObjectMapper(mapper);

    // Set bearer token supplier
    apiClient.setBearerToken(
        () -> {
          try {
            return authService.getValidToken();
          } catch (Exception e) {
            throw new RuntimeException("Failed to get token", e);
          }
        });

    return apiClient.buildClient(apiClass);
  }
}
