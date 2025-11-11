package com.pb40.bimportal.examples;

import com.bimportal.client.model.*;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Search and filter examples for BIM Portal Java client.
 *
 * <p>Demonstrates search functionality for projects, properties, filters, and organizations
 * according to the actual Swagger API.
 */
public class SearchExample {

  private static final Logger logger = LoggerFactory.getLogger(SearchExample.class);

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
      System.out.println("============================================================");
      return false;
    }
    return true;
  }

  /**
   * Demonstrate search and filtering capabilities.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runSearchExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("🔍 SEARCH AND FILTER EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Searching projects with criteria...");
    try {
      AiaProjectForPublicRequest searchRequest = new AiaProjectForPublicRequest();
      searchRequest.setSearchString("Beispiel");
      List<SimpleAiaProjectPublicDto> projects = client.searchProjects(searchRequest);

      System.out.println("✅ Found " + projects.size() + " projects matching 'Beispiel':");
      for (SimpleAiaProjectPublicDto project : projects) {
        System.out.println("   📂 " + project.getName());
      }
    } catch (Exception e) {
      logger.error("❌ Error in project search example", e);
      System.err.println("❌ Error in project search: " + e.getMessage());
    }

    System.out.println("\n2️⃣ Searching properties with criteria...");
    try {
      PropertyOrGroupForPublicRequest propertyRequest = new PropertyOrGroupForPublicRequest();
      propertyRequest.setSearchString("IFC");
      List<PropertyOrGroupForPublicDto> properties = client.searchProperties(propertyRequest);

      System.out.println("✅ Found " + properties.size() + " properties matching 'IFC':");
      for (int i = 0; i < Math.min(5, properties.size()); i++) {
        PropertyOrGroupForPublicDto property = properties.get(i);
        System.out.println("   🔑 " + property.getName() + " (" + property.getDataType() + ")");
      }
    } catch (Exception e) {
      logger.error("❌ Error in property search example", e);
      System.err.println("❌ Error in property search: " + e.getMessage());
    }
  }

  /**
   * Demonstrate filter functionality.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runFilterExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("🔧 FILTER EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Getting AIA filters...");
    try {
      List<FilterGroupForPublicDto> aiaFilters = client.getAiaFilters();
      if (!aiaFilters.isEmpty()) {
        System.out.println("✅ Found " + aiaFilters.size() + " AIA filter groups:");
        for (int i = 0; i < Math.min(3, aiaFilters.size()); i++) {
          FilterGroupForPublicDto filterGroup = aiaFilters.get(i);
          System.out.println("   📂 " + filterGroup.getName());
          if (filterGroup.getFilter() != null && !filterGroup.getFilter().isEmpty()) {
            for (int j = 0; j < Math.min(2, filterGroup.getFilter().size()); j++) {
              FilterForPublicDto filter = filterGroup.getFilter().get(j);
              System.out.println("     - " + filter.getName());
            }
          }
        }
      } else {
        System.out.println("🔭 No AIA filters found");
      }
    } catch (Exception e) {
      logger.error("❌ Error getting AIA filters", e);
      System.err.println("❌ Error getting AIA filters: " + e.getMessage());
    }

    System.out.println("\n2️⃣ Getting property filters...");
    try {
      List<TagGroupForPublicDto> propertyFilters = client.getPropertyFilters();
      if (!propertyFilters.isEmpty()) {
        System.out.println("✅ Found " + propertyFilters.size() + " property filter groups:");
        for (int i = 0; i < Math.min(3, propertyFilters.size()); i++) {
          TagGroupForPublicDto filterGroup = propertyFilters.get(i);
          System.out.println("   📂 " + filterGroup.getName());
          if (filterGroup.getFilter() != null && !filterGroup.getFilter().isEmpty()) {
            for (int j = 0; j < Math.min(2, filterGroup.getFilter().size()); j++) {
              TagForPublicDto filter = filterGroup.getFilter().get(j);
              System.out.println("     - " + filter.getName());
            }
          }
        }
      } else {
        System.out.println("🔭 No property filters found");
      }
    } catch (Exception e) {
      logger.error("❌ Error getting property filters", e);
      System.err.println("❌ Error getting property filters: " + e.getMessage());
    }
  }

  /** Main method to run search and filter examples. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 BIM PORTAL SEARCH AND FILTER EXAMPLES");
    System.out.println("======================================================================");

    if (!checkCredentials()) {
      System.err.println("❌ Cannot run examples without credentials");
      return;
    }

    System.out.println("🔧 Setting up authenticated client...");

    try {
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

      EnhancedBimPortalClient.HealthCheckResult healthCheck = client.performHealthCheck();
      System.out.println("🩺 Health check: " + healthCheck);

      if (!healthCheck.isApiAccessible()) {
        System.err.println("❌ API is not accessible. Please check configuration.");
        return;
      }

      runSearchExamples(client); // 🔍 Search
      runFilterExamples(client); // 🔧 Filters

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ SEARCH AND FILTER EXAMPLES COMPLETE!");
      System.out.println("======================================================================");

      client.logout();
      System.out.println("👋 Logged out and session closed.");

    } catch (Exception e) {
      logger.error("❌ Error running search examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
