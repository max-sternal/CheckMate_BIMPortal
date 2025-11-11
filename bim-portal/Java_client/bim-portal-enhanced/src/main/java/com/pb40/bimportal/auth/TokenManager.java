package com.pb40.bimportal.auth;

import com.bimportal.client.model.JWTTokenPublicDto;
import com.pb40.bimportal.config.BimPortalConfig;
import java.time.Duration;
import java.time.Instant;
import java.util.Base64;
import java.util.Optional;
import java.util.UUID;
import java.util.concurrent.locks.ReentrantReadWriteLock;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Thread-safe JWT token manager for BIM Portal authentication.
 *
 * <p>Handles token storage, validation, expiration checking, and user UUID extraction.
 */
public class TokenManager {

  private static final Logger logger = LoggerFactory.getLogger(TokenManager.class);

  // Patterns for extracting claims from JWT payload
  private static final Pattern USER_ID_PATTERN =
      Pattern.compile("\"(?:sub|userId|user_id|id)\"\\s*:\\s*\"([^\"]+)\"");
  private static final Pattern EXP_PATTERN = Pattern.compile("\"exp\"\\s*:\\s*(\\d+)");

  private volatile String accessToken;
  private volatile String refreshToken;
  private volatile Instant tokenExpiresAt;
  private volatile UUID currentUserId;

  private final ReentrantReadWriteLock lock = new ReentrantReadWriteLock();
  private final ReentrantReadWriteLock.ReadLock readLock = lock.readLock();
  private final ReentrantReadWriteLock.WriteLock writeLock = lock.writeLock();

  /**
   * Store tokens from authentication response and extract user information.
   *
   * @param tokenResponse JWT token response from API
   */
  public void storeTokens(JWTTokenPublicDto tokenResponse) {
    if (tokenResponse == null) {
      logger.warn("Cannot store null token response");
      return;
    }

    writeLock.lock();
    try {
      this.accessToken = tokenResponse.getToken();
      this.refreshToken = tokenResponse.getRefreshToken();

      // Extract user UUID from JWT token
      this.currentUserId = extractUserIdFromToken(tokenResponse.getToken());

      Instant now = Instant.now();

      // ALWAYS use the JWT exp claim - it's the reliable source
      this.tokenExpiresAt = parseTokenExpiration(tokenResponse.getToken());
      logger.info("Using JWT expiration. Token expires at: {}", tokenExpiresAt);

      // Log token validity duration
      Duration validityDuration = Duration.between(now, tokenExpiresAt);
      long secondsUntilExpiry = validityDuration.getSeconds();
      long millisUntilExpiry = validityDuration.toMillis();

      logger.debug(
          "Token valid for {} seconds ({} milliseconds)", secondsUntilExpiry, millisUntilExpiry);

      // Additional warning if token is about to expire
      if (secondsUntilExpiry < 50) {
        logger.warn(
            "Token has only {} seconds remaining - consider refreshing soon", secondsUntilExpiry);
      }

      // Log user information
      if (currentUserId != null) {
        logger.info("Extracted user UUID from token: {}", currentUserId);
      } else {
        logger.warn("Could not extract user UUID from JWT token");
      }

    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Get the current access token if valid.
   *
   * @return Access token or null if not available or expired
   */
  public String getAccessToken() {
    readLock.lock();
    try {
      if (accessToken == null) {
        logger.debug("No access token available");
        return null;
      }

      if (isTokenExpiring()) {
        logger.debug("Access token is expiring soon or expired");
        return null;
      }

      return accessToken;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Get the current refresh token.
   *
   * @return Refresh token or null if not available
   */
  public String getRefreshToken() {
    readLock.lock();
    try {
      return refreshToken;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Get the current user UUID extracted from the JWT token.
   *
   * @return User UUID or empty if not available
   */
  public Optional<UUID> getCurrentUserId() {
    readLock.lock();
    try {
      return Optional.ofNullable(currentUserId);
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Check if the current token is expiring soon.
   *
   * @return True if token will expire within the configured margin
   */
  public boolean isTokenExpiring() {
    readLock.lock();
    try {
      if (tokenExpiresAt == null) {
        logger.debug("No expiration time set - treating as expired");
        return true;
      }

      Instant now = Instant.now();
      Instant refreshThreshold = now.plus(BimPortalConfig.TOKEN_REFRESH_MARGIN);

      boolean expiring = tokenExpiresAt.isBefore(refreshThreshold);

      if (expiring) {
        long secondsUntilExpiry = tokenExpiresAt.getEpochSecond() - now.getEpochSecond();
        logger.debug(
            "Token expiring: {} seconds remaining (refresh margin: {} seconds)",
            secondsUntilExpiry,
            BimPortalConfig.TOKEN_REFRESH_MARGIN.getSeconds());
      }

      return expiring;
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Check if tokens are available.
   *
   * @return True if access token exists
   */
  public boolean hasTokens() {
    readLock.lock();
    try {
      return accessToken != null;
    } finally {
      readLock.unlock();
    }
  }

  /** Clear all stored tokens and user information. */
  public void clearTokens() {
    writeLock.lock();
    try {
      this.accessToken = null;
      this.refreshToken = null;
      this.tokenExpiresAt = null;
      this.currentUserId = null;
      logger.info("All tokens and user information cleared");
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Extract user UUID from JWT token payload. Looks for common user ID claims: sub, userId,
   * user_id, id
   *
   * @param token JWT token
   * @return User UUID or null if not found or parsing fails
   */
  private UUID extractUserIdFromToken(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }

    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        logger.warn("Invalid JWT token format for user ID extraction");
        return null;
      }

      // Decode the payload (second part of JWT)
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
      logger.debug("JWT payload for user extraction: {}", payload);

      // Try to find user ID in common claim names
      Matcher matcher = USER_ID_PATTERN.matcher(payload);
      if (matcher.find()) {
        String userIdString = matcher.group(1);
        logger.debug("Found user ID string in JWT: {}", userIdString);

        try {
          // Try to parse as UUID
          UUID userId = UUID.fromString(userIdString);
          logger.info("Successfully extracted user UUID: {}", userId);
          return userId;
        } catch (IllegalArgumentException e) {
          logger.warn("User ID found in JWT is not a valid UUID: {}", userIdString);
          // Could return null or try other approaches
          return null;
        }
      } else {
        logger.warn("No user ID claim found in JWT payload. Checked for: sub, userId, user_id, id");

        // Log available claims for debugging
        logAvailableClaims(payload);
        return null;
      }

    } catch (Exception e) {
      logger.warn("Failed to extract user ID from JWT: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Parse JWT token expiration from token payload. This is a fallback method when validTill is not
   * available in the response.
   *
   * @param token JWT token
   * @return Expiration instant or null if parsing fails
   */
  private Instant parseTokenExpiration(String token) {
    if (token == null || token.trim().isEmpty()) {
      return null;
    }

    try {
      String[] parts = token.split("\\.");
      if (parts.length < 2) {
        logger.warn("Invalid JWT token format");
        return null;
      }

      // Decode the payload (second part of JWT)
      String payload = new String(Base64.getUrlDecoder().decode(parts[1]));
      //            logger.debug("JWT payload: {}", payload);

      // Use pattern matching for more reliable extraction
      Matcher matcher = EXP_PATTERN.matcher(payload);
      if (matcher.find()) {
        String expValue = matcher.group(1);
        long expirationSeconds = Long.parseLong(expValue);

        Instant expiration = Instant.ofEpochSecond(expirationSeconds);
        logger.debug("Parsed token expiration from JWT exp claim: {}", expiration);

        return expiration;
      } else {
        logger.warn("No exp claim found in JWT token");
        return null;
      }

    } catch (Exception e) {
      logger.warn("Failed to parse token expiration from JWT: {}", e.getMessage());
      return null;
    }
  }

  /**
   * Log available claims in JWT payload for debugging purposes.
   *
   * @param payload JWT payload JSON string
   */
  private void logAvailableClaims(String payload) {
    try {
      // Simple extraction of claim names for debugging
      Pattern claimPattern = Pattern.compile("\"([^\"]+)\"\\s*:");
      Matcher matcher = claimPattern.matcher(payload);
      StringBuilder claims = new StringBuilder();
      while (matcher.find()) {
        if (claims.length() > 0) {
          claims.append(", ");
        }
        claims.append(matcher.group(1));
      }
      logger.debug("Available claims in JWT: {}", claims.toString());
    } catch (Exception e) {
      logger.debug("Could not extract claim names for debugging", e);
    }
  }

  /**
   * Get token status information for debugging.
   *
   * @return Status string
   */
  public String getTokenStatus() {
    readLock.lock();
    try {
      if (accessToken == null) {
        return "No token";
      }

      StringBuilder status = new StringBuilder();

      if (tokenExpiresAt == null) {
        status.append("Token available (expiration unknown)");
      } else {
        long secondsRemaining = tokenExpiresAt.getEpochSecond() - Instant.now().getEpochSecond();

        if (isTokenExpiring()) {
          status.append(
              String.format(
                  "Token expiring soon (expires in %d seconds at %s)",
                  secondsRemaining, tokenExpiresAt));
        } else {
          status.append(
              String.format(
                  "Token valid (expires in %d seconds at %s)", secondsRemaining, tokenExpiresAt));
        }
      }

      // Add user information
      if (currentUserId != null) {
        status.append(", User ID: ").append(currentUserId);
      } else {
        status.append(", User ID: not available");
      }

      return status.toString();
    } finally {
      readLock.unlock();
    }
  }

  /**
   * Manually set the token expiration time. This can be used when the actual expiration is known
   * from other sources.
   *
   * @param expiresAt The expiration instant
   */
  public void setTokenExpiration(Instant expiresAt) {
    writeLock.lock();
    try {
      this.tokenExpiresAt = expiresAt;
      logger.info("Token expiration manually set to: {}", expiresAt);
    } finally {
      writeLock.unlock();
    }
  }

  /**
   * Manually set the current user ID. This can be used if the user ID is obtained from other
   * sources.
   *
   * @param userId The user UUID
   */
  public void setCurrentUserId(UUID userId) {
    writeLock.lock();
    try {
      this.currentUserId = userId;
      logger.info("Current user ID manually set to: {}", userId);
    } finally {
      writeLock.unlock();
    }
  }
}
