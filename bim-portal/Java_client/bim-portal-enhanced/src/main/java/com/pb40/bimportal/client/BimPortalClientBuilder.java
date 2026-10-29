package com.pb40.bimportal.client;

import com.bimportal.client.api.InfrastrukturApi;
import com.pb40.bimportal.auth.AuthService;
import com.pb40.bimportal.auth.AuthServiceImpl;
import com.pb40.bimportal.config.BimPortalConfig;
import feign.Feign;
import feign.jackson.JacksonDecoder;
import feign.jackson.JacksonEncoder;

/**
 * Builder pattern for creating configured BIM Portal clients.
 *
 * <p>This builder provides a fluent API for creating clients with various configuration options
 * while maintaining sensible defaults.
 */
public class BimPortalClientBuilder {

  private String baseUrl;
  private String username;
  private String password;
  private AuthService authService;
  private boolean useDefaultCredentials = true;
  private boolean publicAccessOnly = false;

  /** Create a new builder instance. */
  public BimPortalClientBuilder() {
    // Set defaults from configuration
    this.baseUrl = BimPortalConfig.getBaseUrl();
  }

  /**
   * Set custom base URL.
   *
   * @param baseUrl Base URL for the API
   * @return This builder instance
   */
  public BimPortalClientBuilder withBaseUrl(String baseUrl) {
    this.baseUrl = baseUrl;
    return this;
  }

  /**
   * Set custom credentials.
   *
   * @param username Username for authentication
   * @param password Password for authentication
   * @return This builder instance
   */
  public BimPortalClientBuilder withCredentials(String username, String password) {
    this.username = username;
    this.password = password;
    this.useDefaultCredentials = false;
    return this;
  }

  /**
   * Use credentials from environment configuration.
   *
   * @return This builder instance
   */
  public BimPortalClientBuilder withEnvironmentCredentials() {
    this.useDefaultCredentials = true;
    this.username = null;
    this.password = null;
    return this;
  }

  /**
   * Use anonymous public access only - no login, no Authorization header.
   *
   * <p>The BIM Portal serves every {@code /public/} endpoint without authentication, so searching
   * and exporting published LOINs, projects and domain models works without an account. Private,
   * organisation-internal content is not visible this way.
   *
   * @return This builder instance
   */
  public BimPortalClientBuilder withPublicAccess() {
    this.publicAccessOnly = true;
    return this;
  }

  /**
   * Set custom authentication service.
   *
   * @param authService Custom authentication service
   * @return This builder instance
   */
  public BimPortalClientBuilder withAuthService(AuthService authService) {
    this.authService = authService;
    return this;
  }

  /**
   * Build the enhanced BIM Portal client.
   *
   * @return Configured client instance
   */
  public EnhancedBimPortalClient build() {
    validateConfiguration();

    // Update configuration if custom base URL provided
    if (!baseUrl.equals(BimPortalConfig.getBaseUrl())) {
      BimPortalConfig.setBaseUrl(baseUrl);
    }

    // Create auth service if not provided
    if (authService == null) {
      InfrastrukturApi infraApi = createInfrastrukturApi();

      if (useDefaultCredentials) {
        authService = new AuthServiceImpl(infraApi);
      } else {
        authService = new AuthServiceImpl(infraApi, username, password);
      }
    }

    return new EnhancedBimPortalClient(authService, publicAccessOnly);
  }

  /**
   * Create the InfrastrukturApi client using Feign builder.
   *
   * @return Configured Feign client for InfrastrukturApi
   */
  private InfrastrukturApi createInfrastrukturApi() {
    return Feign.builder()
        .encoder(new JacksonEncoder())
        .decoder(new JacksonDecoder())
        .errorDecoder(
            new feign.codec.ErrorDecoder.Default() {
              @Override
              public Exception decode(String methodKey, feign.Response response) {
                if (response.status() == 401) {
                  // Log 401 for debugging
                  System.err.println("Received 401 on " + methodKey + " - token may be expired");
                }
                return super.decode(methodKey, response);
              }
            })
        .target(InfrastrukturApi.class, baseUrl);
  }

  /**
   * Quick build with default configuration.
   *
   * @return Client with default settings
   */
  public static EnhancedBimPortalClient buildDefault() {
    return new BimPortalClientBuilder().build();
  }

  /**
   * Quick build for public access only (no authentication).
   *
   * <p>Works without any account: no login is attempted and no Authorization header is sent. Only
   * published content is visible.
   *
   * @return Client configured for public access
   */
  public static EnhancedBimPortalClient buildPublicAccess() {
    return new BimPortalClientBuilder().withPublicAccess().build();
  }

  /**
   * Build an authenticated client if credentials are configured, otherwise fall back to anonymous
   * public access.
   *
   * <p>This is the convenient entry point for getting started without an account: published LOINs,
   * projects and domain models are reachable either way, and adding credentials to {@code .env}
   * later unlocks the private content without any code change.
   *
   * @return Authenticated client, or a public-access client if no credentials are set
   */
  public static EnhancedBimPortalClient buildAuthenticatedOrPublic() {
    if (hasUsableCredentials()) {
      return buildDefault();
    }

    System.out.println("ℹ No usable credentials configured - using public (anonymous) access.");
    System.out.println("  Published content is available; private content stays hidden.");
    return buildPublicAccess();
  }

  /**
   * Check for credentials that are actually worth a login attempt.
   *
   * <p>The API expects the username to be an e-mail address and answers anything else with HTTP
   * 400. A leftover placeholder from {@code .env_example} is therefore worse than no credentials at
   * all: it produces failed login attempts, and the portal locks an account after three of them.
   *
   * @return true if username and password are set and the username looks like an e-mail address
   */
  private static boolean hasUsableCredentials() {
    if (!BimPortalConfig.hasCredentials()) {
      return false;
    }

    String username = BimPortalConfig.getUsername();
    if (!username.contains("@") || username.contains("your_username")) {
      System.out.println(
          "⚠ BIM_PORTAL_USERNAME is not an e-mail address ('" + username + "') - ignoring it.");
      return false;
    }

    return true;
  }

  /**
   * Quick build with custom base URL.
   *
   * @param baseUrl Custom base URL
   * @return Client with custom base URL
   */
  public static EnhancedBimPortalClient buildWithUrl(String baseUrl) {
    return new BimPortalClientBuilder().withBaseUrl(baseUrl).build();
  }

  /**
   * Quick build with custom credentials.
   *
   * @param username Username
   * @param password Password
   * @return Client with custom credentials
   */
  public static EnhancedBimPortalClient buildWithCredentials(String username, String password) {
    return new BimPortalClientBuilder().withCredentials(username, password).build();
  }

  /**
   * Validate builder configuration before building.
   *
   * @throws IllegalStateException if configuration is invalid
   */
  private void validateConfiguration() {
    if (baseUrl == null || baseUrl.trim().isEmpty()) {
      throw new IllegalStateException("Base URL cannot be null or empty");
    }

    if (!baseUrl.startsWith("http://") && !baseUrl.startsWith("https://")) {
      throw new IllegalStateException("Base URL must start with http:// or https://");
    }

    // Validate credentials if provided
    if (!useDefaultCredentials) {
      if (username == null || username.trim().isEmpty()) {
        throw new IllegalStateException(
            "Username cannot be null or empty when using custom credentials");
      }
      if (password == null || password.trim().isEmpty()) {
        throw new IllegalStateException(
            "Password cannot be null or empty when using custom credentials");
      }
    }
  }

  /** Display current builder configuration. */
  public void displayConfiguration() {
    System.out.println("--- Builder Configuration ---");
    System.out.println("Base URL:     " + baseUrl);

    if (useDefaultCredentials) {
      System.out.println("Credentials:  From environment");
    } else {
      System.out.println("Username:     " + (username != null ? username : "Not set"));
      System.out.println("Password:     " + (password != null ? "[configured]" : "Not set"));
    }

    System.out.println("Auth Service: " + (authService != null ? "Custom" : "Default"));
    System.out.println("-----------------------------");
  }
}
