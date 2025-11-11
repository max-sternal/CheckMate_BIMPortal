package com.pb40.bimportal.auth;

import com.bimportal.client.model.JWTTokenPublicDto;
import com.bimportal.client.model.UserLoginPublicDto;
import java.util.Optional;
import java.util.UUID;

/**
 * Enhanced authentication service interface for BIM Portal API.
 *
 * <p>Handles authentication lifecycle including login, token management, refresh, logout, and JWT
 * user UUID extraction.
 */
public interface AuthService {

  /**
   * Authenticate with explicit credentials.
   *
   * @param credentials User login credentials
   * @return JWT token response
   * @throws AuthenticationException if login fails
   */
  JWTTokenPublicDto login(UserLoginPublicDto credentials) throws AuthenticationException;

  /**
   * Authenticate with configured credentials from environment.
   *
   * @return JWT token response
   * @throws AuthenticationException if login fails or no credentials configured
   */
  JWTTokenPublicDto login() throws AuthenticationException;

  /**
   * Get a valid access token, refreshing or re-authenticating if needed.
   *
   * @return Valid access token
   * @throws AuthenticationException if unable to obtain valid token
   */
  String getValidToken() throws AuthenticationException;

  /**
   * Refresh the current access token using refresh token.
   *
   * @return New JWT token response
   * @throws AuthenticationException if refresh fails
   */
  JWTTokenPublicDto refreshToken() throws AuthenticationException;

  /**
   * Logout and invalidate current session.
   *
   * @return True if logout was successful
   */
  boolean logout();

  /**
   * Check if currently authenticated with valid token.
   *
   * @return True if authenticated
   */
  boolean isAuthenticated();

  /** Clear all stored tokens and session data. */
  void clearTokens();

  /**
   * Check if credentials are configured for authentication.
   *
   * @return True if credentials are available
   */
  boolean hasCredentials();

  // JWT user UUID extraction methods

  /**
   * Get the current user UUID extracted from JWT token.
   *
   * @return User UUID or empty if not authenticated or not available
   */
  default Optional<UUID> getCurrentUserId() {
    return Optional.empty();
  }

  /**
   * Get the current user UUID, throwing an exception if not available.
   *
   * @return User UUID
   * @throws AuthenticationException if not authenticated or user UUID not available
   */
  default UUID getCurrentUserIdRequired() throws AuthenticationException {
    return getCurrentUserId()
        .orElseThrow(
            () ->
                new AuthenticationException(
                    "User UUID not available. Please ensure you are authenticated and the JWT token contains user information."));
  }

  /**
   * Check if current user UUID is available.
   *
   * @return True if user UUID is available
   */
  default boolean hasCurrentUserId() {
    return getCurrentUserId().isPresent();
  }

  /**
   * Get detailed token status information for debugging.
   *
   * @return Token status string including user information
   */
  default String getTokenStatus() {
    if (isAuthenticated()) {
      StringBuilder status = new StringBuilder("Authenticated");
      getCurrentUserId().ifPresent(userId -> status.append(", User ID: ").append(userId));
      return status.toString();
    }
    return "Not authenticated";
  }
}
