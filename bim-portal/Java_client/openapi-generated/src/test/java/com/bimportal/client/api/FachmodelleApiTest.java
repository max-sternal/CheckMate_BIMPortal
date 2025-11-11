package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AiaDomainSpecificModelForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for FachmodelleApi */
class FachmodelleApiTest {

  private FachmodelleApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(FachmodelleApi.class);
  }

  /**
   * Liefert das Fachmodell als XML-Datei im IDS-Format zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportIds1Test() {
    UUID guid = null;
    // byte[] response = api.exportIds1(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im OpenOffice-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOdt1Test() {
    UUID guid = null;
    // byte[] response = api.exportOdt1(guid);

    // TODO: test validations
  }

  /**
   * Liefert eine .zip-Datei mit allen Okstra-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOkstra1Test() {
    UUID guid = null;
    // byte[] response = api.exportOkstra1(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Fachmodell mit der übergebenen GUID im PDF-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportPdf1Test() {
    UUID guid = null;
    // byte[] response = api.exportPdf1(guid);

    // TODO: test validations
  }

  /**
   * Liefert eine .zip-Datei mit allen LOIN-XML-Dateien, die mit dem Fachmodell assoziiert sind,
   * zurück.
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportXml1Test() {
    UUID guid = null;
    // byte[] response = api.exportXml1(guid);

    // TODO: test validations
  }

  /**
   * Liefert das Fachmodell mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getDomainSpecificModelForPublicTest() {
    UUID guid = null;
    // AIADomainSpecificModelPublicDto response = api.getDomainSpecificModelForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle Fachmodelle, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getDomainSpecificModelsForPublicTest() {
    AiaDomainSpecificModelForPublicRequest aiaDomainSpecificModelForPublicRequest = null;
    // List<SimpleDomainSpecificModelPublicDto> response =
    // api.getDomainSpecificModelsForPublic(aiaDomainSpecificModelForPublicRequest);

    // TODO: test validations
  }
}
