package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import jakarta.validation.constraints.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/** API tests for AiaFilterApi */
class AiaFilterApiTest {

  private AiaFilterApi api;

  @BeforeEach
  public void setup() {
    api = new ApiClient().buildClient(AiaFilterApi.class);
  }

  /** Liefert alle globalen Filter zurück */
  @Test
  void getGlobalFilters1Test() {
    // List<FilterGroupForPublicDto> response = api.getGlobalFilters1();

    // TODO: test validations
  }
}
