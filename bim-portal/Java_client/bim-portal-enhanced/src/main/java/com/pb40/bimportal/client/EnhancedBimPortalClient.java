package com.pb40.bimportal.client;

import com.bimportal.client.ApiClient;
import com.bimportal.client.api.*;
import com.bimportal.client.model.*;
import com.bimportal.client.model.OrganisationForPublicDTO;
import com.pb40.bimportal.auth.AuthService;
import com.pb40.bimportal.auth.AuthServiceImpl;
import com.pb40.bimportal.auth.AuthenticationException;
import com.pb40.bimportal.config.BimPortalConfig;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.*;
import java.util.Collections;
import java.util.Objects;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Enhanced BIM Portal client with improved binary response handling. This version properly handles
 * PDF and other binary export formats.
 */
public class EnhancedBimPortalClient {

  private static final Logger logger = LoggerFactory.getLogger(EnhancedBimPortalClient.class);

  private final AuthService authService;
  private final ApiClient apiClient;

  // Generated API clients
  private final AiaProjekteApi projectsApi;
  private final AiaLoinApi loinApi;
  private final FachmodelleApi domainModelsApi;
  private final KontextinformationenApi contextInfoApi;
  private final AiaVorlagenApi templatesApi;
  private final MerkmaleApi propertiesApi;
  private final MerkmalsgruppenApi propertyGroupsApi;
  private final AiaFilterApi aiaFilterApi;
  private final InfrastrukturApi infraApi;

  /** Constructor with explicit auth service. */
  public EnhancedBimPortalClient(AuthService authService) {
    this.authService = authService;
    this.apiClient = createConfiguredApiClient();

    // Initialize all API clients using ApiClient's buildClient method
    this.projectsApi = apiClient.buildClient(AiaProjekteApi.class);
    this.loinApi = apiClient.buildClient(AiaLoinApi.class);
    this.domainModelsApi = apiClient.buildClient(FachmodelleApi.class);
    this.contextInfoApi = apiClient.buildClient(KontextinformationenApi.class);
    this.templatesApi = apiClient.buildClient(AiaVorlagenApi.class);
    this.propertiesApi = apiClient.buildClient(MerkmaleApi.class);
    this.propertyGroupsApi = apiClient.buildClient(MerkmalsgruppenApi.class);
    this.aiaFilterApi = apiClient.buildClient(AiaFilterApi.class);
    this.infraApi = apiClient.buildClient(InfrastrukturApi.class);
  }

  /** Constructor using default authentication from configuration. */
  public EnhancedBimPortalClient() {
    this.apiClient = createConfiguredApiClient();
    this.infraApi = apiClient.buildClient(InfrastrukturApi.class);
    this.authService = new AuthServiceImpl(infraApi);

    // Initialize remaining API clients
    this.projectsApi = apiClient.buildClient(AiaProjekteApi.class);
    this.loinApi = apiClient.buildClient(AiaLoinApi.class);
    this.domainModelsApi = apiClient.buildClient(FachmodelleApi.class);
    this.contextInfoApi = apiClient.buildClient(KontextinformationenApi.class);
    this.templatesApi = apiClient.buildClient(AiaVorlagenApi.class);
    this.propertiesApi = apiClient.buildClient(MerkmaleApi.class);
    this.propertyGroupsApi = apiClient.buildClient(MerkmalsgruppenApi.class);
    this.aiaFilterApi = apiClient.buildClient(AiaFilterApi.class);

    logger.info("Enhanced BIM Portal client initialized with default config");
  }

  /**
   * Get the authentication service instance.
   *
   * @return AuthService instance
   */
  public AuthService getAuthService() {
    return authService;
  }

  /** Create and configure the ApiClient with binary support. */
  private ApiClient createConfiguredApiClient() {
    ApiClient apiClient = new ApiClient("bearerAuth");
    apiClient.setBasePath(BimPortalConfig.getBaseUrl());

    // Configure the bearer token supplier to get fresh tokens
    apiClient.setBearerToken(
        () -> {
          try {
            return authService.getValidToken();
          } catch (Exception e) {
            logger.error("Failed to get valid token: {}", e.getMessage());
            throw new RuntimeException("Authentication failed", e);
          }
        });

    return apiClient;
  }

  /** Performs a comprehensive health check of the BIM Portal API */
  public HealthCheckResult performHealthCheck() {
    logger.info("Performing BIM Portal health check...");

    boolean apiAccessible;
    boolean authWorking;
    String statusMessage;
    String errorDetails = null;
    long responseTime = 0;

    try {
      long startTime = System.currentTimeMillis();

      try {
        String token = authService.getValidToken();
        responseTime = System.currentTimeMillis() - startTime;

        if (token != null && !token.isEmpty()) {
          apiAccessible = true;
          authWorking = true;
          statusMessage = "API accessible and authentication working";
        } else {
          apiAccessible = true;
          authWorking = false;
          statusMessage = "API accessible but authentication failed";
        }
      } catch (Exception authException) {
        responseTime = System.currentTimeMillis() - startTime;
        apiAccessible = true;
        authWorking = false;
        errorDetails = authException.getMessage();
        statusMessage = "Authentication failed - using public access only";

        logger.warn("Authentication failed during health check: {}", authException.getMessage());
      }

    } catch (Exception e) {
      apiAccessible = false;
      authWorking = false;
      errorDetails = e.getMessage();
      statusMessage = "API not accessible: " + e.getMessage();

      logger.error("Health check failed: {}", e.getMessage());
    }

    HealthCheckResult result =
        new HealthCheckResult(
            apiAccessible, authWorking, statusMessage, errorDetails, responseTime);

    logger.info(
        "Health check completed: API={}, Auth={}, Message={}",
        apiAccessible,
        authWorking,
        statusMessage);

    return result;
  }

  // === AUTHENTICATION METHODS ===

  /** Check if client is authenticated. */
  public boolean isAuthenticated() {
    return authService.isAuthenticated();
  }

  /** Get authentication status. */
  public String getAuthenticationStatus() {
    if (authService instanceof AuthServiceImpl) {
      return ((AuthServiceImpl) authService).getTokenStatus();
    }
    return authService.isAuthenticated() ? "Authenticated" : "Not authenticated";
  }

  /** Force logout and clear tokens. */
  public void logout() {
    authService.logout();
  }

  // === ORGANIZATION METHODS ===

  /**
   * Get all available organizations.
   *
   * @return List of all organizations accessible through the API
   * @throws RuntimeException if the operation fails
   */
  public List<OrganisationForPublicDTO> getAllOrganisations() {
    try {
      logger.debug("Fetching all available organizations");
      List<OrganisationForPublicDTO> organizations = infraApi.getOrganisationsForPublic();
      logger.info("Successfully retrieved {} organizations", organizations.size());
      return organizations;
    } catch (Exception e) {
      logger.error("Failed to fetch organizations", e);
      throw new RuntimeException("Failed to fetch organizations: " + e.getMessage(), e);
    }
  }

  /**
   * Get organizations where the current authenticated user is a member. Requires authentication and
   * assumes the user ID can be extracted from the token. Note: This is a convenience method that
   * would need the current user's UUID. The actual implementation depends on how the user ID is
   * obtained from the authentication context.
   *
   * @param currentUserUuid UUID of the current authenticated user
   * @return List of current user's organizations
   * @throws RuntimeException if the operation fails
   */
  public List<OrganisationForPublicDTO> getCurrentUserOrganisations(UUID currentUserUuid) {
    return getUserOrganisations(currentUserUuid);
  }

  /**
   * Find organization by GUID.
   *
   * @param guid Organization GUID to search for
   * @return Optional containing the organization if found
   */
  public Optional<OrganisationForPublicDTO> findOrganisationByGuid(String guid) {
    if (guid == null || guid.trim().isEmpty()) {
      return Optional.empty();
    }

    try {
      return getAllOrganisations().stream().filter(org -> guid.equals(org.getGuid())).findFirst();
    } catch (Exception e) {
      logger.warn("Failed to search for organization with GUID: {}", guid, e);
      return Optional.empty();
    }
  }

  /**
   * Find organization by name (case-insensitive).
   *
   * @param name Organization name to search for
   * @return Optional containing the organization if found
   */
  public Optional<OrganisationForPublicDTO> findOrganisationByName(String name) {
    if (name == null || name.trim().isEmpty()) {
      return Optional.empty();
    }

    try {
      return getAllOrganisations().stream()
          .filter(org -> org.getName() != null && name.equalsIgnoreCase(org.getName()))
          .findFirst();
    } catch (Exception e) {
      logger.warn("Failed to search for organization with name: {}", name, e);
      return Optional.empty();
    }
  }

  /**
   * Check if the specified user is member of a specific organization.
   *
   * @param userId UUID of the user to check
   * @param organizationGuid GUID of the organization to check
   * @return true if user is a member, false otherwise
   */
  public boolean isUserMemberOfOrganisation(UUID userId, String organizationGuid) {
    if (userId == null || organizationGuid == null || organizationGuid.trim().isEmpty()) {
      return false;
    }

    try {
      return getUserOrganisations(userId).stream()
          .anyMatch(org -> organizationGuid.equals(org.getGuid()));
    } catch (Exception e) {
      logger.warn(
          "Failed to check user membership for organization: {} and user: {}",
          organizationGuid,
          userId,
          e);
      return false;
    }
  }

  /**
   * Get organizations using query parameters. This method provides more flexibility for complex
   * queries.
   *
   * @param queryParams Query parameters for filtering organizations
   * @return List of organizations matching the criteria
   * @throws RuntimeException if the operation fails //
   */

  /**
   * Check if any organizations are available.
   *
   * @return true if there are organizations available, false otherwise
   */
  public boolean hasAvailableOrganisations() {
    try {
      return !getAllOrganisations().isEmpty();
    } catch (Exception e) {
      logger.warn("Failed to check if organizations are available", e);
      return false;
    }
  }

  // Add these methods to your existing EnhancedBimPortalClient class

  /**
   * Get organizations for the current authenticated user. Automatically extracts user UUID from JWT
   * token.
   *
   * @return List of user organizations
   * @throws RuntimeException if not authenticated or user UUID not available
   */
  public List<OrganisationForPublicDTO> getCurrentUserOrganisations() {
    AuthService authService = getAuthService();
    if (!(authService instanceof AuthServiceImpl)) {
      throw new RuntimeException("Enhanced auth service not available for user UUID extraction");
    }

    AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;
    try {
      UUID currentUserId = authServiceImpl.getCurrentUserIdRequired();
      return getUserOrganisations(currentUserId);
    } catch (AuthenticationException e) {
      throw new RuntimeException("Failed to get current user organizations: " + e.getMessage(), e);
    }
  }

  /**
   * Get organizations for a specific user by UUID.
   *
   * @param userId User UUID
   * @return List of organizations for the user
   */
  public List<OrganisationForPublicDTO> getUserOrganisations(UUID userId) {
    try {
      InfrastrukturApi.GetOrganisationsOfUserQueryParams params =
          createUserOrganisationQueryParams(userId);
      return getUserOrganisationsWithParams(params);
    } catch (Exception e) {
      logger.error("Error getting user organizations for user {}: {}", userId, e.getMessage());
      throw new RuntimeException("Failed to get user organizations: " + e.getMessage(), e);
    }
  }

  /**
   * Get organizations for a user using query parameters.
   *
   * @param queryParams Query parameters containing user ID
   * @return List of organizations for the user
   */
  public List<OrganisationForPublicDTO> getUserOrganisationsWithParams(
      InfrastrukturApi.GetOrganisationsOfUserQueryParams queryParams) {
    try {
      return infraApi.getOrganisationsOfUser(queryParams);
    } catch (Exception e) {
      logger.error("Error calling user organizations API: {}", e.getMessage());
      throw new RuntimeException("Failed to call user organizations API: " + e.getMessage(), e);
    }
  }

  /**
   * Create query parameters for user organization requests.
   *
   * @param userId User UUID
   * @return Query parameters object
   */
  public InfrastrukturApi.GetOrganisationsOfUserQueryParams createUserOrganisationQueryParams(
      UUID userId) {
    return new InfrastrukturApi.GetOrganisationsOfUserQueryParams().userId(userId);
  }

  /**
   * Check if a user is a member of a specific organization.
   *
   * @param userId User UUID
   * @param organizationGuid Organization GUID
   * @return True if user is a member of the organization
   */
  public boolean isUserMemberOfOrganisation(UUID userId, UUID organizationGuid) {
    try {
      List<OrganisationForPublicDTO> userOrgs = getUserOrganisations(userId);
      return userOrgs.stream().anyMatch(org -> org.getGuid().equals(organizationGuid));
    } catch (Exception e) {
      logger.error(
          "Error checking organization membership for user {}: {}", userId, e.getMessage());
      return false;
    }
  }

  /**
   * Check if the current user is a member of a specific organization.
   *
   * @param organizationGuid Organization GUID
   * @return True if current user is a member of the organization
   */
  public boolean isCurrentUserMemberOfOrganisation(UUID organizationGuid) {
    try {
      List<OrganisationForPublicDTO> userOrgs = getCurrentUserOrganisations();
      return userOrgs.stream().anyMatch(org -> org.getGuid().equals(organizationGuid));
    } catch (Exception e) {
      logger.error("Error checking organization membership for current user: {}", e.getMessage());
      return false;
    }
  }

  /**
   * Get count of organizations for a specific user.
   *
   * @param userId User UUID
   * @return Number of organizations the user belongs to
   */
  public int getUserOrganisationsCount(UUID userId) {
    try {
      return getUserOrganisations(userId).size();
    } catch (Exception e) {
      logger.error("Error getting organization count for user {}: {}", userId, e.getMessage());
      return 0;
    }
  }

  /**
   * Get count of organizations for the current user.
   *
   * @return Number of organizations the current user belongs to
   */
  public int getCurrentUserOrganisationsCount() {
    try {
      return getCurrentUserOrganisations().size();
    } catch (Exception e) {
      logger.error("Error getting organization count for current user: {}", e.getMessage());
      return 0;
    }
  }

  /**
   * Get organization names for a specific user.
   *
   * @param userId User UUID
   * @return List of organization names
   */
  public List<String> getUserOrganisationNames(UUID userId) {
    try {
      return getUserOrganisations(userId).stream()
          .map(OrganisationForPublicDTO::getName)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Error getting organization names for user {}: {}", userId, e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Get organization names for the current user.
   *
   * @return List of organization names for current user
   */
  public List<String> getCurrentUserOrganisationNames() {
    try {
      return getCurrentUserOrganisations().stream()
          .map(OrganisationForPublicDTO::getName)
          .filter(Objects::nonNull)
          .collect(Collectors.toList());
    } catch (Exception e) {
      logger.error("Error getting organization names for current user: {}", e.getMessage());
      return Collections.emptyList();
    }
  }

  /**
   * Get the first organization for a user (convenience method).
   *
   * @param userId User UUID
   * @return First organization or empty if none found
   */
  public Optional<OrganisationForPublicDTO> getUserFirstOrganisation(UUID userId) {
    try {
      List<OrganisationForPublicDTO> orgs = getUserOrganisations(userId);
      return orgs.isEmpty() ? Optional.empty() : Optional.of(orgs.get(0));
    } catch (Exception e) {
      logger.error("Error getting first organization for user {}: {}", userId, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Get the first organization for the current user (convenience method).
   *
   * @return First organization for current user or empty if none found
   */
  public Optional<OrganisationForPublicDTO> getCurrentUserFirstOrganisation() {
    try {
      List<OrganisationForPublicDTO> orgs = getCurrentUserOrganisations();
      return orgs.isEmpty() ? Optional.empty() : Optional.of(orgs.get(0));
    } catch (Exception e) {
      logger.error("Error getting first organization for current user: {}", e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Get current user UUID from JWT token.
   *
   * @return Current user UUID or empty if not available
   */
  public Optional<UUID> getCurrentUserId() {
    AuthService authService = getAuthService();
    if (authService instanceof AuthServiceImpl) {
      AuthServiceImpl authServiceImpl = (AuthServiceImpl) authService;
      return authServiceImpl.getCurrentUserId();
    }
    return Optional.empty();
  }

  /**
   * Get current user UUID from JWT token, throwing exception if not available.
   *
   * @return Current user UUID
   * @throws RuntimeException if user UUID not available
   */
  public UUID getCurrentUserIdRequired() {
    return getCurrentUserId()
        .orElseThrow(
            () ->
                new RuntimeException(
                    "Current user UUID not available. Ensure authentication and JWT token contains user information."));
  }

  /**
   * Check if current user UUID is available from JWT token.
   *
   * @return True if current user UUID is available
   */
  public boolean hasCurrentUserId() {
    return getCurrentUserId().isPresent();
  }

  // === Merkmalsgruppen - PROPERTY METHODS ===
  /**
   * Search for property groups with optional criteria. (POST /merkmale/api/v1/public/propertygroup)
   * Liefert alle Merkmalsgruppen, die zu den Suchparametern passen
   */
  public List<PropertyOrGroupForPublicDto> searchPropertyGroups(
      PropertyOrGroupForPublicRequest request) {
    try {
      return propertyGroupsApi.getPropertyGroupsForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching property groups: {}", e.getMessage());
      return List.of();
    }
  }

  /** Search for property groups with default criteria. */
  public List<PropertyOrGroupForPublicDto> searchPropertyGroups() {
    PropertyOrGroupForPublicRequest defaultRequest = new PropertyOrGroupForPublicRequest();
    // Set default pagination or search criteria if needed
    return searchPropertyGroups(defaultRequest);
  }

  /**
   * Get detailed property group information by its GUID. (GET
   * /merkmale/api/v1/public/propertygroup/{guid})
   */
  public Optional<PropertyGroupDto> getPropertyGroup(UUID propertyGroupGuid) {
    try {
      PropertyGroupDto propertyGroup =
          propertyGroupsApi.getPropertyGroupForPublic(propertyGroupGuid);
      return Optional.ofNullable(propertyGroup);
    } catch (Exception e) {
      logger.error("Error getting property group {}: {}", propertyGroupGuid, e.getMessage());
      return Optional.empty();
    }
  }

  // === Merkmale - PROPERTY METHODS ===

  /** Search for properties with optional criteria. */
  public List<PropertyOrGroupForPublicDto> searchProperties(
      PropertyOrGroupForPublicRequest request) {
    try {
      return propertiesApi.getPropertiesForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching properties: {}", e.getMessage());
      return List.of();
    }
  }

  /** Search for properties with default criteria. */
  public List<PropertyOrGroupForPublicDto> searchProperties() {
    PropertyOrGroupForPublicRequest defaultRequest = new PropertyOrGroupForPublicRequest();
    defaultRequest.setSearchString("a");
    return searchProperties(defaultRequest);
  }

  /** Get detailed property information. */
  public Optional<PropertyDto> getProperty(UUID propertyGuid) {
    try {
      PropertyDto property = propertiesApi.getPropertyForPublic(propertyGuid);
      return Optional.ofNullable(property);
    } catch (Exception e) {
      logger.error("Error getting property {}: {}", propertyGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /** Get basic statistics about available data. */
  public ApiStats getApiStats() {
    try {
      int projectCount = searchProjects().size();
      int propertyCount = searchProperties().size();
      int loinCount = searchLoins().size();
      int domainModelCount = searchDomainModels().size();

      return new ApiStats(projectCount, propertyCount, loinCount, domainModelCount);
    } catch (Exception e) {
      logger.error("Error getting API stats: {}", e.getMessage());
      return new ApiStats(0, 0, 0, 0);
    }
  }

  // === Property Filters (Merkmale - Filter) ======

  /**
   * Get property filters (Merkmale filters).
   *
   * @return List of property filter groups
   */
  public List<TagGroupForPublicDto> getPropertyFilters() {
    try {
      return propertiesApi.getGlobalFilters();
    } catch (Exception e) {
      logger.error("Error getting property filters: {}", e.getMessage());
      return List.of();
    }
  }

  // === LOIN METHODS WITH BINARY SUPPORT ===

  /** Search for LOINs with optional criteria. */
  public List<SimpleLoinPublicDto> searchLoins(LoinForPublicRequest request) {
    try {
      return loinApi.getLoinsForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching LOINs: {}", e.getMessage());
      return List.of();
    }
  }

  /** Search for all LOINs. */
  public List<SimpleLoinPublicDto> searchLoins() {
    return searchLoins(new LoinForPublicRequest());
  }

  /** Get detailed LOIN information. */
  public Optional<LOINPublicDto> getLoin(UUID loinGuid) {
    try {
      LOINPublicDto loin = loinApi.getLoinForPublic(loinGuid);
      return Optional.ofNullable(loin);
    } catch (Exception e) {
      logger.error("Error getting LOIN {}: {}", loinGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /** Export LOIN as PDF with proper binary handling. */
  public Optional<byte[]> exportLoinPdf(UUID loinGuid) {
    try {
      logger.debug("Attempting to export LOIN {} as PDF", loinGuid);
      byte[] pdfBytes = loinApi.exportPdf(loinGuid);

      if (pdfBytes != null && pdfBytes.length > 0 && isPdfOrZipContent(pdfBytes)) {
        logger.debug("Successfully exported LOIN {} as PDF ({} bytes)", loinGuid, pdfBytes.length);
        return Optional.of(pdfBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(loinGuid, "LOIN PDF", e);
    }
  }

  /** Export LOIN as OpenOffice ODT. 📝 */
  public Optional<byte[]> exportLoinOpenOffice(UUID loinGuid) {
    try {
      logger.debug("Attempting to export LOIN {} as OpenOffice ODT", loinGuid);
      byte[] odtBytes = loinApi.exportOdt(loinGuid);

      if (odtBytes != null && odtBytes.length > 0) {
        logger.debug("Successfully exported LOIN {} as ODT ({} bytes)", loinGuid, odtBytes.length);
        return Optional.of(odtBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(loinGuid, "LOIN OpenOffice ODT", e);
    }
  }

  /** Export LOIN as OKSTRA (ZIP). 🏗️ */
  public Optional<byte[]> exportLoinOkstra(UUID loinGuid) {
    try {
      logger.debug("Attempting to export LOIN {} as OKSTRA", loinGuid);
      byte[] zipBytes = loinApi.exportOkstra(loinGuid);

      if (zipBytes != null && zipBytes.length > 0) {
        logger.debug(
            "Successfully exported LOIN {} as OKSTRA ({} bytes)", loinGuid, zipBytes.length);
        return Optional.of(zipBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(loinGuid, "LOIN OKSTRA", e);
    }
  }

  /** Export LOIN as XML. 🔗 */
  public Optional<byte[]> exportLoinXml(UUID loinGuid) {
    try {
      logger.debug("Attempting to export LOIN {} as XML", loinGuid);
      byte[] xmlBytes = loinApi.exportXml(loinGuid);

      if (xmlBytes != null && xmlBytes.length > 0) {
        logger.debug("Successfully exported LOIN {} as XML ({} bytes)", loinGuid, xmlBytes.length);
        return Optional.of(xmlBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(loinGuid, "LOIN XML", e);
    }
  }

  /** Export LOIN as IDS. 🆔 */
  public Optional<byte[]> exportLoinIds(UUID loinGuid) {
    try {
      logger.debug("Attempting to export LOIN {} as IDS", loinGuid);
      byte[] idsBytes = loinApi.exportIds(loinGuid);

      if (idsBytes != null && idsBytes.length > 0) {
        logger.debug("Successfully exported LOIN {} as IDS ({} bytes)", loinGuid, idsBytes.length);
        return Optional.of(idsBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(loinGuid, "LOIN IDS", e);
    }
  }

  // === DOMAIN MODEL METHODS ===

  /** Search for domain-specific models. */
  public List<SimpleDomainSpecificModelPublicDto> searchDomainModels(
      AiaDomainSpecificModelForPublicRequest request) {
    try {
      return domainModelsApi.getDomainSpecificModelsForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching domain models: {}", e.getMessage());
      return List.of();
    }
  }

  /** Search for all domain models. */
  public List<SimpleDomainSpecificModelPublicDto> searchDomainModels() {
    return searchDomainModels(new AiaDomainSpecificModelForPublicRequest());
  }

  /** Get detailed domain model information. */
  public Optional<AIADomainSpecificModelPublicDto> getDomainModel(UUID modelGuid) {
    try {
      AIADomainSpecificModelPublicDto model =
          domainModelsApi.getDomainSpecificModelForPublic(modelGuid);
      return Optional.ofNullable(model);
    } catch (Exception e) {
      logger.error("Error getting domain model {}: {}", modelGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /** Export domain model as PDF with proper binary handling. */
  public Optional<byte[]> exportDomainModelPdf(UUID modelGuid) {
    try {
      logger.debug("Attempting to export domain model {} as PDF/ZIP", modelGuid);
      byte[] pdfBytes = domainModelsApi.exportPdf1(modelGuid);

      if (pdfBytes != null && pdfBytes.length > 0 && isPdfOrZipContent(pdfBytes)) {
        logger.debug(
            "Successfully exported domain model {} as PDF/ZIP ({} bytes)",
            modelGuid,
            pdfBytes.length);
        return Optional.of(pdfBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(modelGuid, "domain model PDF", e);
    }
  }

  /** Export domain model as OpenOffice ODT. 📝 */
  public Optional<byte[]> exportDomainModelOpenOffice(UUID modelGuid) {
    try {
      logger.debug("Attempting to export domain model {} as OpenOffice ODT", modelGuid);
      byte[] odtBytes = domainModelsApi.exportOdt1(modelGuid);

      if (odtBytes != null && odtBytes.length > 0) {
        logger.debug(
            "Successfully exported domain model {} as ODT ({} bytes)", modelGuid, odtBytes.length);
        return Optional.of(odtBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(modelGuid, "domain model OpenOffice ODT", e);
    }
  }

  /** Export domain model as OKSTRA (ZIP). 🏗️ */
  public Optional<byte[]> exportDomainModelOkstra(UUID modelGuid) {
    try {
      logger.debug("Attempting to export domain model {} as OKSTRA", modelGuid);
      byte[] zipBytes = domainModelsApi.exportOkstra1(modelGuid);

      if (zipBytes != null && zipBytes.length > 0) {
        logger.debug(
            "Successfully exported domain model {} as OKSTRA ({} bytes)",
            modelGuid,
            zipBytes.length);
        return Optional.of(zipBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(modelGuid, "domain model OKSTRA", e);
    }
  }

  /** Export domain model as LOIN XML. 📄 */
  public Optional<byte[]> exportDomainModelLoinXml(UUID modelGuid) {
    try {
      logger.debug("Attempting to export domain model {} as LOIN XML", modelGuid);
      byte[] xmlBytes = domainModelsApi.exportXml1(modelGuid);

      if (xmlBytes != null && xmlBytes.length > 0) {
        logger.debug(
            "Successfully exported domain model {} as LOIN XML ({} bytes)",
            modelGuid,
            xmlBytes.length);
        return Optional.of(xmlBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(modelGuid, "domain model LOIN XML", e);
    }
  }

  /** Export domain model as IDS. 🏢 */
  public Optional<byte[]> exportDomainModelIds(UUID modelGuid) {
    try {
      logger.debug("Attempting to export domain model {} as IDS", modelGuid);
      byte[] idsBytes = domainModelsApi.exportIds1(modelGuid);

      if (idsBytes != null && idsBytes.length > 0) {
        logger.debug(
            "Successfully exported domain model {} as IDS ({} bytes)", modelGuid, idsBytes.length);
        return Optional.of(idsBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(modelGuid, "domain model IDS", e);
    }
  }

  // === CONTEXT INFORMATION METHODS ===

  // === FIXED CONTEXT INFORMATION METHODS ===

  /**
   * Search for context information with optional criteria using the fixed approach.
   *
   * @param request Search criteria (null for all context information)
   * @return List of context information
   */
  public List<SimpleContextInfoPublicDto> searchContextInfo(AiaContextInfoPublicRequest request) {
    try {
      // Use the working custom Feign approach for context info
      return searchContextInfoFixed(request);
    } catch (Exception e) {
      logger.error("Error searching context information: {}", e.getMessage());
      logger.debug("Full exception details:", e);
      return List.of();
    }
  }

  /**
   * Search for all context information using the fixed approach.
   *
   * @return List of all context information
   */
  public List<SimpleContextInfoPublicDto> searchContextInfo() {
    return searchContextInfo(new AiaContextInfoPublicRequest());
  }

  /**
   * Fixed implementation using properly configured Feign client. This solves the Jackson
   * serialization issue that was causing 500 errors.
   */
  private List<SimpleContextInfoPublicDto> searchContextInfoFixed(
      AiaContextInfoPublicRequest request) {
    try {
      if (request == null) {
        request = new AiaContextInfoPublicRequest();
      }

      logger.debug("Using fixed context info search with proper Jackson configuration");

      // Create a custom Feign client with proper Jackson configuration
      com.fasterxml.jackson.databind.ObjectMapper customMapper =
          new com.fasterxml.jackson.databind.ObjectMapper();
      customMapper.registerModule(new com.fasterxml.jackson.datatype.jsr310.JavaTimeModule());
      customMapper.registerModule(new org.openapitools.jackson.nullable.JsonNullableModule());

      // The key fix - exclude null values to match what Postman sends
      customMapper.setSerializationInclusion(
          com.fasterxml.jackson.annotation.JsonInclude.Include.NON_NULL);

      feign.jackson.JacksonEncoder encoder = new feign.jackson.JacksonEncoder(customMapper);
      feign.jackson.JacksonDecoder decoder = new feign.jackson.JacksonDecoder(customMapper);

      // Create the fixed API client (headers are defined in the interface via @Headers)
      com.bimportal.client.api.KontextinformationenApi fixedApi =
          feign.Feign.builder()
              .encoder(encoder)
              .decoder(decoder)
              .requestInterceptor(
                  template -> {
                    try {
                      String token = authService.getValidToken();
                      template.header("Authorization", "Bearer " + token);
                    } catch (Exception e) {
                      logger.error("Failed to add auth header in fixed context search", e);
                    }
                  })
              .target(
                  com.bimportal.client.api.KontextinformationenApi.class, apiClient.getBasePath());

      List<SimpleContextInfoPublicDto> result = fixedApi.getContextInfosForPublic(request);
      logger.debug("Fixed context info search returned {} items", result.size());

      return result;

    } catch (Exception e) {
      logger.error("Fixed context info search failed: {}", e.getMessage());
      logger.debug("Full exception:", e);
      return List.of();
    }
  }

  /**
   * Get detailed context information using the standard API (this one works fine).
   *
   * @param contextGuid Context information GUID
   * @return Context information details or empty if not found
   */
  public Optional<AIAContextInfoPublicDto> getContextInfo(UUID contextGuid) {
    try {
      AIAContextInfoPublicDto contextInfo = contextInfoApi.getContextInfoForPublic(contextGuid);
      return Optional.ofNullable(contextInfo);
    } catch (Exception e) {
      logger.error("Error getting context information {}: {}", contextGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Export context information as PDF (this one works fine).
   *
   * @param contextGuid Context information GUID
   * @return PDF content as byte array or empty if export failed
   */
  public Optional<byte[]> exportContextInfoPdf(UUID contextGuid) {
    try {
      byte[] pdfBytes = contextInfoApi.exportPdf2(contextGuid);
      return Optional.ofNullable(pdfBytes);
    } catch (Exception e) {
      logger.error(
          "Error exporting context information {} to PDF: {}", contextGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Export context information as OpenOffice format (this one works fine).
   *
   * @param contextGuid Context information GUID
   * @return ODT content as byte array or empty if export failed
   */
  public Optional<byte[]> exportContextInfoOpenOffice(UUID contextGuid) {
    try {
      byte[] odtBytes = contextInfoApi.exportOdt2(contextGuid);
      return Optional.ofNullable(odtBytes);
    } catch (Exception e) {
      logger.error(
          "Error exporting context information {} to OpenOffice: {}", contextGuid, e.getMessage());
      return Optional.empty();
    }
  }

  // === TEMPLATE METHODS ===

  /**
   * Search for AIA templates with optional criteria.
   *
   * @param request Search criteria (null for all templates)
   * @return List of templates
   */
  public List<SimpleAiaTemplatePublicDto> searchTemplates(AiaTemplateForPublicRequest request) {
    try {
      return templatesApi.getTemplatesForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching templates: {}", e.getMessage());
      return List.of();
    }
  }

  /**
   * Search for all AIA templates.
   *
   * @return List of all templates
   */
  public List<SimpleAiaTemplatePublicDto> searchTemplates() {
    return searchTemplates(new AiaTemplateForPublicRequest());
  }

  /**
   * Get detailed template information.
   *
   * @param templateGuid Template GUID
   * @return Template details or empty if not found
   */
  public Optional<AIATemplatePublicDto> getTemplate(UUID templateGuid) {
    try {
      AIATemplatePublicDto template = templatesApi.getTemplateForPublic(templateGuid);
      return Optional.ofNullable(template);
    } catch (Exception e) {
      logger.error("Error getting template {}: {}", templateGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Export template as PDF.
   *
   * @param templateGuid Template GUID
   * @return PDF content as byte array or empty if export failed
   */
  public Optional<byte[]> exportTemplatePdf(UUID templateGuid) {
    try {
      byte[] pdfBytes = templatesApi.exportPdf3(templateGuid);
      return Optional.ofNullable(pdfBytes);
    } catch (Exception e) {
      logger.error("Error exporting template {} to PDF: {}", templateGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /**
   * Export template as OpenOffice format.
   *
   * @param templateGuid Template GUID
   * @return ODT content as byte array or empty if export failed
   */
  public Optional<byte[]> exportTemplateOpenOffice(UUID templateGuid) {
    try {
      byte[] odtBytes = templatesApi.exportOdt3(templateGuid);
      return Optional.ofNullable(odtBytes);
    } catch (Exception e) {
      logger.error("Error exporting template {} to OpenOffice: {}", templateGuid, e.getMessage());
      return Optional.empty();
    }
  }

  // === PROJECT METHODS WITH BINARY SUPPORT ===

  /** Search for projects with optional criteria. */
  public List<SimpleAiaProjectPublicDto> searchProjects(AiaProjectForPublicRequest request) {
    try {
      return projectsApi.getProjectsForPublic(request);
    } catch (Exception e) {
      logger.error("Error searching projects: {}", e.getMessage());
      return List.of();
    }
  }

  /** Search for all projects. */
  public List<SimpleAiaProjectPublicDto> searchProjects() {
    return searchProjects(new AiaProjectForPublicRequest());
  }

  /** Get detailed project information. */
  public Optional<AIAProjectPublicDto> getProject(UUID projectGuid) {
    try {
      AIAProjectPublicDto project = projectsApi.getProjectForPublic(projectGuid);
      return Optional.ofNullable(project);
    } catch (Exception e) {
      logger.error("Error getting project {}: {}", projectGuid, e.getMessage());
      return Optional.empty();
    }
  }

  /** Export project as PDF with proper binary handling. */
  public Optional<byte[]> exportProjectPdf(UUID projectGuid) {
    try {
      logger.debug("Attempting to export project {} as PDF", projectGuid);
      byte[] pdfBytes = projectsApi.exportPdf4(projectGuid);

      if (pdfBytes != null && pdfBytes.length > 0) {
        // Verify it's actually PDF content
        if (isPdfOrZipContent(pdfBytes)) {
          logger.debug(
              "Successfully exported project {} as PDF ({} bytes)", projectGuid, pdfBytes.length);
          return Optional.of(pdfBytes);
        } else {
          logger.warn(
              "Response for project {} PDF export doesn't appear to be valid PDF content",
              projectGuid);
          return Optional.empty();
        }
      }

      logger.warn("PDF export for project {} returned empty content", projectGuid);
      return Optional.empty();

    } catch (Exception e) {
      // Handle other types of exceptions
      String errorMsg = e.getMessage();
      if (errorMsg != null && errorMsg.contains("Unexpected character ('%'")) {
        logger.info(
            "PDF export for project {} returned binary content but client couldn't parse it properly",
            projectGuid);
        // This is actually a success case, but we can't access the content due to client
        // configuration
        return Optional.empty();
      } else if (errorMsg != null && errorMsg.contains("404")) {
        logger.debug("Project {} not found for PDF export", projectGuid);
        return Optional.empty();
      } else {
        logger.error("Error exporting project {} to PDF: {}", projectGuid, errorMsg);
        return Optional.empty();
      }
    }
  }

  /** Export project as OpenOffice format with proper binary handling. */
  public Optional<byte[]> exportProjectOpenOffice(UUID projectGuid) {
    try {
      logger.debug("Attempting to export project {} as OpenOffice format", projectGuid);
      byte[] odtBytes = projectsApi.exportOdt4(projectGuid);

      if (odtBytes != null && odtBytes.length > 0) {
        logger.debug(
            "Successfully exported project {} as OpenOffice format ({} bytes)",
            projectGuid,
            odtBytes.length);
        return Optional.of(odtBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(projectGuid, "OpenOffice", e);
    }
  }

  /** Export project as OKSTRA format. */
  public Optional<byte[]> exportProjectOkstra(UUID projectGuid) {
    try {
      logger.debug("Attempting to export project {} as OKSTRA format", projectGuid);
      byte[] okstraBytes = projectsApi.exportOkstra2(projectGuid);

      if (okstraBytes != null && okstraBytes.length > 0) {
        logger.debug(
            "Successfully exported project {} as OKSTRA format ({} bytes)",
            projectGuid,
            okstraBytes.length);
        return Optional.of(okstraBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(projectGuid, "OKSTRA", e);
    }
  }

  /** Export project as LOIN-XML format. */
  public Optional<byte[]> exportProjectLoinXml(UUID projectGuid) {
    try {
      logger.debug("Attempting to export project {} as LOIN-XML format", projectGuid);
      byte[] xmlBytes = projectsApi.exportXml2(projectGuid);

      if (xmlBytes != null && xmlBytes.length > 0) {
        logger.debug(
            "Successfully exported project {} as LOIN-XML format ({} bytes)",
            projectGuid,
            xmlBytes.length);
        return Optional.of(xmlBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(projectGuid, "LOIN-XML", e);
    }
  }

  /** Export project as IDS format. */
  public Optional<byte[]> exportProjectIds(UUID projectGuid) {
    try {
      logger.debug("Attempting to export project {} as IDS format", projectGuid);
      byte[] idsBytes = projectsApi.exportIds2(projectGuid);

      if (idsBytes != null && idsBytes.length > 0) {
        logger.debug(
            "Successfully exported project {} as IDS format ({} bytes)",
            projectGuid,
            idsBytes.length);
        return Optional.of(idsBytes);
      }
      return Optional.empty();

    } catch (Exception e) {
      return handleBinaryExportException(projectGuid, "IDS", e);
    }
  }

  // === FILTER METHODS ===

  /**
   * Get AIA filters.
   *
   * @return List of AIA filter groups
   */
  public List<FilterGroupForPublicDto> getAiaFilters() {
    try {
      return aiaFilterApi.getGlobalFilters1();
    } catch (Exception e) {
      logger.error("Error getting AIA filters: {}", e.getMessage());
      return List.of();
    }
  }

  // === GETTER METHODS FOR DIRECT API ACCESS ===

  public AiaProjekteApi getProjectsApi() {
    return projectsApi;
  }

  public AiaLoinApi getLoinApi() {
    return loinApi;
  }

  public FachmodelleApi getDomainModelsApi() {
    return domainModelsApi;
  }

  public KontextinformationenApi getContextInfoApi() {
    return contextInfoApi;
  }

  public AiaVorlagenApi getTemplatesApi() {
    return templatesApi;
  }

  public MerkmaleApi getPropertiesApi() {
    return propertiesApi;
  }

  public MerkmalsgruppenApi getPropertyGroupsApi() {
    return propertyGroupsApi;
  }

  public AiaFilterApi getAiaFilterApi() {
    return aiaFilterApi;
  }

  public InfrastrukturApi getInfraApi() {
    return infraApi;
  }

  /** Get the underlying ApiClient for advanced usage. */
  public ApiClient getApiClient() {
    return apiClient;
  }

  /** Gets the underlying InfrastrukturApi for direct access */
  public InfrastrukturApi getInfrastructureApi() {
    return infraApi;
  }

  // === UTILITY METHODS ===

  /** Common handler for binary export exceptions. */
  private Optional<byte[]> handleBinaryExportException(UUID guid, String format, Exception e) {
    String errorMsg = e.getMessage();
    if (errorMsg != null && errorMsg.contains("Unexpected character")) {
      logger.info(
          "{} export for {} returned binary content but client couldn't parse it properly",
          format,
          guid);
      return Optional.empty();
    } else if (errorMsg != null && errorMsg.contains("404")) {
      logger.debug("{} {} not found for export", format, guid);
      return Optional.empty();
    } else {
      logger.error("Error exporting {} {} to {}: {}", format, guid, format, errorMsg);
      return Optional.empty();
    }
  }

  /** Check if byte array contains PDF content. */
  private boolean isPdfOrZipContent(byte[] data) {
    if (data == null || data.length < 4) {
      return false;
    }

    // Accept both PDF and ZIP files
    // PDF files start with %PDF (0x25 0x50 0x44 0x46)
    boolean isPdf = data[0] == 0x25 && data[1] == 0x50 && data[2] == 0x44 && data[3] == 0x46;

    // ZIP files start with PK (0x50 0x4B)
    boolean isZip = data.length >= 2 && data[0] == 0x50 && data[1] == 0x4B;

    return isPdf || isZip;
  }

  /** Helper method to save byte arrays to files. */
  public File saveToFile(byte[] data, String filename) {
    try {
      File file = new File("exports", filename);
      file.getParentFile().mkdirs();

      try (FileOutputStream fos = new FileOutputStream(file)) {
        fos.write(data);
      }

      logger.info("Saved {} bytes to file: {}", data.length, file.getAbsolutePath());
      return file;
    } catch (IOException e) {
      logger.error("Failed to save file {}: {}", filename, e.getMessage());
      throw new RuntimeException("Failed to save file: " + filename, e);
    }
  }

  /** Find the first exportable project from available projects. */
  public Optional<SimpleAiaProjectPublicDto> findExportableProject() {
    try {
      List<SimpleAiaProjectPublicDto> projects = searchProjects();
      if (projects.isEmpty()) {
        return Optional.empty();
      }

      logger.info("Checking {} projects for export capability...", projects.size());

      int maxToTest = Math.min(projects.size(), BimPortalConfig.MAX_PROJECTS_TO_TEST);
      for (int i = 0; i < maxToTest; i++) {
        SimpleAiaProjectPublicDto project = projects.get(i);
        logger.debug("   Testing project {}: {}...", i + 1, project.getName());

        // First check if we can get project details
        Optional<AIAProjectPublicDto> detailedProject = getProject(project.getGuid());
        if (detailedProject.isEmpty()) {
          logger.debug("      Skip: Cannot access project details");
          continue;
        }

        // Test if PDF export works
        Optional<byte[]> pdfContent = exportProjectPdf(project.getGuid());
        if (pdfContent.isPresent()) {
          logger.info("      Found exportable project: {}", project.getName());
          return Optional.of(project);
        } else {
          logger.debug("      Skip: Export not available");
        }
      }

      logger.warn("   No exportable projects found in {} tested projects", maxToTest);
      return Optional.empty();

    } catch (Exception e) {
      logger.error("Error finding exportable project: {}", e.getMessage());
      return Optional.empty();
    }
  }

  // === INNER CLASSES ===

  /** Health check result container */
  public static class HealthCheckResult {
    private final boolean apiAccessible;
    private final boolean authWorking;
    private final String statusMessage;
    private final String errorDetails;
    private final long responseTime;

    public HealthCheckResult(
        boolean apiAccessible,
        boolean authWorking,
        String statusMessage,
        String errorDetails,
        long responseTime) {
      this.apiAccessible = apiAccessible;
      this.authWorking = authWorking;
      this.statusMessage = statusMessage;
      this.errorDetails = errorDetails;
      this.responseTime = responseTime;
    }

    public boolean isApiAccessible() {
      return apiAccessible;
    }

    public boolean isAuthWorking() {
      return authWorking;
    }

    public String getStatusMessage() {
      return statusMessage;
    }

    public String getErrorDetails() {
      return errorDetails;
    }

    public long getResponseTime() {
      return responseTime;
    }

    @Override
    public String toString() {
      return String.format(
          "HealthCheck{api=%s, auth=%s, message='%s'}", apiAccessible, authWorking, statusMessage);
    }
  }

  /** API statistics container. */
  public static class ApiStats {
    private final int projectCount;
    private final int propertyCount;
    private final int loinCount;
    private final int domainModelCount;

    public ApiStats(int projectCount, int propertyCount, int loinCount, int domainModelCount) {
      this.projectCount = projectCount;
      this.propertyCount = propertyCount;
      this.loinCount = loinCount;
      this.domainModelCount = domainModelCount;
    }

    public int getProjectCount() {
      return projectCount;
    }

    public int getPropertyCount() {
      return propertyCount;
    }

    public int getLoinCount() {
      return loinCount;
    }

    public int getDomainModelCount() {
      return domainModelCount;
    }

    @Override
    public String toString() {
      return String.format(
          "ApiStats{projects=%d, properties=%d, loins=%d, domainModels=%d}",
          projectCount, propertyCount, loinCount, domainModelCount);
    }
  }
}
