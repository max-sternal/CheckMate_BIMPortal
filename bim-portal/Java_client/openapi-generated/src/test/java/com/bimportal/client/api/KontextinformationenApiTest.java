package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AiaContextInfoPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for KontextinformationenApi */
class KontextinformationenApiTest {

  private KontextinformationenApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(KontextinformationenApi.class);
  }

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im OpenOffice-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOdt2Test() {
    UUID guid = null;
    // byte[] response = api.exportOdt2(guid);

    // TODO: test validations
  }

  /**
   * Liefert die Kontextinformation mit der übergebenen GUID im PDF-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportPdf2Test() {
    UUID guid = null;
    // byte[] response = api.exportPdf2(guid);

    // TODO: test validations
  }

  /**
   * Liefert die Kontextinformation mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getContextInfoForPublicTest() {
    UUID guid = null;
    // AIAContextInfoPublicDto response = api.getContextInfoForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle Kontextinformationen, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getContextInfosForPublicTest() {
    AiaContextInfoPublicRequest aiaContextInfoPublicRequest = null;
    // List<SimpleContextInfoPublicDto> response =
    // api.getContextInfosForPublic(aiaContextInfoPublicRequest);

    // TODO: test validations
  }
}
