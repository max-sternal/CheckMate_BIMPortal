package com.bimportal.hackathon.examples;

import com.bimportal.client.model.*;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import com.pb40.bimportal.util.ExportUtils;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic PDF export example for Domain Models and LOINs.
 *
 * <p>This example demonstrates how to: - Search for Domain Models and LOINs - Export them as PDF
 * files (may return ZIP if multiple PDFs) - Handle exports efficiently - Save files with meaningful
 * names
 */
public class BasicPdfExample {

  private static final Logger logger = LoggerFactory.getLogger(BasicPdfExample.class);

  public static void main(String[] args) {
    System.out.println("📄 Basic PDF Export Example");
    System.out.println("============================");

    try {
      // Initialize client
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

      // Run PDF export examples
      demonstrateLoinPdfExport(client);
      demonstrateDomainModelPdfExport(client);

      // Clean up
      client.logout();
      System.out.println("✅ PDF export example completed successfully!");

    } catch (Exception e) {
      logger.error("Error in PDF export example", e);
      System.err.println("❌ Error: " + e.getMessage());
    }
  }

  /** Demonstrate LOIN PDF export functionality. */
  public static void demonstrateLoinPdfExport(EnhancedBimPortalClient client) {
    System.out.println("\n📋 LOIN PDF Export Example");
    System.out.println("---------------------------");

    try {
      List<SimpleLoinPublicDto> loins = client.searchLoins();
      System.out.println("Found " + loins.size() + " LOINs");

      if (loins.isEmpty()) {
        System.out.println("⚠️ No LOINs available for PDF export");
        return;
      }

      // Export first 3 LOINs
      int exportCount = Math.min(3, loins.size());
      int successfulExports = 0;

      System.out.println("Attempting to export " + exportCount + " LOINs as PDF:");

      for (int i = 0; i < exportCount; i++) {
        SimpleLoinPublicDto loin = loins.get(i);
        System.out.println("\n" + (i + 1) + ". Exporting LOIN: " + loin.getName());
        System.out.println("   GUID: " + loin.getGuid());

        try {
          Optional<byte[]> exportContent = client.exportLoinPdf(loin.getGuid());

          if (exportContent.isPresent()) {
            byte[] content = exportContent.get();

            // Create filename with appropriate extension
            String sanitizedName = sanitizeFilename(loin.getName());
            String baseFilename = sanitizedName + "_" + loin.getGuid();

            // Detect file type and add correct extension
            String filename = detectAndAddExtension(content, baseFilename);
            String fileType = getFileType(content);

            Optional<Path> savedPath = ExportUtils.saveExportFile(content, filename);

            System.out.println("   ✅ Export successful!");
            System.out.println("   📊 File size: " + formatFileSize(content.length));
            System.out.println("   📄 File type: " + fileType);

            if (savedPath.isPresent()) {
              System.out.println("   💾 Saved to: " + savedPath.get());
            }

            successfulExports++;
          } else {
            System.out.println("   ❌ Export returned empty");
            System.out.println("   💡 This LOIN may not have export capabilities");
          }

        } catch (Exception exportError) {
          System.out.println("   ❌ Export failed: " + exportError.getMessage());
          logger.debug("LOIN PDF export error details", exportError);
        }
      }

      // Summary
      System.out.println("\n📈 LOIN Export Summary:");
      System.out.println("   Attempted: " + exportCount + " LOINs");
      System.out.println("   Successful: " + successfulExports + " exports");

      if (successfulExports > 0) {
        System.out.println("   ✅ LOIN export is working!");
        System.out.println(
            "   📝 Note: API returns PDF for single documents, ZIP for multiple documents");
      }

    } catch (Exception e) {
      logger.error("Error in LOIN PDF export demonstration", e);
      System.err.println("❌ LOIN PDF export error: " + e.getMessage());
    }
  }

  /** Demonstrate Domain Model PDF export functionality. */
  public static void demonstrateDomainModelPdfExport(EnhancedBimPortalClient client) {
    System.out.println("\n🏗️ Domain Model PDF Export Example");
    System.out.println("------------------------------------");

    try {
      List<SimpleDomainSpecificModelPublicDto> domainModels = client.searchDomainModels();
      System.out.println("Found " + domainModels.size() + " Domain Models");

      if (domainModels.isEmpty()) {
        System.out.println("⚠️ No Domain Models available for PDF export");
        return;
      }

      // Export all available Domain Models (or first 5 if many)
      int exportCount = Math.min(5, domainModels.size());
      int successfulExports = 0;

      System.out.println("Attempting to export " + exportCount + " Domain Models as PDF:");

      for (int i = 0; i < exportCount; i++) {
        SimpleDomainSpecificModelPublicDto domainModel = domainModels.get(i);
        System.out.println("\n" + (i + 1) + ". Exporting Domain Model: " + domainModel.getName());
        System.out.println("   GUID: " + domainModel.getGuid());

        try {
          Optional<byte[]> exportContent = client.exportDomainModelPdf(domainModel.getGuid());

          if (exportContent.isPresent()) {
            byte[] content = exportContent.get();

            // Create filename - ExportUtils will handle the extension
            String sanitizedName = sanitizeFilename(domainModel.getName());
            String baseFilename = sanitizedName + "_" + domainModel.getGuid();

            String filename = detectAndAddExtension(content, baseFilename);
            String fileType = getFileType(content);
            System.out.println("   📄 File type: " + fileType);

            Optional<Path> savedPath = ExportUtils.saveExportFile(content, filename);

            System.out.println("   ✅ Export successful!");
            System.out.println("   📊 File size: " + formatFileSize(content.length));

            if (savedPath.isPresent()) {
              System.out.println("   💾 Saved to: " + savedPath.get());
            }

            successfulExports++;
          } else {
            System.out.println("   ❌ Export returned empty");
            System.out.println("   💡 This Domain Model may not have export capabilities");
          }

        } catch (Exception exportError) {
          System.out.println("   ❌ Export failed: " + exportError.getMessage());
          logger.debug("Domain Model PDF export error details", exportError);
        }
      }

      // Summary
      System.out.println("\n📈 Domain Model Export Summary:");
      System.out.println("   Attempted: " + exportCount + " Domain Models");
      System.out.println("   Successful: " + successfulExports + " exports");

      if (successfulExports > 0) {
        System.out.println("   ✅ Domain Model export is working!");
        System.out.println(
            "   📝 Note: API returns PDF for single documents, ZIP for multiple documents");
      }

    } catch (Exception e) {
      logger.error("Error in Domain Model PDF export demonstration", e);
      System.err.println("❌ Domain Model PDF export error: " + e.getMessage());
    }
  }

  /**
   * Sanitize filename for safe file system usage.
   *
   * @param name Original name
   * @return Sanitized filename
   */
  private static String sanitizeFilename(String name) {
    if (name == null) {
      return "unnamed";
    }

    // Replace invalid characters with underscore
    String sanitized = name.replaceAll("[^a-zA-Z0-9._-]", "_");

    // Remove multiple consecutive underscores
    sanitized = sanitized.replaceAll("_{2,}", "_");

    // Remove leading/trailing underscores
    sanitized = sanitized.replaceAll("^_+|_+$", "");

    // Ensure not empty
    if (sanitized.isEmpty()) {
      sanitized = "unnamed";
    }

    // Limit length
    if (sanitized.length() > 50) {
      sanitized = sanitized.substring(0, 50);
    }

    return sanitized;
  }

  /**
   * Format file size in human-readable format.
   *
   * @param bytes File size in bytes
   * @return Formatted string
   */
  private static String formatFileSize(long bytes) {
    if (bytes < 1024) {
      return bytes + " bytes";
    } else if (bytes < 1024 * 1024) {
      return String.format("%.1f KB", bytes / 1024.0);
    } else {
      return String.format("%.1f MB", bytes / (1024.0 * 1024.0));
    }
  }

  /**
   * Detect file type based on file signature and add appropriate extension.
   *
   * @param content File content bytes
   * @param baseFilename Filename without extension
   * @return Filename with correct extension
   */
  private static String detectAndAddExtension(byte[] content, String baseFilename) {
    if (content == null || content.length < 4) {
      return baseFilename + ".unknown";
    }

    // Check for PDF signature (%PDF)
    if (content.length >= 4
        && content[0] == 0x25
        && content[1] == 0x50
        && content[2] == 0x44
        && content[3] == 0x46) {
      return baseFilename + ".pdf";
    }

    // Check for ZIP signature (PK)
    if (content.length >= 2 && content[0] == 0x50 && content[1] == 0x4B) {
      return baseFilename + ".zip";
    }

    // Default to PDF extension for unknown content
    return baseFilename + ".pdf";
  }

  /**
   * Get human-readable file type description.
   *
   * @param content File content bytes
   * @return File type description
   */
  private static String getFileType(byte[] content) {
    if (content == null || content.length < 4) {
      return "Unknown";
    }

    // Check for PDF signature (%PDF)
    if (content.length >= 4
        && content[0] == 0x25
        && content[1] == 0x50
        && content[2] == 0x44
        && content[3] == 0x46) {
      return "PDF (single document)";
    }

    // Check for ZIP signature (PK)
    if (content.length >= 2 && content[0] == 0x50 && content[1] == 0x4B) {
      return "ZIP (multiple PDFs)";
    }

    return "Unknown (saved as PDF)";
  }
}
