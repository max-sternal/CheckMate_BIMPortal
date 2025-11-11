package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.PropertyGroupDto;
import com.bimportal.client.model.PropertyOrGroupForPublicDto;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface MerkmalsgruppenApi extends ApiClient.Api {

  /**
   * Liefert die Merkmalsgruppe mit der gesuchten GUID Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   *
   * @param guid (required)
   * @return PropertyGroupDto
   */
  @RequestLine("GET /merkmale/api/v1/public/propertygroup/{guid}")
  @Headers({
    "Accept: application/json",
  })
  PropertyGroupDto getPropertyGroupForPublic(@Param("guid") UUID guid);

  /**
   * Liefert die Merkmalsgruppe mit der gesuchten GUID Similar to <code>getPropertyGroupForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /merkmale/api/v1/public/propertygroup/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<PropertyGroupDto> getPropertyGroupForPublicWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert alle Merkmalsgruppen, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   *
   * @param propertyOrGroupForPublicRequest (optional)
   * @return List&lt;PropertyOrGroupForPublicDto&gt;
   */
  @RequestLine("POST /merkmale/api/v1/public/propertygroup")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<PropertyOrGroupForPublicDto> getPropertyGroupsForPublic(
      PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest);

  /**
   * Liefert alle Merkmalsgruppen, die zu den Suchparametern passen Similar to <code>
   * getPropertyGroupsForPublic</code> but it also returns the http response headers . Diese API
   * erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   *
   * @param propertyOrGroupForPublicRequest (optional)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /merkmale/api/v1/public/propertygroup")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<PropertyOrGroupForPublicDto>> getPropertyGroupsForPublicWithHttpInfo(
      PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest);
}
