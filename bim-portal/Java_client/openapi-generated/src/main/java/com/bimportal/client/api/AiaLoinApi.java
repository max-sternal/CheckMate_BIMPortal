package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.LOINPublicDto;
import com.bimportal.client.model.LoinForPublicRequest;
import com.bimportal.client.model.SimpleLoinPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;
import java.util.UUID;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface AiaLoinApi extends ApiClient.Api {

  /**
   * Liefert die LOIN als XML-Datei im IDS-XML-Format zurück Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  byte[] exportIds(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN als XML-Datei im IDS-XML-Format zurück Similar to <code>exportIds</code> but
   * it also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/IDS")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportIdsWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der übergebenen GUID im OpenOffice-Format zurück Diese API erlaubt
   * optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOdt(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der übergebenen GUID im OpenOffice-Format zurück Similar to <code>
   * exportOdt</code> but it also returns the http response headers . Diese API erlaubt optional
   * eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/openOffice")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOdtWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit der LOIN assoziiert sind, zurück.
   * Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über
   * den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  byte[] exportOkstra(@Param("guid") UUID guid);

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit der LOIN assoziiert sind, zurück.
   * Similar to <code>exportOkstra</code> but it also returns the http response headers . Diese API
   * erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den
   * Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/okstra")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportOkstraWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der übergebenen GUID im PDF-Format zurück Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  byte[] exportPdf(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der übergebenen GUID im PDF-Format zurück Similar to <code>exportPdf
   * </code> but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/pdf")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportPdfWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN als XML-Datei im LOIN-XML-Format zurück Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return byte[]
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  byte[] exportXml(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN als XML-Datei im LOIN-XML-Format zurück Similar to <code>exportXml</code> but
   * it also returns the http response headers . Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}/loinXML")
  @Headers({
    "Accept: */*",
  })
  ApiResponse<byte[]> exportXmlWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der gesuchten GUID Diese API erlaubt optional eine Authentifizierung
   * mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich
   * sichtbare Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return LOINPublicDto
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}")
  @Headers({
    "Accept: application/json",
  })
  LOINPublicDto getLoinForPublic(@Param("guid") UUID guid);

  /**
   * Liefert die LOIN mit der gesuchten GUID Similar to <code>getLoinForPublic</code> but it also
   * returns the http response headers . Diese API erlaubt optional eine Authentifizierung mittels
   * &#39;Bearer {accessToken}&#39; über den Authorization-Header, um nicht öffentlich sichtbare
   * Informationselemente abrufen zu können.
   *
   * @param guid (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/loin/{guid}")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<LOINPublicDto> getLoinForPublicWithHttpInfo(@Param("guid") UUID guid);

  /**
   * Liefert alle LOINs, die zu den Suchparametern passen Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param loinForPublicRequest (required)
   * @return List&lt;SimpleLoinPublicDto&gt;
   */
  @RequestLine("POST /aia/api/v1/public/loin")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  List<SimpleLoinPublicDto> getLoinsForPublic(LoinForPublicRequest loinForPublicRequest);

  /**
   * Liefert alle LOINs, die zu den Suchparametern passen Similar to <code>getLoinsForPublic</code>
   * but it also returns the http response headers . Diese API erlaubt optional eine
   * Authentifizierung mittels &#39;Bearer {accessToken}&#39; über den Authorization-Header, um
   * nicht öffentlich sichtbare Informationselemente abrufen zu können.
   *
   * @param loinForPublicRequest (required)
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("POST /aia/api/v1/public/loin")
  @Headers({
    "Content-Type: application/json",
    "Accept: application/json",
  })
  ApiResponse<List<SimpleLoinPublicDto>> getLoinsForPublicWithHttpInfo(
      LoinForPublicRequest loinForPublicRequest);
}
