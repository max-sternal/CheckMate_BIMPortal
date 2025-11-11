package com.pb40.bimportal.examples;

import com.bimportal.client.model.SimpleAiaTemplatePublicDto;
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
 * AIA template export examples for BIM Portal Java client.
 *
 * <p>Demonstrates AIA template export workflows in multiple formats including PDF and OpenOffice
 * with automatic file type detection.
 */
public class TemplateExportExample {

  private static final Logger logger = LoggerFactory.getLogger(TemplateExportExample.class);

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
   * Run AIA template export examples.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runTemplateExportExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("📋 AIA TEMPLATE EXPORT EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Searching for available templates...");
    try {
      List<SimpleAiaTemplatePublicDto> templates = client.searchTemplates();
      if (templates.isEmpty()) {
        System.out.println("🔭 No templates found for export");
        return;
      }

      System.out.println("✅ Found " + templates.size() + " templates:");
      for (int i = 0; i < Math.min(3, templates.size()); i++) {
        SimpleAiaTemplatePublicDto template = templates.get(i);
        System.out.println(
            "   " + (i + 1) + ". " + template.getName() + " (" + template.getGuid() + ")");
      }

      SimpleAiaTemplatePublicDto selectedTemplate = templates.get(0);
      System.out.println("\n🎯 Using template: '" + selectedTemplate.getName() + "'");

      Map<String, Path> exportResults = new HashMap<>();

      System.out.println("\n2️⃣ Exporting template...");

      // PDF Export with auto-detection
      System.out.println("   📄 Exporting as PDF...");
      Optional<byte[]> pdfContent = client.exportTemplatePdf(selectedTemplate.getGuid());
      if (pdfContent.isPresent()) {
        String baseFilename = "template_pdf_" + selectedTemplate.getGuid();
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
      Optional<byte[]> odtContent = client.exportTemplateOpenOffice(selectedTemplate.getGuid());
      if (odtContent.isPresent()) {
        String baseFilename = "template_odt_" + selectedTemplate.getGuid();
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

    } catch (Exception e) {
      logger.error("❌ Error in template export examples", e);
      System.err.println("❌ Error in template export examples: " + e.getMessage());
    }
  }

  /** Main method to run AIA template export examples. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 BIM PORTAL AIA TEMPLATE EXPORT EXAMPLES");
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

      runTemplateExportExamples(client);

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ AIA TEMPLATE EXPORT EXAMPLES COMPLETE!");
      System.out.println("======================================================================");
      System.out.println(
          "📂 Check the '" + BimPortalConfig.getExportPath() + "' directory for exported files");

      client.logout();
      System.out.println("👋 Logged out and session closed.");

    } catch (Exception e) {
      logger.error("❌ Error running template export examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
