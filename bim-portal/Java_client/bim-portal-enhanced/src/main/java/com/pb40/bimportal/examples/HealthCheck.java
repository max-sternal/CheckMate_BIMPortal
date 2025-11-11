package com.pb40.bimportal.examples;

import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;

/**
 * Health check example to test BIM Portal API connectivity and authentication This file should be
 * placed in: bim-portal-enhanced/src/main/java/com/pb40/bimportal/examples/
 */
public class HealthCheck {

  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("BIM PORTAL API HEALTH CHECK");
    System.out.println("======================================================================");

    try {
      // Display current configuration
      System.out.println("Configuration:");
      BimPortalConfig.displayConfig(true); // true = mask sensitive data
      System.out.println();

      // Create enhanced client
      System.out.println("Creating BIM Portal client...");
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();
      System.out.println("Client created successfully.");
      System.out.println();

      // Perform health check
      System.out.println("Performing comprehensive health check...");
      EnhancedBimPortalClient.HealthCheckResult health = client.performHealthCheck();

      System.out.println("======================================================================");
      System.out.println("HEALTH CHECK RESULTS");
      System.out.println("======================================================================");

      System.out.println("API Accessible: " + (health.isApiAccessible() ? "✅ YES" : "❌ NO"));
      System.out.println("Authentication: " + (health.isAuthWorking() ? "✅ WORKING" : "❌ FAILED"));
      System.out.println("Status Message: " + health.getStatusMessage());

      if (health.getResponseTime() > 0) {
        System.out.println("Response Time: " + health.getResponseTime() + "ms");
      }

      if (health.getErrorDetails() != null) {
        System.out.println("Error Details: " + health.getErrorDetails());
      }

      System.out.println("======================================================================");

      // Provide guidance based on results
      if (health.isApiAccessible() && health.isAuthWorking()) {
        System.out.println("✅ SUCCESS: BIM Portal API is fully operational!");
        System.out.println("You can now proceed with API operations.");
      } else if (health.isApiAccessible() && !health.isAuthWorking()) {
        System.out.println("⚠️  PARTIAL: API is accessible but authentication failed.");
        System.out.println("Recommendations:");
        System.out.println("1. Verify credentials in .env file");
        System.out.println("2. Check if email address is verified in BIM Portal");
        System.out.println("3. Ensure account has API access permissions");
        System.out.println("4. Public API operations may still work");
      } else {
        System.out.println("❌ FAILURE: Cannot reach BIM Portal API.");
        System.out.println("Recommendations:");
        System.out.println("1. Check internet connectivity");
        System.out.println("2. Verify base URL in configuration");
        System.out.println("3. Check for firewall or proxy issues");
      }

    } catch (Exception e) {
      System.err.println("❌ Health check failed with exception:");
      System.err.println("Error: " + e.getMessage());

      if (e.getCause() != null) {
        System.err.println("Cause: " + e.getCause().getMessage());
      }

      System.err.println("\nThis typically indicates:");
      System.err.println("1. Configuration issues");
      System.err.println("2. Network connectivity problems");
      System.err.println("3. Missing dependencies");

      // Print stack trace for debugging
      System.err.println("\nFull error details:");
      e.printStackTrace();
    }

    System.out.println("\n======================================================================");
    System.out.println("Health check completed.");
    System.out.println("======================================================================");
  }
}
