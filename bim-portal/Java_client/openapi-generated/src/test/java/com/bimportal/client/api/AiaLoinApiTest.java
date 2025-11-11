package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.LoinForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for AiaLoinApi */
class AiaLoinApiTest {

  private AiaLoinApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(AiaLoinApi.class);
  }

  /**
   * Liefert die LOIN als XML-Datei im IDS-XML-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportIdsTest() {
    UUID guid = null;
    // byte[] response = api.exportIds(guid);

    // TODO: test validations
  }

  /**
   * Liefert die LOIN mit der übergebenen GUID im OpenOffice-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOdtTest() {
    UUID guid = null;
    // byte[] response = api.exportOdt(guid);

    // TODO: test validations
  }

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit der LOIN assoziiert sind, zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOkstraTest() {
    UUID guid = null;
    // byte[] response = api.exportOkstra(guid);

    // TODO: test validations
  }

  /**
   * Liefert die LOIN mit der übergebenen GUID im PDF-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportPdfTest() {
    UUID guid = null;
    // byte[] response = api.exportPdf(guid);

    // TODO: test validations
  }

  /**
   * Liefert die LOIN als XML-Datei im LOIN-XML-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportXmlTest() {
    UUID guid = null;
    // byte[] response = api.exportXml(guid);

    // TODO: test validations
  }

  /**
   * Liefert die LOIN mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getLoinForPublicTest() {
    UUID guid = null;
    // LOINPublicDto response = api.getLoinForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle LOINs, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getLoinsForPublicTest() {
    LoinForPublicRequest loinForPublicRequest = null;
    // List<SimpleLoinPublicDto> response = api.getLoinsForPublic(loinForPublicRequest);

    // TODO: test validations
  }
}
