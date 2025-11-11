//    * This test will systematically verify each search operation and help diagnose API
// discrepancies by
//    * for services with many optional query parameters.
//    * however It requires valid authentication (in .env file) to run successfully.
//    */
 package com.bimportal.hackathon.examples;

 import static org.junit.jupiter.api.Assertions.*;
 import static org.junit.jupiter.api.Assumptions.assumeTrue;

 import com.bimportal.client.api.InfrastrukturApi;
 import com.bimportal.client.model.*;
 import com.pb40.bimportal.auth.AuthService;
 import com.pb40.bimportal.auth.AuthServiceImpl;
 import com.pb40.bimportal.client.ApiClientFactory;
 import com.pb40.bimportal.client.BimPortalClientBuilder;
 import com.pb40.bimportal.client.EnhancedBimPortalClient;
 import java.util.*;
 import java.util.concurrent.atomic.AtomicInteger;
 import org.junit.jupiter.api.*;
 import org.junit.jupiter.api.extension.ExtendWith;
 import org.mockito.junit.jupiter.MockitoExtension;
 import org.slf4j.Logger;
 import org.slf4j.LoggerFactory;

/// **
// * Integration tests for BasicExample to identify and resolve search request data structure
// * mismatches.
// *
// * <p>These tests systematically verify each search operation and help diagnose API discrepancies
// by
// * testing different request parameter combinations and analyzing response structures.
// */
 @ExtendWith(MockitoExtension.class)
 @TestMethodOrder(MethodOrderer.OrderAnnotation.class)
 public class BasicExampleIntegrationTest {

  private static final Logger logger = LoggerFactory.getLogger(BasicExampleIntegrationTest.class);

  private static EnhancedBimPortalClient client;
  private static AuthService authService;
  private static boolean authenticationSuccessful = false;

  // Test counters for reporting
  private static final AtomicInteger successfulTests = new AtomicInteger(0);
  private static final AtomicInteger failedTests = new AtomicInteger(0);
  private static final List<String> detailedResults = new ArrayList<>();

  @BeforeAll
  static void setupIntegrationTest() {
    System.out.println("🧪 Starting BasicExample Integration Tests");
    System.out.println("============================================");

    try {
      // Initialize client and authenticate
      client = BimPortalClientBuilder.buildDefault();

      // Get auth service for detailed testing
      InfrastrukturApi infraApi = ApiClientFactory.createInfrastrukturApi();
      authService = new AuthServiceImpl(infraApi);

      // Verify authentication
      if (authService.hasCredentials()) {
        authService.login();
        authenticationSuccessful = authService.isAuthenticated();

        if (authenticationSuccessful) {
          System.out.println("✅ Authentication successful");
          System.out.println("📋 User ID: " + authService.getCurrentUserId());
          System.out.println("🔑 Token Status: " + authService.getTokenStatus());
        } else {
          System.err.println("❌ Authentication failed");
        }
      } else {
        System.err.println("❌ No credentials configured");
      }

    } catch (Exception e) {
      logger.error("Setup failed", e);
      System.err.println("❌ Setup error: " + e.getMessage());
    }
  }
//
  @Test
  @Order(1)
  @DisplayName("Test Authentication and Basic Client Functionality")
  void testAuthentication() {
    assumeTrue(authenticationSuccessful, "Authentication required for integration tests");

    try {
      assertTrue(authService.isAuthenticated(), "Should be authenticated");
      assertTrue(authService.hasCredentials(), "Should have credentials");

      String token = authService.getValidToken();
      assertNotNull(token, "Should have valid token");
      assertFalse(token.trim().isEmpty(), "Token should not be empty");

      recordSuccess("Authentication test passed");

    } catch (Exception e) {
      recordFailure("Authentication test failed: " + e.getMessage());
      fail("Authentication test failed", e);
    }
  }
//
//  @Test
//  @Order(2)
//  @DisplayName("Test Project Search - Basic and With Criteria")
//  void testProjectSearch() {
//    assumeTrue(authenticationSuccessful, "Authentication required");
//
//    // Test 1: Basic project search (no parameters)
//    try {
//      List<SimpleAiaProjectPublicDto> projects = client.searchProjects();
//      assertNotNull(projects, "Projects list should not be null");
//
//      System.out.println("📊 Basic project search: Found " + projects.size() + " projects");
//      recordSuccess("Basic project search successful - " + projects.size() + " projects found");
//
//      // Analyze project structure
//      if (!projects.isEmpty()) {
//        SimpleAiaProjectPublicDto firstProject = projects.getFirst();
//        analyzeProjectStructure(firstProject);
//      }
//
//    } catch (Exception e) {
//      recordFailure("Basic project search failed: " + e.getMessage());
//      logger.error("Basic project search error", e);
//    }
//
//    // Test 2: Project search with criteria - Test different request structures
//    testProjectSearchWithCriteria();
//  }
//
//  private void testProjectSearchWithCriteria() {
//    System.out.println("\n🔍 Testing project search with different criteria structures...");
//
//    // Test Case 1: Basic search string
//    try {
//      AiaProjectForPublicRequest request1 = new AiaProjectForPublicRequest();
//      request1.setSearchString("Beispiel");
//
//      List<SimpleAiaProjectPublicDto> results1 = client.searchProjects(request1);
//      System.out.println("✅ Search with 'Beispiel': " + results1.size() + " results");
//      recordSuccess("Project search with criteria (Beispiel) - " + results1.size() + " results");
//
//    } catch (Exception e) {
//      recordFailure("Project search with 'Beispiel' failed: " + e.getMessage());
//      logger.error("Project search with criteria error", e);
//    }
//
//    // Test Case 2: Empty search string
//    try {
//      AiaProjectForPublicRequest request2 = new AiaProjectForPublicRequest();
//      request2.setSearchString("");
//
//      List<SimpleAiaProjectPublicDto> results2 = client.searchProjects(request2);
//      System.out.println("✅ Search with empty string: " + results2.size() + " results");
//      recordSuccess("Project search with empty criteria - " + results2.size() + " results");
//
//    } catch (Exception e) {
//      recordFailure("Project search with empty string failed: " + e.getMessage());
//      logger.error("Project search with empty string error", e);
//    }
//
//    // Test Case 3: Test other possible parameters
//    testProjectRequestParameters();
//  }
//
//  private void testProjectRequestParameters() {
//    System.out.println("\n🔬 Analyzing AiaProjectForPublicRequest parameters...");
//
//    try {
//      AiaProjectForPublicRequest request = new AiaProjectForPublicRequest();
//
//      // Log available methods to understand the structure
//      analyzeRequestObjectStructure(request);
//
//      // Test different parameter combinations
//      String[] testSearchStrings = {"Test", "Projekt", "BIM", "Beispiel"};
//
//      for (String searchString : testSearchStrings) {
//        try {
//          request.setSearchString(searchString);
//          List<SimpleAiaProjectPublicDto> results = client.searchProjects(request);
//          System.out.println("  📈 '" + searchString + "': " + results.size() + " results");
//
//        } catch (Exception e) {
//          System.out.println("  ❌ '" + searchString + "': " + e.getMessage());
//        }
//      }
//
//    } catch (Exception e) {
//      logger.error("Error analyzing project request parameters", e);
//    }
//  }
//
//  @Test
//  @Order(3)
//  @DisplayName("Test LOIN Search - Identify Data Structure Issues")
//  void testLoinSearch() {
//    assumeTrue(authenticationSuccessful, "Authentication required");
//
//    // Test 1: Basic LOIN search (no parameters)
//    try {
//      List<SimpleLoinPublicDto> loins = client.searchLoins();
//      assertNotNull(loins, "LOINs list should not be null");
//
//      System.out.println("📊 Basic LOIN search: Found " + loins.size() + " LOINs");
//      recordSuccess("Basic LOIN search successful - " + loins.size() + " LOINs found");
//
//      // Analyze LOIN structure
//      if (!loins.isEmpty()) {
//        SimpleLoinPublicDto firstLoin = loins.getFirst();
//        analyzeLoinStructure(firstLoin);
//      }
//
//    } catch (Exception e) {
//      recordFailure("Basic LOIN search failed: " + e.getMessage());
//      logger.error("Basic LOIN search error", e);
//    }
//
//    // Test 2: LOIN search with request - THIS IS WHERE THE MISMATCH LIKELY OCCURS
//    testLoinSearchWithRequest();
//  }
//
//  private void testLoinSearchWithRequest() {
//    System.out.println(
//        "\n🚨 Testing LOIN search with LoinForPublicRequest - POTENTIAL MISMATCH AREA");
//
//    try {
//      // First, let's see if LoinForPublicRequest exists and analyze its structure
//      System.out.println("🔍 Attempting to create LoinForPublicRequest...");
//
//      // This might fail if the class doesn't exist or has wrong structure
//      // LoinForPublicRequest loinRequest = new LoinForPublicRequest();
//
//      // For now, let's test what we expect the request structure to be
//      testExpectedLoinRequestStructure();
//
//    } catch (Exception e) {
//      recordFailure("LOIN request structure test failed: " + e.getMessage());
//      logger.error("LOIN request structure error", e);
//    }
//  }
//
//  private void testExpectedLoinRequestStructure() {
//    System.out.println("🔬 Analyzing expected LOIN request structure...");
//
//    // Test different approaches that might work
//    String[] testSearchTerms = {"IFC", "Geometry", "LOD", "Information"};
//
//    for (String searchTerm : testSearchTerms) {
//      System.out.println("  🧪 Testing search term: '" + searchTerm + "'");
//
//      // For now, document what we expect:
//      System.out.println("  📝 Expected structure:");
//      System.out.println("    - searchString: " + searchTerm);
//      System.out.println("    - Possibly other filters like visibility, type, etc.");
//    }
//
//    recordFailure("LoinForPublicRequest class structure needs verification");
//  }
//
//  @Test
//  @Order(4)
//  @DisplayName("Test Domain Model Search - Identify Data Structure Issues")
//  void testDomainModelSearch() {
//    assumeTrue(authenticationSuccessful, "Authentication required");
//
//    System.out.println("\n🚨 Testing Domain Model search - POTENTIAL MISMATCH AREA");
//
//    try {
//      // Test if AiaDomainSpecificModelForPublicRequest exists and works
//      System.out.println("🔍 Attempting to create AiaDomainSpecificModelForPublicRequest...");
//
//      testExpectedDomainModelRequestStructure();
//
//    } catch (Exception e) {
//      recordFailure("Domain model request structure test failed: " + e.getMessage());
//      logger.error("Domain model request structure error", e);
//    }
//  }
//
//  private void testExpectedDomainModelRequestStructure() {
//    System.out.println("🔬 Analyzing expected Domain Model request structure...");
//
//    // Document what we expect the structure to be
//    System.out.println("  📝 Expected AiaDomainSpecificModelForPublicRequest structure:");
//    System.out.println("    - searchString: for text-based filtering");
//    System.out.println("    - domainType: for domain-specific filtering");
//    System.out.println("    - visibility: for access control");
//    System.out.println("    - Other potential filters...");
//
//    recordFailure("AiaDomainSpecificModelForPublicRequest class structure needs verification");
//  }
//
//  @Test
//  @Order(5)
//  @DisplayName("Test Property Search - Verify Working Example")
//  void testPropertySearch() {
//    assumeTrue(authenticationSuccessful, "Authentication required");
//
//    try {
//      PropertyOrGroupForPublicRequest propertyRequest = new PropertyOrGroupForPublicRequest();
//      propertyRequest.setSearchString("IFC");
//
//      List<PropertyOrGroupForPublicDto> properties = client.searchProperties(propertyRequest);
//      assertNotNull(properties, "Properties list should not be null");
//
//      System.out.println("📊 Property search: Found " + properties.size() + " properties");
//      recordSuccess("Property search successful - " + properties.size() + " properties found");
//
//      // Analyze property structure
//      if (!properties.isEmpty()) {
//        PropertyOrGroupForPublicDto firstProperty = properties.getFirst();
//        analyzePropertyStructure(firstProperty);
//      }
//
//      // Test different search terms
//      testPropertySearchVariations();
//
//    } catch (Exception e) {
//      recordFailure("Property search failed: " + e.getMessage());
//      logger.error("Property search error", e);
//    }
//  }
//
//  private void testPropertySearchVariations() {
//    System.out.println("\n🔍 Testing property search variations...");
//
//    String[] testTerms = {"", "IFC", "Property", "Geometry", "Material"};
//
//    for (String term : testTerms) {
//      try {
//        PropertyOrGroupForPublicRequest request = new PropertyOrGroupForPublicRequest();
//        request.setSearchString(term);
//
//        List<PropertyOrGroupForPublicDto> results = client.searchProperties(request);
//        System.out.println("  📈 '" + term + "': " + results.size() + " results");
//
//      } catch (Exception e) {
//        System.out.println("  ❌ '" + term + "': " + e.getMessage());
//      }
//    }
//  }
//
//  @Test
//  @Order(6)
//  @DisplayName("Test Export Functionality")
//  void testExportFunctionality() {
//    assumeTrue(authenticationSuccessful, "Authentication required");
//
//    try {
//      Optional<SimpleAiaProjectPublicDto> exportableProject = client.findExportableProject();
//
//      if (exportableProject.isPresent()) {
//        SimpleAiaProjectPublicDto project = exportableProject.get();
//        System.out.println("📤 Testing exports with project: " + project.getName());
//
//        testProjectExports(project);
//        testLoinExports();
//
//      } else {
//        System.out.println("⚠️ No exportable projects found for testing");
//        recordFailure("No exportable projects available for export testing");
//      }
//
//    } catch (Exception e) {
//      recordFailure("Export functionality test failed: " + e.getMessage());
//      logger.error("Export test error", e);
//    }
//  }
//
//  private void testProjectExports(SimpleAiaProjectPublicDto project) {
//    System.out.println("🔧 Testing project export formats...");
//
//    // Test PDF export
//    try {
//      Optional<byte[]> pdfContent = client.exportProjectPdf(project.getGuid());
//      if (pdfContent.isPresent()) {
//        System.out.println("  ✅ PDF export: " + pdfContent.get().length + " bytes");
//        recordSuccess("PDF export successful");
//      } else {
//        System.out.println("  ⚠️ PDF export returned empty");
//        recordFailure("PDF export returned empty");
//      }
//    } catch (Exception e) {
//      System.out.println("  ❌ PDF export failed: " + e.getMessage());
//      recordFailure("PDF export failed: " + e.getMessage());
//    }
//
//    // Test OpenOffice export
//    try {
//      Optional<byte[]> odtContent = client.exportProjectOpenOffice(project.getGuid());
//      if (odtContent.isPresent()) {
//        System.out.println("  ✅ OpenOffice export: " + odtContent.get().length + " bytes");
//        recordSuccess("OpenOffice export successful");
//      } else {
//        System.out.println("  ⚠️ OpenOffice export returned empty");
//        recordFailure("OpenOffice export returned empty");
//      }
//    } catch (Exception e) {
//      System.out.println("  ❌ OpenOffice export failed: " + e.getMessage());
//      recordFailure("OpenOffice export failed: " + e.getMessage());
//    }
//  }
//
//  private void testLoinExports() {
//    System.out.println("🔧 Testing LOIN export formats...");
//
//    try {
//      List<SimpleLoinPublicDto> loins = client.searchLoins();
//
//      if (loins.isEmpty()) {
//        System.out.println("  ⚠️ No LOINs available for export testing");
//        return;
//      }
//
//      // Test IDS export with first available LOIN
//      SimpleLoinPublicDto testLoin = loins.getFirst();
//      System.out.println("  🧪 Testing IDS export with LOIN: " + testLoin.getName());
//
//      try {
//        Optional<byte[]> idsContent = client.exportLoinIds(testLoin.getGuid());
//        if (idsContent.isPresent()) {
//          System.out.println("  ✅ IDS export: " + idsContent.get().length + " bytes");
//          recordSuccess("IDS export successful");
//        } else {
//          System.out.println("  ⚠️ IDS export returned empty");
//          recordFailure("IDS export returned empty");
//        }
//      } catch (Exception e) {
//        System.out.println("  ❌ IDS export failed: " + e.getMessage());
//        recordFailure("IDS export failed: " + e.getMessage());
//      }
//
//    } catch (Exception e) {
//      System.out.println("  ❌ LOIN export test setup failed: " + e.getMessage());
//      recordFailure("LOIN export test setup failed: " + e.getMessage());
//    }
//  }
//
//  // Utility methods for structure analysis
//
//  private void analyzeProjectStructure(SimpleAiaProjectPublicDto project) {
//    System.out.println("🔍 Project Structure Analysis:");
//    System.out.println("  📝 Name: " + project.getName());
//    System.out.println("  🔑 GUID: " + project.getGuid());
//    System.out.println(
//        "  📄 Description: "
//            + (project.getDescription() != null ? project.getDescription() : "N/A"));
//
//    // Use reflection to find all available methods/properties
//    analyzeObjectStructure(project, "SimpleAiaProjectPublicDto");
//  }
//
//  private void analyzeLoinStructure(SimpleLoinPublicDto loin) {
//    System.out.println("🔍 LOIN Structure Analysis:");
//    System.out.println("  📝 Name: " + loin.getName());
//    System.out.println("  🔑 GUID: " + loin.getGuid());
//    System.out.println(
//        "  👁️ Visibility: " + (loin.getVisibility() != null ? loin.getVisibility() : "N/A"));
//
//    analyzeObjectStructure(loin, "SimpleLoinPublicDto");
//  }
//
//  private void analyzePropertyStructure(PropertyOrGroupForPublicDto property) {
//    System.out.println("🔍 Property Structure Analysis:");
//    System.out.println("  📝 Name: " + property.getName());
//    System.out.println("  🔑 GUID: " + property.getGuid());
//    System.out.println("  🏷️ Data Type: " + property.getDataType());
//
//    analyzeObjectStructure(property, "PropertyOrGroupForPublicDto");
//  }
//
//  private void analyzeObjectStructure(Object obj, String className) {
//    try {
//      System.out.println("  🔬 " + className + " Methods:");
//      Arrays.stream(obj.getClass().getMethods())
//          .filter(method -> method.getName().startsWith("get") ||
// method.getName().startsWith("is"))
//          .filter(method -> method.getParameterCount() == 0)
//          .sorted((a, b) -> a.getName().compareTo(b.getName()))
//          .forEach(
//              method -> {
//                try {
//                  Object value = method.invoke(obj);
//                  System.out.println(
//                      "    "
//                          + method.getName()
//                          + "(): "
//                          + (value != null ? value.toString() : "null"));
//                } catch (Exception e) {
//                  System.out.println("    " + method.getName() + "(): ERROR - " + e.getMessage());
//                }
//              });
//    } catch (Exception e) {
//      System.out.println("  ❌ Structure analysis failed: " + e.getMessage());
//    }
//  }
//
//  private void analyzeRequestObjectStructure(Object request) {
//    System.out.println("  🔬 " + "AiaProjectForPublicRequest" + " Available Methods:");
//    Arrays.stream(request.getClass().getMethods())
//        .filter(
//            method ->
//                method.getName().startsWith("set")
//                    || method.getName().startsWith("get")
//                    || method.getName().startsWith("is"))
//        .sorted((a, b) -> a.getName().compareTo(b.getName()))
//        .forEach(method -> System.out.println("    " + method.getName() + "()"));
//  }
//
//  // Test result tracking
//
  private void recordSuccess(String message) {
    successfulTests.incrementAndGet();
    detailedResults.add("✅ " + message);
    System.out.println("✅ " + message);
  }
//
  private void recordFailure(String message) {
    failedTests.incrementAndGet();
    detailedResults.add("❌ " + message);
    System.out.println("❌ " + message);
  }
//
//  @AfterAll
//  static void teardownAndReport() {
//    System.out.println("\n📊 Integration Test Results Summary");
//    System.out.println("=====================================");
//    System.out.println("✅ Successful tests: " + successfulTests.get());
//    System.out.println("❌ Failed tests: " + failedTests.get());
//    System.out.println("📈 Total tests: " + (successfulTests.get() + failedTests.get()));
//
//    if (!detailedResults.isEmpty()) {
//      System.out.println("\n📋 Detailed Results:");
//      detailedResults.forEach(System.out::println);
//    }
//
//    // Specific recommendations for the data structure mismatches
//    System.out.println("\n🔧 Recommendations for Data Structure Mismatches:");
//    System.out.println("================================================");
//    System.out.println("1. 🚨 CRITICAL: Verify LoinForPublicRequest class exists and structure");
//    System.out.println("   - Check if the class name matches the API specification");
//    System.out.println("   - Verify field names and types match expected parameters");
//    System.out.println("   - Consider if it should be LoinSearchRequest or similar");
//
//    System.out.println("2. 🚨 CRITICAL: Verify AiaDomainSpecificModelForPublicRequest class");
//    System.out.println("   - Check if the class name matches the API specification");
//    System.out.println("   - Verify field names and types match expected parameters");
//    System.out.println("   - Consider if it should be DomainModelSearchRequest or similar");
//
//    System.out.println("3. ✅ PropertyOrGroupForPublicRequest appears to work correctly");
//    System.out.println("   - Use this as a reference for expected request structure patterns");
//
//    System.out.println("4. 🔍 Next Steps:");
//    System.out.println("   - Compare generated client DTOs with API documentation");
//    System.out.println("   - Verify OpenAPI specification matches implementation");
//    System.out.println("   - Check for version mismatches between client and server");
//    System.out.println("   - Consider regenerating client from latest API specification");
//
//    // Cleanup
//    if (client != null) {
//      try {
//        client.logout();
//        System.out.println("🚪 Client logged out successfully");
//      } catch (Exception e) {
//        System.err.println("⚠️ Logout warning: " + e.getMessage());
//      }
//    }
//  }
 }
