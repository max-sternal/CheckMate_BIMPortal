package com.bimportal.client.api;

import com.bimportal.client.ApiClient;
import com.bimportal.client.model.ApiResponse;
import com.bimportal.client.model.FilterGroupForPublicDto;
import feign.*;
import jakarta.validation.constraints.*;
import java.util.List;

@jakarta.annotation.Generated(
    value = "org.openapitools.codegen.languages.JavaClientCodegen",
    date = "2025-09-03T22:05:51.794303+02:00[Europe/Berlin]",
    comments = "Generator version: 7.9.0")
public interface AiaFilterApi extends ApiClient.Api {

  /**
   * Liefert alle globalen Filter zurück
   *
   * @return List&lt;FilterGroupForPublicDto&gt;
   */
  @RequestLine("GET /aia/api/v1/public/filter")
  @Headers({
    "Accept: application/json",
  })
  List<FilterGroupForPublicDto> getGlobalFilters1();

  /**
   * Liefert alle globalen Filter zurück Similar to <code>getGlobalFilters1</code> but it also
   * returns the http response headers .
   *
   * @return A ApiResponse that wraps the response boyd and the http headers.
   */
  @RequestLine("GET /aia/api/v1/public/filter")
  @Headers({
    "Accept: application/json",
  })
  ApiResponse<List<FilterGroupForPublicDto>> getGlobalFilters1WithHttpInfo();
}
