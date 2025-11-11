package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AIADomainSpecificModelPublicDto;
import com.bimportal.client.model.AiaDomainSpecificModelForPublicRequest;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.SimpleDomainSpecificModelPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface FachmodelleApi extends ApiClient.Api {

  /**
   * Liefert das Fachmodell als XML-Datei im IDS-Format zurück. Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  byte[] exportIds1(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell als XML-Datei im IDS-Format zurück. Similar to <code>exportIds1</code>
   * but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportIds1WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im OpenOffice-Format zurück Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOdt1(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im OpenOffice-Format zurück Similar to <code>
   * exportOdt1</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOdt1WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück. Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer
   * {accessToken}&#39; über den Authorization-Header, um nicht öffentlich sichtbare
   * Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOkstra1(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück. Similar to <code>exportOkstra1</code> but it also returns the http response headers .
   * Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über
   * den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOkstra1WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im PDF-Format zurück Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  byte[] exportPdf1(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im PDF-Format zurück Similar to <code>
   * exportPdf1</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportPdf1WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück. Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer
   * {accessToken}&#39; über den Authorization-Header, um nicht öffentlich sichtbare
   * Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  byte[] exportXml1(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück. Similar to <code>exportXml1</code> but it also returns the http response headers .
   * Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über
   * den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportXml1WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der gesuchten GUID Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return AIADomainSpecificModelPublicDto
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}")
  @Headers({
    "Accept: application/json",
  })
  AIADomainSpecificModelPublicDto getDomainSpecificModelForPublic(@Param("guid") UUID guid);

  /**
   * Liefert das Fachmodell mit der gesuchten GUID Similar to <code>getDomainSpecificModelForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/domainSpecificModel/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<AIADomainSpecificModelPublicDto> getDomainSpecificModelForPublicWithHttpInfo(
      @Param("guid") UUID guid);

  /**
   * Liefert alle Fachmodelle, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaDomainSpecificModelForPublicRequest (required)
   * @return List&lt;SimpleDomainSpecificModelPublicDto&gt;
   */
  @RequestLine("POST /aia/api/v1/public/domainSpecificModel")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<SimpleDomainSpecificModelPublicDto> getDomainSpecificModelsForPublic(
      AiaDomainSpecificModelForPublicRequest aiaDomainSpecificModelForPublicRequest);

  /**
   * Liefert alle Fachmodelle, die zu den Suchparametern passen Similar to <code>
   * getDomainSpecificModelsForPublic</code> but it also returns the http response headers . Diese
   * API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaDomainSpecificModelForPublicRequest (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /aia/api/v1/public/domainSpecificModel")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<SimpleDomainSpecificModelPublicDto>>
      getDomainSpecificModelsForPublicWithHttpInfo(
          AiaDomainSpecificModelForPublicRequest aiaDomainSpecificModelForPublicRequest);
}
