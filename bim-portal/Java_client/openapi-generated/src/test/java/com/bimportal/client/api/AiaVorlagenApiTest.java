package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.AiaTemplateForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for AiaVorlagenApi */
class AiaVorlagenApiTest {

  private AiaVorlagenApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(AiaVorlagenApi.class);
  }

  /**
   * Liefert die Vorlage mit der übergebenen GUID im OpenOffice-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportOdt3Test() {
    UUID guid = null;
    // byte[] response = api.exportOdt3(guid);

    // TODO: test validations
  }

  /**
   * Liefert die Vorlage mit der übergebenen GUID im PDF-Format zurück
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void exportPdf3Test() {
    UUID guid = null;
    // byte[] response = api.exportPdf3(guid);

    // TODO: test validations
  }

  /**
   * Liefert die Vorlage mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getTemplateForPublicTest() {
    UUID guid = null;
    // AIATemplatePublicDto response = api.getTemplateForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle Vorlagen, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Informationselemente abrufen zu
   * können.
   */
  @Test
  void getTemplatesForPublicTest() {
    AiaTemplateForPublicRequest aiaTemplateForPublicRequest = null;
    // List<SimpleAiaTemplatePublicDto> response =
    // api.getTemplatesForPublic(aiaTemplateForPublicRequest);

    // TODO: test validations
  }
}
