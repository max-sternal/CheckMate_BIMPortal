package com.bimportal.client.decoder;

import feign.Response;
import feign.codec.Decoder;
import java.io.IOException;
import java.lang.reflect.Type;
import java.util.Collection;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Custom Feign decoder that handles both JSON and binary responses. This class should be placed in
 * a separate package to avoid circular dependencies.
 */
public class BinaryResponseDecoder implements Decoder {

  private static final Logger logger = LoggerFactory.getLogger(BinaryResponseDecoder.class);

  private final Decoder jsonDecoder;

  public BinaryResponseDecoder(Decoder jsonDecoder) {
    this.jsonDecoder = jsonDecoder;
  }

  @Override
  public Object decode(Response response, Type type) throws IOException {
    // Check if the expected return type is byte[]
    if (type.equals(byte[].class)) {
      return handleBinaryResponse(response);
    }

    // Check Content-Type header to determine if response is binary
    Collection<String> contentTypeHeaders = response.headers().get("content-type");
    if (contentTypeHeaders != null && !contentTypeHeaders.isEmpty()) {
      String contentType = contentTypeHeaders.iterator().next().toLowerCase();

      if (isBinaryContentType(contentType)) {
        logger.debug("Detected binary content type: {}, returning byte array", contentType);
        return handleBinaryResponse(response);
      }
    }

    // For non-binary responses, use the original JSON decoder
    return jsonDecoder.decode(response, type);
  }

  private boolean isBinaryContentType(String contentType) {
    return contentType.contains("application/pdf")
        || contentType.contains("application/zip")
        || contentType.contains("application/octet-stream")
        || contentType.contains("application/vnd.oasis.opendocument")
        || contentType.contains("application/xml")
        || contentType.startsWith("image/")
        || contentType.startsWith("video/")
        || contentType.startsWith("audio/");
  }

  private byte[] handleBinaryResponse(Response response) throws IOException {
    if (response.body() == null) {
      return new byte[0];
    }

    try {
      byte[] bytes = response.body().asInputStream().readAllBytes();
      logger.debug("Successfully read {} bytes of binary content", bytes.length);
      return bytes;
    } catch (IOException e) {
      logger.error("Failed to read binary response", e);
      throw e;
    }
  }
}
