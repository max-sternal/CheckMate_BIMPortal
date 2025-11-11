package com.pb40.bimportal.examples;

import com.bimportal.client.model.*;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;
import com.pb40.bimportal.util.ExportUtils;
import java.nio.file.Path;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Context information export examples for BIM Portal Java client. Demonstrates context information
 * export workflows in multiple formats including PDF and OpenOffice with automatic file type
 * detection.
 */
public class ContextInfoExportExample {

  private static final Logger logger = LoggerFactory.getLogger(ContextInfoExportExample.class);

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
   * Run context information export examples with the fixed search method.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runContextInfoExportExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("📚 CONTEXT INFORMATION EXPORT EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Searching for available context information...");
    try {
      List<SimpleContextInfoPublicDto> contextInfos = client.searchContextInfo();
      if (contextInfos.isEmpty()) {
        System.out.println("🔭 No context information found for export");
        return;
      }

      System.out.println("✅ Found " + contextInfos.size() + " context information items:");
      for (int i = 0; i < Math.min(5, contextInfos.size()); i++) {
        SimpleContextInfoPublicDto context = contextInfos.get(i);
        System.out.println(
            "   " + (i + 1) + ". " + context.getName() + " (" + context.getGuid() + ")");
      }

      SimpleContextInfoPublicDto selectedContext = contextInfos.getFirst();
      System.out.println("\n🎯 Using context info: '" + selectedContext.getName() + "'");

      Map<String, Path> exportResults = new HashMap<>();

      System.out.println("\n2️⃣ Exporting context information...");

      // PDF Export with auto-detection
      System.out.println("   📄 Exporting as PDF...");
      Optional<byte[]> pdfContent = client.exportContextInfoPdf(selectedContext.getGuid());
      if (pdfContent.isPresent()) {
        String baseFilename = "context_pdf_" + selectedContext.getGuid();
        Optional<Path> pdfPath =
            ExportUtils.exportWithDetection(pdfContent.get(), baseFilename, "pdf");
        if (pdfPath.isPresent()) {
          exportResults.put("PDF", pdfPath.get());
          System.out.println("   ✅ PDF exported: " + pdfPath.get());
        } else {
          System.out.println("   ❌ PDF export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ PDF export failed: No content received");
      }

      // OpenOffice Export with auto-detection
      System.out.println("   📝 Exporting as OpenOffice...");
      Optional<byte[]> odtContent = client.exportContextInfoOpenOffice(selectedContext.getGuid());
      if (odtContent.isPresent()) {
        String baseFilename = "context_odt_" + selectedContext.getGuid();
        Optional<Path> odtPath =
            ExportUtils.exportWithDetection(odtContent.get(), baseFilename, "odt");
        if (odtPath.isPresent()) {
          exportResults.put("OpenOffice", odtPath.get());
          System.out.println("   ✅ OpenOffice exported: " + odtPath.get());
        } else {
          System.out.println("   ❌ OpenOffice export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ OpenOffice export failed: No content received");
      }

      // Summary with file type information
      System.out.println("\n📈 Export Summary: " + exportResults.size() + "/2 formats successful");
      for (Map.Entry<String, Path> entry : exportResults.entrySet()) {
        String filename = entry.getValue().getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
        System.out.println("   ✅ " + entry.getKey() + " (" + extension + "): " + entry.getValue());
      }

      // Additional context info details
      System.out.println("\n📋 Context Information Details:");
      try {
        Optional<AIAContextInfoPublicDto> contextDetails =
            client.getContextInfo(selectedContext.getGuid());
        if (contextDetails.isPresent()) {
          AIAContextInfoPublicDto details = contextDetails.get();
          System.out.println("   Name: " + details.getName());
          System.out.println("   GUID: " + details.getGuid());
          System.out.println(
              "   Description: "
                  + (details.getDescription() != null ? details.getDescription() : "N/A"));
        } else {
          System.out.println("   Could not retrieve detailed information");
        }
      } catch (Exception e) {
        System.out.println("   Error retrieving details: " + e.getMessage());
      }

    } catch (Exception e) {
      logger.error("❌ Error in context info export examples", e);
      System.err.println("❌ Error in context info export examples: " + e.getMessage());
    }
  }

  /** Main method to run context information export examples. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 BIM PORTAL CONTEXT INFORMATION EXPORT EXAMPLES");
    System.out.println("======================================================================");

    if (!checkCredentials()) {
      System.err.println("❌ Cannot run export examples without credentials");
      return;
    }

    if (!ExportUtils.isExportDirectoryWritable()) {
      System.err.println("❌ Export directory is not writable!");
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

      // Run the fixed context info export examples
      runContextInfoExportExamples(client);

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ CONTEXT INFORMATION EXPORT EXAMPLES COMPLETE!");
      System.out.println("======================================================================");
      System.out.println(
          "📂 Check the '" + BimPortalConfig.getExportPath() + "' directory for exported files");

      client.logout();
      System.out.println("👋 Logged out and session closed.");

    } catch (Exception e) {
      logger.error("❌ Error running context information export examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
