package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.PropertyDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import com.bimportal.client.model.TagGroupForPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface MerkmaleApi extends ApiClient.Api {

  /**
   * Liefert alle globalen Filter zurück
   *
   * @return List&lt;TagGroupForPublicDto&gt;
   */
  @RequestLine("GET /merkmale/api/v1/public/filter")
  @Headers({
    "Accept: application/json",
  })
  List<TagGroupForPublicDto> getGlobalFilters();

  /**
   * Liefert alle globalen Filter zurück Similar to <code>getGlobalFilters</code> but it also
   * returns the http response headers .
   *
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /merkmale/api/v1/public/filter")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<List<TagGroupForPublicDto>> getGlobalFiltersWithHttpInfo();

  /**
   * Liefert alle Merkmale, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Merkmale abrufen zu können.
   *
   * @param propertyOrGroupForPublicRequest (optional)
   * @return List&lt;PropertyOrGroupForPublicDto&gt;
   */
  @RequestLine("POST /merkmale/api/v1/public/property")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<PropertyOrGroupForPublicDto> getPropertiesForPublic(
      PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest);

  /**
   * Liefert alle Merkmale, die zu den Suchparametern passen Similar to <code>getPropertiesForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Merkmale abrufen zu können.
   *
   * @param propertyOrGroupForPublicRequest (optional)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /merkmale/api/v1/public/property")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<PropertyOrGroupForPublicDto>> getPropertiesForPublicWithHttpInfo(
      PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest);

  /**
   * Liefert das Merkmal mit der gesuchten GUID Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Merkmale abrufen zu können.
   *
   * @param guid (required)
   * @return PropertyDto
   */
  @RequestLine("GET /merkmale/api/v1/public/property/{guid}")
  @Headers({
    "Accept: application/json",
  })
  PropertyDto getPropertyForPublic(@Param("guid") UUID guid);

  /**
   * Liefert das Merkmal mit der gesuchten GUID Similar to <code>getPropertyForPublic</code> but it
   * also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Merkmale abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /merkmale/api/v1/public/property/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<PropertyDto> getPropertyForPublicWithHttpInfo(@Param("guid") UUID guid);
}
