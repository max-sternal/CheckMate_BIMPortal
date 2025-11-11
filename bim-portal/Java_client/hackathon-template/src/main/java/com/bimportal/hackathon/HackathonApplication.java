package com.bimportal.hackathon;

import com.bimportal.hackathon.examples.BasicExample_old;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Main hackathon application entry point.
 *
 * <p>This template provides a starting point for hackathon participants to build innovative
 * solutions using the BIM Portal API.
 */
public class HackathonApplication {

  private static final Logger logger = LoggerFactory.getLogger(HackathonApplication.class);

  public static void main(String[] args) {
    System.out.println("🚀 BIM Portal Hackathon Template");
    System.out.println("==================================");

    // Check if credentials are configured
    if (!BimPortalConfig.hasCredentials()) {
      System.err.println("❌ Credentials not found!");
      System.err.println("Please create .env file with:");
      System.err.println("BIM_PORTAL_USERNAME=your_username");
      System.err.println("BIM_PORTAL_PASSWORD=your_password");
      return;
    }

    try {
      // Create the enhanced BIM Portal client
      System.out.println("🔧 Initializing BIM Portal client...");
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

      // Perform health check
      EnhancedBimPortalClient.HealthCheckResult healthCheck = client.performHealthCheck();
      System.out.println("🩺 Health check: " + healthCheck);

      if (!healthCheck.isApiAccessible()) {
        System.err.println("❌ API is not accessible. Check configuration.");
        return;
      }

      // 🚀 Build your innovative solution here!
      // Run the hackathon demonstration
      BasicExample_old.demonstrateBasicLoinIdsExport(client);

      // Clean up
      client.logout();
      System.out.println("👋 Session ended successfully.");

    } catch (Exception e) {
      logger.error("❌ Error in hackathon application", e);
      System.err.println("❌ Error: " + e.getMessage());
    }
  }
}
