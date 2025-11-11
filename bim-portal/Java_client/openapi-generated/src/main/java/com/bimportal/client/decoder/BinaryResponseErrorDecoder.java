package com.bimportal.client.decoder;

import feign.Response;
import feign.codec.ErrorDecoder;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/** Custom error decoder that handles binary content in error responses. */
public class BinaryResponseErrorDecoder implements ErrorDecoder {

  private static final Logger logger = LoggerFactory.getLogger(BinaryResponseErrorDecoder.class);
  private final ErrorDecoder defaultErrorDecoder = new Default();

  @Override
  public Exception decode(String methodKey, Response response) {
    // Check if the error response might actually be binary content
    Collection<String> contentTypeHeaders = response.headers().get("content-type");
    if (contentTypeHeaders != null && !contentTypeHeaders.isEmpty()) {
      String contentType = contentTypeHeaders.iterator().next().toLowerCase();

      if (contentType.contains("application/pdf")
          || contentType.contains("application/zip")
          || contentType.contains("application/octet-stream")) {

        logger.debug(
            "Error response has binary content type: {}, might be successful binary response misinterpreted as error",
            contentType);

        // Return a special exception that indicates this might be successful binary content
        return new BinaryContentMisinterpretedException(
            "Response appears to be binary content but was treated as error. Content-Type: "
                + contentType);
      }
    }

    return defaultErrorDecoder.decode(methodKey, response);
  }

  /** Special exception to indicate that an "error" response might actually be binary content. */
  public static class BinaryContentMisinterpretedException extends Exception {
    public BinaryContentMisinterpretedException(String message) {
      super(message);
    }
  }
}
