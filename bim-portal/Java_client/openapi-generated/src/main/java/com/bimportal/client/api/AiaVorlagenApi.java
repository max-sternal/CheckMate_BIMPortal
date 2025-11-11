package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AIATemplatePublicDto;
import com.bimportal.client.model.AiaTemplateForPublicRequest;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.SimpleAiaTemplatePublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface AiaVorlagenApi extends ApiClient.Api {

  /**
   * Liefert die Vorlage mit der übergebenen GUID im OpenOffice-Format zurück Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOdt3(@Param("guid") UUID guid);

  /**
   * Liefert die Vorlage mit der übergebenen GUID im OpenOffice-Format zurück Similar to <code>
   * exportOdt3</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOdt3WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die Vorlage mit der übergebenen GUID im PDF-Format zurück Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  byte[] exportPdf3(@Param("guid") UUID guid);

  /**
   * Liefert die Vorlage mit der übergebenen GUID im PDF-Format zurück Similar to <code>exportPdf3
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportPdf3WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die Vorlage mit der gesuchten GUID Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return AIATemplatePublicDto
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}")
  @Headers({
    "Accept: application/json",
  })
  AIATemplatePublicDto getTemplateForPublic(@Param("guid") UUID guid);

  /**
   * Liefert die Vorlage mit der gesuchten GUID Similar to <code>getTemplateForPublic</code> but it
   * also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaTemplate/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<AIATemplatePublicDto> getTemplateForPublicWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert alle Vorlagen, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaTemplateForPublicRequest (required)
   * @return List&lt;SimpleAiaTemplatePublicDto&gt;
   */
  @RequestLine("POST /aia/api/v1/public/aiaTemplate")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<SimpleAiaTemplatePublicDto> getTemplatesForPublic(
      AiaTemplateForPublicRequest aiaTemplateForPublicRequest);

  /**
   * Liefert alle Vorlagen, die zu den Suchparametern passen Similar to <code>getTemplatesForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaTemplateForPublicRequest (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /aia/api/v1/public/aiaTemplate")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<SimpleAiaTemplatePublicDto>> getTemplatesForPublicWithHttpInfo(
      AiaTemplateForPublicRequest aiaTemplateForPublicRequest);
}
