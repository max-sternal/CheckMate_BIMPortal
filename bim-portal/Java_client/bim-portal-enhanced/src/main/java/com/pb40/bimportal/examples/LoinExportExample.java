package com.pb40.bimportal.examples;

import com.bimportal.client.model.SimpleLoinPublicDto;
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
 * LOIN export examples for BIM Portal Java client.
 *
 * <p>Demonstrates LOIN export workflows in multiple formats including PDF, OpenOffice, OKSTRA,
 * LOIN-XML, and IDS with automatic content type detection.
 */
public class LoinExportExample {

  private static final Logger logger = LoggerFactory.getLogger(LoinExportExample.class);

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
   * Detect content type from byte array and return appropriate file extension.
   *
   * @param content The byte array content
   * @param defaultExtension Default extension if detection fails
   * @return Detected file extension
   */
  private static String detectFileExtension(byte[] content, String defaultExtension) {
    if (content == null || content.length < 4) {
      return defaultExtension;
    }

    // Check for ZIP file signature (PK)
    if (content.length >= 2 && content[0] == 0x50 && content[1] == 0x4B) {
      logger.debug("Detected ZIP file signature");
      return "zip";
    }

    // Check for PDF signature (%PDF)
    if (content.length >= 4
        && content[0] == 0x25
        && content[1] == 0x50
        && content[2] == 0x44
        && content[3] == 0x46) {
      logger.debug("Detected PDF file signature");
      return "pdf";
    }

    // Check for OpenDocument format (ZIP-based, but starts differently)
    if (content.length >= 4 && content[0] == 0x50 && content[1] == 0x4B) {
      // Additional check for OpenDocument by looking for mimetype file
      // This is a more sophisticated check for ODT files
      String contentStr = new String(content, 0, Math.min(1024, content.length));
      if (contentStr.contains("mimetypeapplication/vnd.oasis.opendocument")) {
        logger.debug("Detected OpenDocument format");
        return "odt";
      }
      logger.debug("Detected ZIP-based format (possibly ODT or other)");
      return "zip";
    }

    // Check for XML content (IDS files are typically XML)
    if (content.length >= 5) {
      String start = new String(content, 0, Math.min(100, content.length));
      if (start.trim().startsWith("<?xml") || start.contains("<ids") || start.contains("<loin")) {
        logger.debug("Detected XML format");
        return "xml";
      }
    }

    logger.debug("Could not detect file type, using default: {}", defaultExtension);
    return defaultExtension;
  }

  /**
   * Export content with automatic file type detection.
   *
   * @param content The byte array content
   * @param baseFilename Base filename without extension
   * @param expectedExtension Expected file extension for logging
   * @return Optional path to saved file
   */
  private static Optional<Path> exportWithDetection(
      byte[] content, String baseFilename, String expectedExtension) {
    String detectedExtension = detectFileExtension(content, expectedExtension);
    String filename = baseFilename + "." + detectedExtension;

    if (!detectedExtension.equals(expectedExtension)) {
      logger.info(
          "Content type detection: expected '{}' but detected '{}' for {}",
          expectedExtension,
          detectedExtension,
          baseFilename);
    }

    return ExportUtils.saveExportFile(content, filename);
  }

  /**
   * Run LOIN export examples.
   *
   * @param client Enhanced BIM Portal client
   */
  public static void runLoinExportExamples(EnhancedBimPortalClient client) {
    System.out.println("\n============================================================");
    System.out.println("📋 LOIN EXPORT EXAMPLES");
    System.out.println("============================================================");

    System.out.println("\n1️⃣ Searching for available LOINs...");
    try {
      List<SimpleLoinPublicDto> loins = client.searchLoins();
      if (loins.isEmpty()) {
        System.out.println("🔭 No LOINs found for export");
        return;
      }

      System.out.println("✅ Found " + loins.size() + " LOINs:");
      for (int i = 0; i < Math.min(3, loins.size()); i++) {
        SimpleLoinPublicDto loin = loins.get(i);
        System.out.println("   " + (i + 1) + ". " + loin.getName() + " (" + loin.getGuid() + ")");
      }

      SimpleLoinPublicDto selectedLoin = loins.get(0);
      System.out.println("\n🎯 Using LOIN: '" + selectedLoin.getName() + "'");

      Map<String, Path> exportResults = new HashMap<>();

      System.out.println("\n2️⃣ Exporting LOIN in multiple formats...");

      // PDF Export with auto-detection
      System.out.println("   📄 Exporting as PDF...");
      Optional<byte[]> pdfContent = client.exportLoinPdf(selectedLoin.getGuid());
      if (pdfContent.isPresent()) {
        String baseFilename = "loin_pdf_" + selectedLoin.getGuid();
        Optional<Path> pdfPath = exportWithDetection(pdfContent.get(), baseFilename, "pdf");
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
      Optional<byte[]> odtContent = client.exportLoinOpenOffice(selectedLoin.getGuid());
      if (odtContent.isPresent()) {
        String baseFilename = "loin_odt_" + selectedLoin.getGuid();
        Optional<Path> odtPath = exportWithDetection(odtContent.get(), baseFilename, "odt");
        if (odtPath.isPresent()) {
          exportResults.put("OpenOffice", odtPath.get());
          System.out.println("   ✅ OpenOffice exported: " + odtPath.get());
        } else {
          System.out.println("   ❌ OpenOffice export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ OpenOffice export failed: No content received");
      }

      // OKSTRA Export with auto-detection
      System.out.println("   🏗️ Exporting as OKSTRA...");
      Optional<byte[]> okstraContent = client.exportLoinOkstra(selectedLoin.getGuid());
      if (okstraContent.isPresent()) {
        String baseFilename = "loin_okstra_" + selectedLoin.getGuid();
        Optional<Path> okstraPath = exportWithDetection(okstraContent.get(), baseFilename, "zip");
        if (okstraPath.isPresent()) {
          exportResults.put("OKSTRA", okstraPath.get());
          System.out.println("   ✅ OKSTRA exported: " + okstraPath.get());
        } else {
          System.out.println("   ❌ OKSTRA export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ OKSTRA export failed: No content received");
      }

      // LOIN-XML Export with auto-detection
      System.out.println("   🔗 Exporting as LOIN-XML...");
      Optional<byte[]> xmlContent = client.exportLoinXml(selectedLoin.getGuid());
      if (xmlContent.isPresent()) {
        String baseFilename = "loin_xml_" + selectedLoin.getGuid();
        Optional<Path> xmlPath = exportWithDetection(xmlContent.get(), baseFilename, "xml");
        if (xmlPath.isPresent()) {
          exportResults.put("LOIN-XML", xmlPath.get());
          System.out.println("   ✅ LOIN-XML exported: " + xmlPath.get());
        } else {
          System.out.println("   ❌ LOIN-XML export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ LOIN-XML export failed: No content received");
      }

      // IDS Export with auto-detection
      System.out.println("   🆔 Exporting as IDS...");
      Optional<byte[]> idsContent = client.exportLoinIds(selectedLoin.getGuid());
      if (idsContent.isPresent()) {
        String baseFilename = "loin_ids_" + selectedLoin.getGuid();
        Optional<Path> idsPath = exportWithDetection(idsContent.get(), baseFilename, "ids");
        if (idsPath.isPresent()) {
          exportResults.put("IDS", idsPath.get());
          System.out.println("   ✅ IDS exported: " + idsPath.get());
        } else {
          System.out.println("   ❌ IDS export failed: Could not save file");
        }
      } else {
        System.out.println("   ❌ IDS export failed: No content received");
      }

      // Summary with file type information
      System.out.println("\n📈 Export Summary: " + exportResults.size() + "/5 formats successful");
      for (Map.Entry<String, Path> entry : exportResults.entrySet()) {
        String filename = entry.getValue().getFileName().toString();
        String extension = filename.substring(filename.lastIndexOf('.') + 1).toUpperCase();
        System.out.println("   ✅ " + entry.getKey() + " (" + extension + "): " + entry.getValue());
      }

      if (exportResults.size() < 3) {
        System.out.println(
            "💡 Note: Some export formats may require special permissions or project setup");
        System.out.println(
            "💡 Content type detection helps ensure correct file extensions are used");
      }

    } catch (Exception e) {
      logger.error("❌ Error in LOIN export examples", e);
      System.err.println("❌ Error in LOIN export examples: " + e.getMessage());
    }
  }

  /** Main method to run LOIN export examples. */
  public static void main(String[] args) {
    System.out.println("======================================================================");
    System.out.println("🚀 BIM PORTAL LOIN EXPORT EXAMPLES");
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

      runLoinExportExamples(client);

      System.out.println(
          "\n======================================================================");
      System.out.println("✅ LOIN EXPORT EXAMPLES COMPLETE!");
      System.out.println("======================================================================");
      System.out.println(
          "📂 Check the '" + BimPortalConfig.getExportPath() + "' directory for exported files");

      client.logout();
      System.out.println("👋 Logged out and session closed.");

    } catch (Exception e) {
      logger.error("❌ Error running LOIN export examples", e);
      System.err.println("❌ Error running examples: " + e.getMessage());
    }
  }
}
