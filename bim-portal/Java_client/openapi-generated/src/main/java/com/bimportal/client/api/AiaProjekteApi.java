package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AIAProjectPublicDto;
import com.bimportal.client.model.AiaProjectForPublicRequest;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.SimpleAiaProjectPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface AiaProjekteApi extends ApiClient.Api {

  /**
   * Liefert das AIA-P als XML-Datei im IDS-Format zurück. Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  byte[] exportIds2(@Param("guid") UUID guid);

  /**
   * Liefert das AIA-P als XML-Datei im IDS-Format zurück. Similar to <code>exportIds2</code> but it
   * also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportIds2WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der übergebenen GUID im OpenOffice-Format zurück Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOdt4(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der übergebenen GUID im OpenOffice-Format zurück Similar to <code>
   * exportOdt4</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOdt4WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Projekt assoziiert sind, zurück.
   * Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über
   * den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOkstra2(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Projekt assoziiert sind, zurück.
   * Similar to <code>exportOkstra2</code> but it also returns the http response headers . Diese API
   * erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOkstra2WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der übergebenen GUID im PDF-Format zurück Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  byte[] exportPdf4(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der übergebenen GUID im PDF-Format zurück Similar to <code>exportPdf4
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportPdf4WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Projekt assoziiert sind,
   * zurück. Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer
   * {accessToken}&#39; über den Authorization-Header, um nicht öffentlich sichtbare
   * Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  byte[] exportXml2(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Projekt assoziiert sind,
   * zurück. Similar to <code>exportXml2</code> but it also returns the http response headers .
   * Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über
   * den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportXml2WithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der gesuchten GUID Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return AIAProjectPublicDto
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}")
  @Headers({
    "Accept: application/json",
  })
  AIAProjectPublicDto getProjectForPublic(@Param("guid") UUID guid);

  /**
   * Liefert das Projekt mit der gesuchten GUID Similar to <code>getProjectForPublic</code> but it
   * also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/aiaProject/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<AIAProjectPublicDto> getProjectForPublicWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert alle Projekte, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaProjectForPublicRequest (required)
   * @return List&lt;SimpleAiaProjectPublicDto&gt;
   */
  @RequestLine("POST /aia/api/v1/public/aiaProject")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<SimpleAiaProjectPublicDto> getProjectsForPublic(
      AiaProjectForPublicRequest aiaProjectForPublicRequest);

  /**
   * Liefert alle Projekte, die zu den Suchparametern passen Similar to <code>getProjectsForPublic
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param aiaProjectForPublicRequest (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /aia/api/v1/public/aiaProject")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<SimpleAiaProjectPublicDto>> getProjectsForPublicWithHttpInfo(
      AiaProjectForPublicRequest aiaProjectForPublicRequest);
}
