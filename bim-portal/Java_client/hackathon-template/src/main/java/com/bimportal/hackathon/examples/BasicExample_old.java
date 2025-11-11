package com.bimportal.hackathon.examples;

import com.bimportal.client.model.*;
import com.pb40.bimportal.client.BimPortalClientBuilder;
import com.pb40.bimportal.client.EnhancedBimPortalClient;
import java.nio.file.Path;
import java.util.*;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Basic example demonstrating core BIM Portal API functionality.
 *
 * <p>This example shows how to: - Authenticate with the API - Search for projects and LOINs -
 * Retrieve detailed information - Perform basic exports
 */
public class BasicExample_old {

  private static final Logger logger = LoggerFactory.getLogger(BasicExample_old.class);

  public static void main(String[] args) {
    System.out.println("🔰 Basic BIM Portal API Example");
    System.out.println("================================");

    try {
      // Initialize client
      EnhancedBimPortalClient client = BimPortalClientBuilder.buildDefault();

      // Run examples
      demonstrateProjectSearch(client);
      demonstrateLoinSearch(client);
      demonstratePropertySearch(client);
      demonstrateBasicExport(client);

      // Clean up
      client.logout();
      System.out.println("✅ Basic example completed successfully!");

    } catch (Exception e) {
      logger.error("Error in basic example", e);
      System.err.println("❌ Error: " + e.getMessage());
    }
  }

  /** Demonstrate project search and details retrieval. */
  public static void demonstrateProjectSearch(EnhancedBimPortalClient client) {
    System.out.println("\n📋 Project Search Example");
    System.out.println("--------------------------");

    try {
      // Search all projects
      List<SimpleAiaProjectPublicDto> projects = client.searchProjects();
      System.out.println("Found " + projects.size() + " projects");

      // Display first few projects
      for (int i = 0; i < Math.min(3, projects.size()); i++) {
        SimpleAiaProjectPublicDto project = projects.get(i);
        System.out.println((i + 1) + ". " + project.getName());
        System.out.println("   GUID: " + project.getGuid());
        System.out.println(
            "   Description: "
                + (project.getDescription() != null ? project.getDescription() : "N/A"));
      }

      // Search with criteria
      if (!projects.isEmpty()) {
        System.out.println("\n🔍 Searching with criteria...");
        AiaProjectForPublicRequest searchRequest = new AiaProjectForPublicRequest();
        searchRequest.setSearchString("Beispiel");

        List<SimpleAiaProjectPublicDto> filteredProjects = client.searchProjects(searchRequest);
        System.out.println("Found " + filteredProjects.size() + " projects matching 'Beispiel'");
      }

    } catch (Exception e) {
      logger.error("Error in project search", e);
      System.err.println("❌ Project search error: " + e.getMessage());
    }
  }

  /** Demonstrate LOIN search and analysis. */
  public static void demonstrateLoinSearch(EnhancedBimPortalClient client) {
    System.out.println("\n📊 LOIN Search Example");
    System.out.println("-----------------------");

    try {
      List<SimpleLoinPublicDto> loins = client.searchLoins();
      System.out.println("Found " + loins.size() + " LOINs");

      // Display LOIN information
      for (int i = 0; i < Math.min(3, loins.size()); i++) {
        SimpleLoinPublicDto loin = loins.get(i);
        System.out.println((i + 1) + ". " + loin.getName());
        System.out.println("   GUID: " + loin.getGuid());
        System.out.println(
            "   Visibility: " + (loin.getVisibility() != null ? loin.getVisibility() : "N/A"));
      }

    } catch (Exception e) {
      logger.error("Error in LOIN search", e);
      System.err.println("❌ LOIN search error: " + e.getMessage());
    }
  }

  /** Demonstrate basic LOIN IDS export functionality. */
  public static void demonstrateBasicLoinIdsExport(EnhancedBimPortalClient client) {
    System.out.println("\n🆔 LOIN IDS Export Example");
    System.out.println("--------------------------");

    try {
      List<SimpleLoinPublicDto> loins = client.searchLoins();

      if (loins.isEmpty()) {
        System.out.println("⚠️ No LOINs available for IDS export");
        return;
      }

      System.out.println("Found " + loins.size() + " LOINs for potential IDS export");

      // Filter LOINs to get different GUIDs and avoid duplicates
      Set<UUID> processedGuids = new HashSet<>();
      List<SimpleLoinPublicDto> uniqueLoins = new ArrayList<>();

      for (SimpleLoinPublicDto loin : loins) {
        if (loin.getGuid() != null && !processedGuids.contains(loin.getGuid())) {
          processedGuids.add(loin.getGuid());
          uniqueLoins.add(loin);
          if (uniqueLoins.size() >= 5) { // Try up to 5 different LOINs
            break;
          }
        }
      }

      if (uniqueLoins.isEmpty()) {
        System.out.println("⚠️ No LOINs with valid GUIDs found");
        return;
      }

      int exportAttempts = Math.min(3, uniqueLoins.size());
      int successfulExports = 0;
      Set<String> exportedFileNames = new HashSet<>();

      System.out.println("Selected " + exportAttempts + " LOINs with different GUIDs for export:");
      for (int i = 0; i < exportAttempts; i++) {
        SimpleLoinPublicDto loin = uniqueLoins.get(i);
        System.out.println(
            "   " + (i + 1) + ". " + loin.getName() + " (GUID: " + loin.getGuid() + ")");
      }

      for (int i = 0; i < exportAttempts; i++) {
        SimpleLoinPublicDto loin = uniqueLoins.get(i);
        System.out.println("\n" + (i + 1) + ". Attempting IDS export for LOIN: " + loin.getName());
        System.out.println("   GUID: " + loin.getGuid());

        try {
          Optional<byte[]> idsContent = client.exportLoinIds(loin.getGuid());

          if (idsContent.isPresent()) {
            byte[] content = idsContent.get();

            // Create unique filename with LOIN GUID
            String filename = "loin_" + loin.getGuid() + ".ids";

            // Ensure filename uniqueness (should be unique due to GUID filtering, but extra safety)
            int counter = 1;
            String originalFilename = filename;
            while (exportedFileNames.contains(filename)) {
              String baseName = originalFilename.substring(0, originalFilename.lastIndexOf('.'));
              String extension = originalFilename.substring(originalFilename.lastIndexOf('.'));
              filename = baseName + "_" + counter + extension;
              counter++;
            }
            exportedFileNames.add(filename);

            Optional<Path> savedPath =
                com.pb40.bimportal.util.ExportUtils.saveExportFile(content, filename);

            System.out.println("   ✅ IDS export successful!");
            System.out.println("   📊 File size: " + content.length + " bytes");
            System.out.println("   📁 Filename: " + filename);

            if (savedPath.isPresent()) {
              System.out.println("   💾 Saved to: " + savedPath.get());
            } else {
              System.out.println("   ⚠️ File saved but path unavailable");
            }

            // Basic content analysis
            String contentPreview = new String(content, 0, Math.min(200, content.length));
            if (contentPreview.contains("<ids")
                || contentPreview.contains("Information Delivery Specification")) {
              System.out.println("   🔍 Content appears to be valid IDS format");
            } else {
              System.out.println(
                  "   ⚠️ Content format unclear (first 200 chars): "
                      + contentPreview.replaceAll("[\\r\\n]", " "));
            }

            // Show GUID verification
            System.out.println("   🔗 Verified unique GUID: " + loin.getGuid());
            successfulExports++;
          } else {
            System.out.println("   ❌ IDS export returned empty");
            System.out.println(
                "   💡 This LOIN may not have IDS export capabilities or may require special permissions");
          }

        } catch (Exception exportError) {
          System.out.println("   ❌ IDS export failed: " + exportError.getMessage());
          logger.debug("IDS export error details", exportError);
        }
      }

      // Summary
      System.out.println("\n📈 IDS Export Summary:");
      System.out.println("   Attempted: " + exportAttempts + " LOINs with unique GUIDs");
      System.out.println("   Successful: " + successfulExports + " exports");
      System.out.println("   Unique files created: " + exportedFileNames.size());

      if (!exportedFileNames.isEmpty()) {
        System.out.println("   📁 Created files:");
        exportedFileNames.forEach(name -> System.out.println("      - " + name));
      }

      if (successfulExports > 0) {
        System.out.println("   ✅ IDS export functionality is working!");
        System.out.println("   💡 You can use these IDS files for:");
        System.out.println("      - Information requirements specification");
        System.out.println("      - BIM model validation");
        System.out.println("      - Digital construction workflows");
        System.out.println("   🔍 Each file represents a different LOIN with unique requirements");
      } else if (exportAttempts > 0) {
        System.out.println("   ⚠️ No successful IDS exports");
        System.out.println("   💡 This could be due to:");
        System.out.println("      - LOINs not having IDS export capabilities");
        System.out.println("      - Permission requirements");
        System.out.println("      - API access limitations");
        System.out.println(
            "   🔧 Try running other export formats (PDF, OpenOffice) to test basic export functionality");
      }

    } catch (Exception e) {
      logger.error("Error in LOIN IDS export demonstration", e);
      System.err.println("❌ LOIN IDS export error: " + e.getMessage());
    }
  }

  /** Demonstrate property and group search. */
  public static void demonstratePropertySearch(EnhancedBimPortalClient client) {
    System.out.println("\n🏷️ Property Search Example");
    System.out.println("---------------------------");

    try {
      PropertyOrGroupForPublicRequest propertyRequest = new PropertyOrGroupForPublicRequest();
      propertyRequest.setSearchString("IFC");

      List<PropertyOrGroupForPublicDto> properties = client.searchProperties(propertyRequest);
      System.out.println("Found " + properties.size() + " properties matching 'IFC'");

      // Display property information
      for (int i = 0; i < Math.min(5, properties.size()); i++) {
        PropertyOrGroupForPublicDto property = properties.get(i);
        System.out.println((i + 1) + ". " + property.getName());
        System.out.println("   Type: " + property.getDataType());
        System.out.println("   GUID: " + property.getGuid());
      }

    } catch (Exception e) {
      logger.error("Error in property search", e);
      System.err.println("❌ Property search error: " + e.getMessage());
    }
  }

  /** Demonstrate basic export functionality. */
  public static void demonstrateBasicExport(EnhancedBimPortalClient client) {
    System.out.println("\n📤 Basic Export Example");
    System.out.println("------------------------");

    try {
      // Find an exportable project
      Optional<SimpleAiaProjectPublicDto> exportableProject = client.findExportableProject();

      if (exportableProject.isPresent()) {
        SimpleAiaProjectPublicDto project = exportableProject.get();
        System.out.println("Testing export with project: " + project.getName());

        // Try PDF export
        Optional<byte[]> pdfContent = client.exportProjectPdf(project.getGuid());
        if (pdfContent.isPresent()) {
          System.out.println(
              "✅ PDF export successful (size: " + pdfContent.get().length + " bytes)");
        } else {
          System.out.println("⚠️ PDF export returned empty");
        }

        // Try OpenOffice export
        Optional<byte[]> odtContent = client.exportProjectOpenOffice(project.getGuid());
        if (odtContent.isPresent()) {
          System.out.println(
              "✅ OpenOffice export successful (size: " + odtContent.get().length + " bytes)");
        } else {
          System.out.println("⚠️ OpenOffice export returned empty");
        }

      } else {
        System.out.println("⚠️ No exportable projects found");
      }

    } catch (Exception e) {
      logger.error("Error in export example", e);
      System.err.println("❌ Export error: " + e.getMessage());
    }
  }
}
