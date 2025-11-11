package com.bimportal.client;

import com.bimportal.client.auth.ApiKeyAuth;
import com.bimportal.client.auth.HttpBasicAuth;
import com.bimportal.client.auth.HttpBearerAuth;
import com.bimportal.client.decoder.BinaryResponseDecoder;
import com.bimportal.client.decoder.BinaryResponseErrorDecoder;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.databind.json.JsonMapper;
import com.fasterxml.jackson.databind.util.StdDateFormat;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import feign.Feign;
import feign.RequestInterceptor;
import feign.form.FormEncoder;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;
import feign.okhttp.OkHttpClient;
import feign.slf4j.Slf4jLogger;
import java.time.OffsetDateTime;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.function.Supplier;
import java.util.logging.Level;
import java.util.logging.Logger;
import org.openapitools.jackson.nullable.JsonNullableModule;

/** Enhanced ApiClient with binary response support. */
@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
@SuppressWarnings({"unchecked", "rawtypes"})
public class ApiClient {
  private static final Logger log = Logger.getLogger(ApiClient.class.getName());

  public interface Api {}

  protected ObjectMapper objectMapper;
  private String basePath = "https://via.bund.de/bmdv/bim-portal/edu/bim";
  private Map<String, RequestInterceptor> apiAuthorizations;
  private Feign.Builder feignBuilder;

  public ApiClient() {
    apiAuthorizations = new LinkedHashMap<String, RequestInterceptor>();
    objectMapper = createObjectMapper();
    feignBuilder = createFeignBuilder();
  }

  public ApiClient(String[] authNames) {
    this();
    for (String authName : authNames) {
      log.log(Level.FINE, "Creating authentication {0}", authName);
      RequestInterceptor auth = null;
      if ("bearerAuth".equals(authName)) {
        auth = new HttpBearerAuth("bearer");
      } else {
        throw new RuntimeException(
            "auth name \"" + authName + "\" not found in available auth names");
      }
      if (auth != null) {
        addAuthorization(authName, auth);
      }
    }
  }

  /** Basic constructor for single auth name */
  public ApiClient(String authName) {
    this(new String[] {authName});
  }

  /** Helper constructor for single api key */
  public ApiClient(String authName, String apiKey) {
    this(authName);
    this.setApiKey(apiKey);
  }

  public String getBasePath() {
    return basePath;
  }

  public ApiClient setBasePath(String basePath) {
    this.basePath = basePath;
    return this;
  }

  public Map<String, RequestInterceptor> getApiAuthorizations() {
    return apiAuthorizations;
  }

  public void setApiAuthorizations(Map<String, RequestInterceptor> apiAuthorizations) {
    this.apiAuthorizations = apiAuthorizations;
  }

  public Feign.Builder getFeignBuilder() {
    return feignBuilder;
  }

  public ApiClient setFeignBuilder(Feign.Builder feignBuilder) {
    this.feignBuilder = feignBuilder;
    return this;
  }

  /** Create ObjectMapper with proper JSR310 support. */
  private ObjectMapper createObjectMapper() {
    JsonMapper.Builder builder = JsonMapper.builder();

    // Register JavaTimeModule FIRST
    JavaTimeModule javaTimeModule = new JavaTimeModule();
    javaTimeModule.addDeserializer(OffsetDateTime.class, new LenientOffsetDateTimeDeserializer());
    builder.addModule(javaTimeModule);

    // Register JsonNullableModule
    JsonNullableModule jsonNullableModule = new JsonNullableModule();
    builder.addModule(jsonNullableModule);

    ObjectMapper mapper = builder.build();

    // Configure features
    mapper.enable(SerializationFeature.WRITE_ENUMS_USING_TO_STRING);
    mapper.enable(DeserializationFeature.READ_ENUMS_USING_TO_STRING);
    mapper.disable(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES);
    mapper.disable(DeserializationFeature.FAIL_ON_INVALID_SUBTYPE);
    mapper.disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    mapper.configure(DeserializationFeature.READ_DATE_TIMESTAMPS_AS_NANOSECONDS, false);
    mapper.configure(SerializationFeature.WRITE_DATE_TIMESTAMPS_AS_NANOSECONDS, false);

    mapper.setDateFormat(new StdDateFormat().withColonInTimeZone(true));

    log.info("ObjectMapper configured with modules: " + mapper.getRegisteredModuleIds());

    return mapper;
  }

  /** Create Feign builder with binary response support. */
  private Feign.Builder createFeignBuilder() {
    // Create the original Jackson decoder
    JacksonDecoder originalDecoder = new JacksonDecoder(objectMapper);

    // Wrap it with our binary decoder
    BinaryResponseDecoder binaryDecoder = new BinaryResponseDecoder(originalDecoder);

    // Create custom error decoder
    BinaryResponseErrorDecoder errorDecoder = new BinaryResponseErrorDecoder();

    return Feign.builder()
        .client(new OkHttpClient())
        .encoder(new FormEncoder(new JacksonEncoder(objectMapper)))
        .decoder(binaryDecoder) // Use our custom binary decoder
        .errorDecoder(errorDecoder) // Use our custom error decoder
        .logger(new Slf4jLogger());
  }

  public ObjectMapper getObjectMapper() {
    return objectMapper;
  }

  public void setObjectMapper(ObjectMapper objectMapper) {
    this.objectMapper = objectMapper;
    // Rebuild Feign builder with new ObjectMapper
    this.feignBuilder = createFeignBuilder();
  }

  /** Creates a feign client for given API interface. */
  public <T extends Api> T buildClient(Class<T> clientClass) {
    return feignBuilder.target(clientClass, basePath);
  }

  /** Select the Accept header's value from the given accepts array. */
  public String selectHeaderAccept(String[] accepts) {
    if (accepts.length == 0) return null;
    if (StringUtil.containsIgnoreCase(accepts, "application/json")) return "application/json";
    return StringUtil.join(accepts, ",");
  }

  /** Select the Content-Type header's value from the given array. */
  public String selectHeaderContentType(String[] contentTypes) {
    if (contentTypes.length == 0) return "application/json";
    if (StringUtil.containsIgnoreCase(contentTypes, "application/json")) return "application/json";
    return contentTypes[0];
  }

  /** Helper method to configure the bearer token. */
  public void setBearerToken(String bearerToken) {
    HttpBearerAuth apiAuthorization = getAuthorization(HttpBearerAuth.class);
    apiAuthorization.setBearerToken(bearerToken);
  }

  /** Helper method to configure the supplier of bearer tokens. */
  public void setBearerToken(Supplier<String> tokenSupplier) {
    HttpBearerAuth apiAuthorization = getAuthorization(HttpBearerAuth.class);
    apiAuthorization.setBearerToken(tokenSupplier);
  }

  /** Helper method to configure the first api key found */
  public void setApiKey(String apiKey) {
    ApiKeyAuth apiAuthorization = getAuthorization(ApiKeyAuth.class);
    apiAuthorization.setApiKey(apiKey);
  }

  /** Helper method to configure the username/password for basic auth */
  public void setCredentials(String username, String password) {
    HttpBasicAuth apiAuthorization = getAuthorization(HttpBasicAuth.class);
    apiAuthorization.setCredentials(username, password);
  }

  /** Gets request interceptor based on authentication name */
  public RequestInterceptor getAuthorization(String authName) {
    return apiAuthorizations.get(authName);
  }

  /** Adds an authorization to be used by the client */
  public void addAuthorization(String authName, RequestInterceptor authorization) {
    if (apiAuthorizations.containsKey(authName)) {
      throw new RuntimeException("auth name \"" + authName + "\" already in api authorizations");
    }
    apiAuthorizations.put(authName, authorization);
    feignBuilder.requestInterceptor(authorization);
  }

  @SuppressWarnings("unchecked")
  private <T extends RequestInterceptor> T getAuthorization(Class<T> type) {
    return (T)
        apiAuthorizations.values().stream()
            .filter(requestInterceptor -> type.isAssignableFrom(requestInterceptor.getClass()))
            .findFirst()
            .orElseThrow(
                () -> new RuntimeException("No Oauth authentication or OAuth configured!"));
  }
}
