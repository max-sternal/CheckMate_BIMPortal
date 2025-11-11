package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AIAContextInfoPublicDto;
import com.bimportal.client.model.AiaContextInfoPublicRequest;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.SimpleContextInfoPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface KontextinformationenApi extends ApiClient.Api {

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im OpenOffice-Format zurück Diese API
   * erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOdt2(@Param("guid") UUID guid);

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im OpenOffice-Format zurück Similar to
   * <code>exportOdt2</code> but it also returns the http response headers . Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOdt2WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im PDF-Format zurück Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  byte[] exportPdf2(@Param("guid") UUID guid);

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im PDF-Format zurück Similar to <code>
   * exportPdf2</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportPdf2WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die Kontextinformation mit der gesuchten GUID Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return AIAContextInfoPublicDto
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}")
  @Headers({
    "Accept: application/json",
  })
  AIAContextInfoPublicDto getContextInfoForPublic(@Param("guid") UUID guid);

  /**
   * Liefert die Kontextinformation mit der gesuchten GUID Similar to <code>getContextInfoForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/contextInfo/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<AIAContextInfoPublicDto> getContextInfoForPublicWithHttpInfo(
      @Param("guid") UUID guid);

  /**
   * Liefert alle Kontextinformationen, die zu den Suchparametern passen Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaContextInfoPublicRequest (required)
   * @return List&lt;SimpleContextInfoPublicDto&gt;
   */
  @RequestLine("POST /aia/api/v1/public/contextInfo")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<SimpleContextInfoPublicDto> getContextInfosForPublic(
      AiaContextInfoPublicRequest aiaContextInfoPublicRequest);

  /**
   * Liefert alle Kontextinformationen, die zu den Suchparametern passen Similar to <code>
   * getContextInfosForPublic</code> but it also returns the http response headers . Diese API
   * erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaContextInfoPublicRequest (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /aia/api/v1/public/contextInfo")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<SimpleContextInfoPublicDto>> getContextInfosForPublicWithHttpInfo(
      AiaContextInfoPublicRequest aiaContextInfoPublicRequest);
}
