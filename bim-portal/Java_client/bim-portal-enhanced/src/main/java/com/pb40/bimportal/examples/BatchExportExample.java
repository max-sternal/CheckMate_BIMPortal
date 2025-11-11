package com.pb40.bimportal.examples;

import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import com.bimportal.client.model.SimpleLoinPublicDto;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.config.BimPortalConfig;
import com.pb40.bimportal.util.ExportUtils;
import java.nio.file.Path;
import java.util.List;
import java.util.Optional;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Batch export examples for BIM Portal Java client.
 *
 * <p>Demonstrates batch export workflows for multiple projects and LOINs in PDF format for
 * efficient bulk processing.
 */
public class BatchExportExample {

  private static final Logger logger = LoggerFactory.getLogger(BatchExportExample.class);

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
   * Run batch export examples for projects and LOINs.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runBatchExportExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("📦 BATCH EXPORT EXAMPLES");
    System.out.println("============================================================");

    int totalExported = 0;

    System.out.println("\n1️⃣ Batch exporting projects as PDF...");
    try {
      List<SimpleAiaProjectPublicDto> projects = client.searchProjects();
      if (!projects.isEmpty()) {
        int projectCount = Math.min(3, projects.size());
        for (int i = 0; i < projectCount; i++) {
          SimpleAiaProjectPublicDto project = projects.get(i);
          System.out.println(
              "   🚧 Exporting project " + (i + 1) + "/" + projectCount + ": " + project.getName());

          Optional<byte[]> pdfContent = client.exportProjectPdf(project.getGuid());
          if (pdfContent.isPresent()) {
            String filename = "batch_project_" + (i + 1) + "_" + project.getGuid() + ".pdf";
            Optional<Path> savedPath = ExportUtils.saveExportFile(pdfContent.get(), filename);
            if (savedPath.isPresent()) {
              totalExported++;
              System.out.println("   ✅ Success: " + savedPath.get());
            } else {
              System.out.println("   ❌ Failed: File save error");
            }
          } else {
            System.out.println("   ❌ Failed: " + project.getName());
          }
        }
      } else {
        System.out.println("🔭 No projects available");
      }
    } catch (Exception e) {
      logger.error("❌ Error in project batch export", e);
      System.err.println("❌ Error in project batch export: " + e.getMessage());
    }

    System.out.println("\n2️⃣ Batch exporting LOINs as PDF...");
    try {
      List<SimpleLoinPublicDto> loins = client.searchLoins();
      if (!loins.isEmpty()) {
        int loinCount = Math.min(2, loins.size());
        for (int i = 0; i < loinCount; i++) {
          SimpleLoinPublicDto loin = loins.get(i);
          System.out.println(
              "   📊 Exporting LOIN " + (i + 1) + "/" + loinCount + ": " + loin.getName());

          Optional<byte[]> pdfContent = client.exportLoinPdf(loin.getGuid());
          if (pdfContent.isPresent()) {
            String filename = "batch_loin_" + (i + 1) + "_" + loin.getGuid() + ".pdf";
            Optional<Path> savedPath = ExportUtils.saveExportFile(pdfContent.get(), filename);
            if (savedPath.isPresent()) {
              totalExported++;
              System.out.println("   ✅ Success: " + savedPath.get());
            } else {
              System.out.println("   ❌ Failed: File save error");
            }
          } else {
            System.out.println("   ❌ Failed: " + loin.getName());
          }
        }
      } else {
        System.out.println("🔭 No LOINs available");
      }
    } catch (Exception e) {
      logger.error("❌ Error in LOIN batch export", e);
      System.err.println("❌ Error in LOIN batch export: " + e.getMessage());
    }

    System.out.println(
        "\n📊 BATCH EXPORT COMPLETE: " + totalExported + " files exported successfully");
  }

  /** Main method to run batch export examples. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 BIM PORTAL BATCH EXPORT EXAMPLES");
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

      runBatchExportExamples(client);

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ BATCH EXPORT EXAMPLES COMPLETE!");
      System.out.println("======================================================================");
      System.out.println(
          "📂 Check the '" + BimPortalConfig.getExportPath() + "' directory for exported files");

      client.logout();
      System.out.println("👋 Logged out and session closed.");

    } catch (Exception e) {
      logger.error("❌ Error running batch export examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
