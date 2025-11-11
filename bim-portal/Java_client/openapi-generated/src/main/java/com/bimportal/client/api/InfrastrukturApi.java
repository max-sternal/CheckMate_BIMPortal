package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.EncodingUtils;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.JWTTokenPublicDto;
import com.bimportal.client.model.OrganisationForPublicDTO;
import com.bimportal.client.model.RefreshTokenRequestDTO;
import com.bimportal.client.model.UserLoginPublicDto;
import feign.*;
import feign.Headers;
import feign.RequestLine;
import jakarta.validation.constraints.*;
import java.util.HashMap;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface InfrastrukturApi extends ApiClient.Api {

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen
   *
   * @return List&lt;OrganisationForPublicDTO&gt;
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation")
  @Headers({
    "Accept: application/json",
  })
  List<OrganisationForPublicDTO> getOrganisationsForPublic();

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen Similar to <code>
   * getOrganisationsForPublic</code> but it also returns the http response headers .
   *
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<List<OrganisationForPublicDTO>> getOrganisationsForPublicWithHttpInfo();

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen, in denen der Nutzer Mitglied ist. Diese
   * API erfordert eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header.
   *
   * @param userId (required)
   * @return List&lt;OrganisationForPublicDTO&gt;
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation/my?userId={userId}")
  @Headers({
    "Accept: application/json",
  })
  List<OrganisationForPublicDTO> getOrganisationsOfUser(@Param("userId") UUID userId);

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen, in denen der Nutzer Mitglied ist.
   * Similar to <code>getOrganisationsOfUser</code> but it also returns the http response headers .
   * Diese API erfordert eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header.
   *
   * @param userId (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation/my?userId={userId}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<List<OrganisationForPublicDTO>> getOrganisationsOfUserWithHttpInfo(
      @Param("userId") UUID userId);

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen, in denen der Nutzer Mitglied ist. Diese
   * API erfordert eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header. Note, this is equivalent to the other <code>getOrganisationsOfUser</code>
   * method, but with the query parameters collected into a single Map parameter. This is convenient
   * for services with optional query parameters, especially when used with the {@link
   * GetOrganisationsOfUserQueryParams} class that allows for building up this map in a fluent
   * style.
   *
   * @param queryParams Map of query parameters as name-value pairs
   *     <p>The following elements may be specified in the query map:
   *     <ul>
   *       <li>userId - (required)
   *     </ul>
   *
   * @return List&lt;OrganisationForPublicDTO&gt;
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation/my?userId={userId}")
  @Headers({
    "Accept: application/json",
  })
  @SuppressWarnings("deprecation")
  List<OrganisationForPublicDTO> getOrganisationsOfUser(
      @QueryMap GetOrganisationsOfUserQueryParams queryParams);

  /**
   * Liste aller über die Rest-API nutzbaren Organisationen, in denen der Nutzer Mitglied ist. Diese
   * API erfordert eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header. Note, this is equivalent to the other <code>getOrganisationsOfUser</code>
   * that receives the query parameters as a map, but this one also exposes the Http response
   * headers
   *
   * @param queryParams Map of query parameters as name-value pairs
   *     <p>The following elements may be specified in the query map:
   *     <ul>
   *       <li>userId - (required)
   *     </ul>
   *
   * @return List&lt;OrganisationForPublicDTO&gt;
   */
  @RequestLine("GET /infrastruktur/api/v1/public/organisation/my?userId={userId}")
  @Headers({
    "Accept: application/json",
  })
  @SuppressWarnings("deprecation")
  ApiResponse<List<OrganisationForPublicDTO>> getOrganisationsOfUserWithHttpInfo(
      @QueryMap GetOrganisationsOfUserQueryParams queryParams);

  /**
   * A convenience class for generating query parameters for the <code>getOrganisationsOfUser</code>
   * method in a fluent style.
   */
  public static class GetOrganisationsOfUserQueryParams extends HashMap<String, Object> {
    public GetOrganisationsOfUserQueryParams userId(final UUID value) {
      put("userId", EncodingUtils.encode(value));
      return this;
    }
  }

  /**
   * Anmeldung am System
   *
   * @param userLoginPublicDto (required)
   * @return JWTTokenPublicDto
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/login")
  @Headers({
    "Content-Type: application/json",
    "Accept: */*",
  })
  JWTTokenPublicDto login(UserLoginPublicDto userLoginPublicDto);

  /**
   * Anmeldung am System Similar to <code>login</code> but it also returns the http response headers
   * .
   *
   * @param userLoginPublicDto (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/login")
  @Headers({
    "Content-Type: application/json",
    "Accept: */*",
  })
  ApiResponse<JWTTokenPublicDto> loginWithHttpInfo(UserLoginPublicDto userLoginPublicDto);

  /**
   * Benutzer abmelden Diese API erfordert eine Authentifizierung mittels &#39;Bearer
   * {accessToken}&#39; über den Authorization-Header.
   *
   * @return String
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/logout")
  @Headers({
    "Accept: */*",
  })
  String logout();

  /**
   * Benutzer abmelden Similar to <code>logout</code> but it also returns the http response headers
   * . Diese API erfordert eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header.
   *
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/logout")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<String> logoutWithHttpInfo();

  /**
   * Aktualisierung des Autorisierungs-Token
   *
   * @param refreshTokenRequestDTO (required)
   * @return JWTTokenPublicDto
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/refresh")
  @Headers({
    "Content-Type: application/json",
    "Accept: */*",
  })
  JWTTokenPublicDto refreshToken(RefreshTokenRequestDTO refreshTokenRequestDTO);

  /**
   * Aktualisierung des Autorisierungs-Token Similar to <code>refreshToken</code> but it also
   * returns the http response headers .
   *
   * @param refreshTokenRequestDTO (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /infrastruktur/api/v1/public/auth/refresh")
  @Headers({
    "Content-Type: application/json",
    "Accept: */*",
  })
  ApiResponse<JWTTokenPublicDto> refreshTokenWithHttpInfo(
      RefreshTokenRequestDTO refreshTokenRequestDTO);
}
