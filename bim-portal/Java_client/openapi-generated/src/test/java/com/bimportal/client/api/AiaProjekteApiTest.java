package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AiaProjectForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for AiaProjekteApi */
class AiaProjekteApiTest {

  private AiaProjekteApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(AiaProjekteApi.class);
  }

  /**
   * Liefert das AIA-P als XML-Datei im IDS-Format zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportIds2Test() {
    UUID guid = null;
    // byte[] response = api.exportIds2(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Projekt mit der übergebenen GUID im OpenOffice-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOdt4Test() {
    UUID guid = null;
    // byte[] response = api.exportOdt4(guid);

    // TODO: test validations
  }

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Projekt assoziiert sind, zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOkstra2Test() {
    UUID guid = null;
    // byte[] response = api.exportOkstra2(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Projekt mit der übergebenen GUID im PDF-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportPdf4Test() {
    UUID guid = null;
    // byte[] response = api.exportPdf4(guid);

    // TODO: test validations
  }

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Projekt assoziiert sind,
   * zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportXml2Test() {
    UUID guid = null;
    // byte[] response = api.exportXml2(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Projekt mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getProjectForPublicTest() {
    UUID guid = null;
    // AIAProjectPublicDto response = api.getProjectForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle Projekte, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getProjectsForPublicTest() {
    AiaProjectForPublicRequest aiaProjectForPublicRequest = null;
    // List<SimpleAiaProjectPublicDto> response =
    // api.getProjectsForPublic(aiaProjectForPublicRequest);

    // TODO: test validations
  }
}
