package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.PropertyOrGroupForPublicRequest;
import jakarta.validation.constraints.*;
import java.util.UUID;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for MerkmalsgruppenApi */
class MerkmalsgruppenApiTest {

  private MerkmalsgruppenApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(MerkmalsgruppenApi.class);
  }

  /**
   * Liefert die Merkmalsgruppe mit der gesuchten GUID
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   */
  @Test
  void getPropertyGroupForPublicTest() {
    UUID guid = null;
    // PropertyGroupDto response = api.getPropertyGroupForPublic(guid);

    // TODO: test validations
  }

  /**
   * Liefert alle Merkmalsgruppen, die zu den Suchparametern passen
   *
   * <p>Diese API erlaubt optional eine Authentifizierung mittels &#39;Bearer {accessToken}&#39;
   * über den Authorization-Header, um nicht öffentlich sichtbare Merkmalsgruppen abrufen zu können.
   */
  @Test
  void getProperyGroupsForPublicTest() {
    PropertyOrGroupForPublicRequest propertyOrGroupForPublicRequest = null;
    // List<PropertyOrGroupForPublicDto> response =
    // api.getProperyGroupsForPublic(propertyOrGroupForPublicRequest);

    // TODO: test validations
  }
}
