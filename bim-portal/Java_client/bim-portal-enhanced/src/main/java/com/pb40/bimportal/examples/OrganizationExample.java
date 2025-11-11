package com.pb40.bimportal.examples;

import com.bimportal.client.api.InfrastrukturApi;
import com.bimportal.client.model.OrganisationForPublicDTO;
import com.pb40.bimportal.auth.AuthService;
import com.pb40.bimportal.auth.AuthServiceImpl;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced organization examples for BIM Portal Java client with real JWT user UUID extraction.
 *
 * <p>Demonstrates organization functionality according to the actual Swagger API: - GET
 * /infrastruktur/api/v1/public/organisation (public, no auth required) - GET
 * /infrastruktur/api/v1/public/organisation/my?userId={userId} (requires auth + userId)
 *
 * <p>Now includes real implementation of JWT user UUID extraction from token payload.
 */
public class OrganizationExample {

  private static final Logger logger = LoggerFactory.getLogger(OrganizationExample.class);

  /**
   * Check if credentials are available for authentication.
   *
   * @return True if credentials are configured
   */
  private static boolean checkCredentials() {
    if (!BimPortalConfig.hasCredentials()) {
      System.out.println("============================================================");
      System.out.println("WARNING: Credentials not found in environment variables.");
      System.out.println(
          "Please set "
              + BimPortalConfig.USERNAME_ENV_VAR
              + " and "
              + BimPortalConfig.PASSWORD_ENV_VAR
              + " in .env file.");
      System.out.println("Some organization examples require authentication.");
      System.out.println("============================================================");
      return false;
    }
    return true;
  }

  /**
   * Demonstrate organization API usage with real JWT user UUID extraction.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runOrganizationExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("🏢 ORGANIZATION API EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Fetching all available organizations (public endpoint)...");
    try {
      List<OrganisationForPublicDTO> allOrganizations = client.getAllOrganisations();
      System.out.println("✅ Found " + allOrganizations.size() + " available organizations:");

      for (int i = 0; i < Math.min(5, allOrganizations.size()); i++) {
        OrganisationForPublicDTO org = allOrganizations.get(i);
        System.out.println(
            "   "
                + (i + 1)
                + ". "
                + org.getName()
                + " ("
                + org.getGuid()
                + ")"
                + (org.getDescription() != null ? " - " + org.getDescription() : ""));
      }

      if (allOrganizations.size() > 5) {
        System.out.println("   ... and " + (allOrganizations.size() - 5) + " more");
      }
    } catch (Exception e) {
      System.err.println("❌ Error fetching organizations: " + e.getMessage());
      logger.error("Error fetching organizations", e);
    }

    System.out.println("\n2️⃣ Testing organization search functionality...");
    try {
      List<OrganisationForPublicDTO> allOrgs = client.getAllOrganisations();
      if (!allOrgs.isEmpty()) {
        OrganisationForPublicDTO firstOrg = allOrgs.get(0);

        // Test GUID search
        Optional<OrganisationForPublicDTO> foundByGuid =
            client.findOrganisationByGuid(firstOrg.getGuid().toString());
        System.out.println(
            "   🔍 Search by GUID: " + (foundByGuid.isPresent() ? "✅ Found" : "❌ Not found"));

        // Test name search
        if (firstOrg.getName() != null) {
          Optional<OrganisationForPublicDTO> foundByName =
              client.findOrganisationByName(firstOrg.getName());
          System.out.println(
              "   🔍 Search by name: " + (foundByName.isPresent() ? "✅ Found" : "❌ Not found"));
        }

        // Test availability check
        boolean hasOrgs = client.hasAvailableOrganisations();
        System.out.println("   📊 Organizations available: " + (hasOrgs ? "✅ Yes" : "❌ No"));
      }
    } catch (Exception e) {
      System.err.println("❌ Error in organization search tests: " + e.getMessage());
      logger.error("Error in organization search tests", e);
    }

    System.out.println("\n3️⃣ Testing JWT user UUID extraction and user organizations...");
    if (!client.isAuthenticated()) {
      System.out.println("   ⚠️  Authentication required for user organization endpoints");
      System.out.println("   💡 Client is not authenticated - skipping user organization examples");
    } else {
      System.out.println("   ✅ Client is authenticated");

      // Extract user UUID from JWT token
      demonstrateRealUserOrganizationMethods(client);
    }

    System.out.println("\n4️⃣ Testing query parameters approach with real user UUID...");
    if (client.isAuthenticated()) {
      try {
        // Get the auth service to access user UUID
        AuthService authService = client.getAuthService();
        if (authService instanceof AuthServiceImpl) {
          AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;

          Optional<UUID> currentUserId = authServiceImpl.getCurrentUserId();
          if (currentUserId.isPresent()) {
            UUID userId = currentUserId.get();
            InfrastrukturApi.GetOrganisationsOfUserQueryParams queryParams =
                client.createUserOrganisationQueryParams(userId);

            System.out.println("   ✅ Query parameters created for real user: " + userId);
            System.out.println(
                "   💡 Ready to call: client.getUserOrganisationsWithParams(queryParams)");

            // Actually test the query parameters
            try {
              List<OrganisationForPublicDTO> userOrgs =
                  client.getUserOrganisationsWithParams(queryParams);
              System.out.println(
                  "   ✅ User organizations retrieved: " + userOrgs.size() + " found");
            } catch (Exception e) {
              System.out.println("   ⚠️  User organizations call failed: " + e.getMessage());
              logger.debug("User organizations API call failed", e);
            }
          } else {
            System.out.println("   ⚠️  User UUID not available from JWT token");
            System.out.println(
                "   💡 Check JWT token structure - user ID may be in different claim");
          }
        }
      } catch (Exception e) {
        System.err.println("   ❌ Error in query parameters test: " + e.getMessage());
        logger.error("Error in query parameters test", e);
      }
    } else {
      System.out.println("   ⚠️  Authentication required for real user UUID extraction");
    }
  }

  /** Demonstrate user organization methods with real JWT user UUID extraction. */
  private static void demonstrateRealUserOrganizationMethods(EnhancedBimPortalClient client) {
    System.out.println("\n   🔐 Real JWT User UUID Extraction:");

    try {
      // Get the auth service to access user UUID
      AuthService authService = client.getAuthService();
      if (!(authService instanceof AuthServiceImpl)) {
        System.out.println("   ⚠️  AuthService implementation not available for UUID extraction");
        return;
      }

      AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;

      // Extract user UUID from JWT token
      Optional<UUID> currentUserId = authServiceImpl.getCurrentUserId();

      if (currentUserId.isEmpty()) {
        System.out.println("   ❌ User UUID not available from JWT token");
        System.out.println("   💡 Possible reasons:");
        System.out.println("      • JWT token doesn't contain user ID claim");
        System.out.println("      • User ID claim is in unexpected format");
        System.out.println("      • Token parsing failed");

        // Show token status for debugging
        String tokenStatus = authServiceImpl.getTokenStatus();
        System.out.println("   🔍 Token status: " + tokenStatus);
        return;
      }

      UUID userId = currentUserId.get();
      System.out.println("   ✅ Successfully extracted user UUID: " + userId);

      // Now test all user organization methods with real UUID
      System.out.println("\n   📋 Testing User Organization Methods:");

      try {
        // Test 1: Get user organizations
        System.out.println("   1️⃣ Getting user organizations...");
        List<OrganisationForPublicDTO> userOrgs = client.getUserOrganisations(userId);
        System.out.println("   ✅ Found " + userOrgs.size() + " organizations for user");

        for (int i = 0; i < Math.min(3, userOrgs.size()); i++) {
          OrganisationForPublicDTO org = userOrgs.get(i);
          System.out.println("      👤 " + org.getName() + " (" + org.getGuid() + ")");
        }

        if (userOrgs.size() > 3) {
          System.out.println("      ... and " + (userOrgs.size() - 3) + " more");
        }

        // Test 2: Organization count
        System.out.println("\n   2️⃣ Getting organization count...");
        int orgCount = client.getUserOrganisationsCount(userId);
        System.out.println("   ✅ User is member of " + orgCount + " organizations");

        // Test 3: Organization names
        System.out.println("\n   3️⃣ Getting organization names...");
        List<String> orgNames = client.getUserOrganisationNames(userId);
        System.out.println("   ✅ Organization names: " + String.join(", ", orgNames));

        // Test 4: Membership check (if user has organizations)
        if (!userOrgs.isEmpty()) {
          System.out.println("\n   4️⃣ Testing membership check...");
          OrganisationForPublicDTO firstOrg = userOrgs.get(0);
          boolean isMember = client.isUserMemberOfOrganisation(userId, firstOrg.getGuid());
          System.out.println(
              "   ✅ Membership check for '"
                  + firstOrg.getName()
                  + "': "
                  + (isMember ? "✅ Confirmed" : "❌ Not confirmed"));
        } else {
          System.out.println("\n   4️⃣ Skipping membership check (no organizations found)");
        }

        // Test 5: First organization (convenience method)
        System.out.println("\n   5️⃣ Getting first user organization...");
        Optional<OrganisationForPublicDTO> firstUserOrg = client.getUserFirstOrganisation(userId);
        if (firstUserOrg.isPresent()) {
          System.out.println("   ✅ First organization: " + firstUserOrg.get().getName());
        } else {
          System.out.println("   ⚠️  No organizations found for user");
        }

      } catch (Exception e) {
        System.err.println("   ❌ Error in user organization API calls: " + e.getMessage());
        logger.error("Error in user organization API calls", e);

        // Provide debugging info
        if (e.getMessage().contains("404") || e.getMessage().contains("Not Found")) {
          System.out.println(
              "   💡 404 error suggests the user organization endpoint may not be available");
          System.out.println("      or the user UUID format is not accepted by the API");
        } else if (e.getMessage().contains("401") || e.getMessage().contains("Unauthorized")) {
          System.out.println("   💡 401 error suggests authentication issues");
        } else if (e.getMessage().contains("403") || e.getMessage().contains("Forbidden")) {
          System.out.println(
              "   💡 403 error suggests insufficient permissions for user organization access");
        }
      }

    } catch (Exception e) {
      System.err.println("   ❌ Error in JWT user UUID extraction: " + e.getMessage());
      logger.error("Error in JWT user UUID extraction", e);
    }
  }

  /** Demonstrate the JWT token structure analysis for debugging. */
  public static void demonstrateJwtTokenAnalysis(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("🔍 JWT TOKEN ANALYSIS FOR DEBUGGING");
    System.out.println("============================================================");

    if (!client.isAuthenticated()) {
      System.out.println("❌ Not authenticated - cannot analyze JWT token");
      return;
    }

    try {
      AuthService authService = client.getAuthService();
      if (authService instanceof AuthServiceImpl) {
        AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;

        String tokenStatus = authServiceImpl.getTokenStatus();
        System.out.println("📊 Token Status: " + tokenStatus);

        // Get the current token for analysis
        String currentToken = authServiceImpl.getValidToken();
        if (currentToken != null) {
          System.out.println("✅ Current token available for analysis");

          // Analyze token structure
          analyzeJwtTokenStructure(currentToken);

          // Test user ID extraction
          Optional<UUID> extractedUserId = authServiceImpl.getCurrentUserId();
          if (extractedUserId.isPresent()) {
            System.out.println("✅ User UUID extraction: " + extractedUserId.get());
          } else {
            System.out.println("❌ User UUID extraction failed");
            System.out.println("💡 Check the JWT payload claims for user identification");
          }
        } else {
          System.out.println("❌ No valid token available for analysis");
        }
      }
    } catch (Exception e) {
      System.err.println("❌ Error in JWT token analysis: " + e.getMessage());
      logger.error("Error in JWT token analysis", e);
    }
  }

  /**
   * Analyze JWT token structure for debugging purposes.
   *
   * @param token JWT token string
   */
  private static void analyzeJwtTokenStructure(String token) {
    try {
      System.out.println("\n🔬 JWT Token Structure Analysis:");

      String[] parts = token.split("\\.");
      System.out.println("   📋 Token parts: " + parts.length + " (expected: 3 for JWT)");

      if (parts.length >= 2) {
        // Decode and display payload
        String payload = new String(java.util.Base64.getUrlDecoder().decode(parts[1]));
        System.out.println(
            "   📄 JWT Payload (first 200 chars): "
                + payload.substring(0, Math.min(200, payload.length()))
                + "...");

        // Look for common user ID claims
        System.out.println("   🔍 Checking for user ID claims:");
        checkForClaim(payload, "sub");
        checkForClaim(payload, "userId");
        checkForClaim(payload, "user_id");
        checkForClaim(payload, "id");
        checkForClaim(payload, "email");
        checkForClaim(payload, "username");
      }

    } catch (Exception e) {
      System.err.println("   ❌ Failed to analyze token structure: " + e.getMessage());
    }
  }

  /**
   * Check if a specific claim exists in the JWT payload.
   *
   * @param payload JWT payload string
   * @param claimName Name of the claim to check
   */
  private static void checkForClaim(String payload, String claimName) {
    if (payload.contains("\"" + claimName + "\"")) {
      try {
        // Simple regex to extract claim value
        java.util.regex.Pattern pattern =
            java.util.regex.Pattern.compile("\"" + claimName + "\"\\s*:\\s*\"?([^,}\"]+)\"?");
        java.util.regex.Matcher matcher = pattern.matcher(payload);
        if (matcher.find()) {
          String value = matcher.group(1);
          System.out.println("      ✅ " + claimName + ": " + value);
        } else {
          System.out.println("      ⚠️  " + claimName + ": found but value extraction failed");
        }
      } catch (Exception e) {
        System.out.println("      ❌ " + claimName + ": error extracting value");
      }
    } else {
      System.out.println("      ❌ " + claimName + ": not found");
    }
  }

  /** Main method to run enhanced organization examples with real JWT user UUID extraction. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 ENHANCED BIM PORTAL ORGANIZATION API EXAMPLES");
    System.out.println("   📋 Features: Real JWT User UUID Extraction");
    System.out.println("======================================================================");

    boolean hasCredentials = checkCredentials();

    System.out.println("🔧 Setting up client...");

    try {
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

      // Health check
      EnhancedBimPortalClient.HealthCheckResult healthCheck = client.performHealthCheck();
      System.out.println("🩺 Health check: " + healthCheck);

      if (!healthCheck.isApiAccessible()) {
        System.err.println("❌ API is not accessible. Please check configuration.");
        return;
      }

      // Run organization examples
      runOrganizationExamples(client);

      // Run JWT token analysis for debugging
      if (hasCredentials && client.isAuthenticated()) {
        demonstrateJwtTokenAnalysis(client);
      }

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ ENHANCED ORGANIZATION EXAMPLES COMPLETE!");
      System.out.println("======================================================================");

      if (hasCredentials && client.isAuthenticated()) {
        System.out.println("🎯 Key achievements:");
        System.out.println("   ✅ JWT token user UUID extraction implemented");
        System.out.println("   ✅ Real user organization API calls demonstrated");
        System.out.println("   ✅ Comprehensive error handling and debugging");

        AuthService authService = client.getAuthService();
        if (authService instanceof AuthServiceImpl) {
          AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;
          Optional<UUID> userId = authServiceImpl.getCurrentUserId();
          if (userId.isPresent()) {
            System.out.println("   ✅ Your user UUID: " + userId.get());
          }
        }
      } else {
        System.out.println("💡 Set up credentials to test authenticated organization endpoints");
        System.out.println("   and experience JWT user UUID extraction");
      }

      // Cleanup
      if (client.isAuthenticated()) {
        client.logout();
        System.out.println("👋 Logged out and session closed.");
      }

    } catch (Exception e) {
      logger.error("❌ Error running enhanced organization examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
