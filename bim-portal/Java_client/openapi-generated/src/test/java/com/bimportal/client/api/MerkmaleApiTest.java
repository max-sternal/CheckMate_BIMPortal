package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for MerkmaleApi */
class MerkmaleApiTest {

  private MerkmaleApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(MerkmaleApi.class);
  }

  /** Liefert alle globalen Filter zurück */
  @Test
  void getGlobalFiltersTest() {
    // List<TagGroupForPublicDto> response = api.getGlobalFilters();

    // TODO: test validations
  }

  /**
   * Liefert alle Merkmale, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Merkmale abrufen zu können.
   */
  @Test
  void getPropertiesForPublicTest() {
    PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest = null;
    // List<PropertyOrGroupForPublicDto> response =
    // api.getPropertiesForPublic(propertyOrGroupForPublicRequest);

    // TODO: test validations
  }

  /**
   * Liefert das Merkmal mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Merkmale abrufen zu können.
   */
  @Test
  void getPropertyForPublicTest() {
    UUID guid = null;
    // PropertyDto response = api.getPropertyForPublic(guid);

    // TODO: test validations
  }
}
