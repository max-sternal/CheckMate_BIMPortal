package com.bimportal.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.core.JsonToken;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import com.fasterxml.jackson.databind.JsonNode;
import java.io.IOException;

/**
 * Reads a String even when the API sends an object instead.
 *
 * <p>The OpenAPI spec declares several fields as plain string lists, but the BIM Portal returns
 * objects for some of them - {@code purposes}, for example, comes back as {@code [{"name": "...",
 * "alternativeIdentifier": "000"}]} while the generated DTOs expect {@code List<String>}. Without
 * this, every search endpoint fails to deserialize. Objects collapse to their {@code name} (falling
 * back to {@code alternativeIdentifier}, then to the raw JSON); scalars behave as before.
 */
public class LenientStringDeserializer extends JsonDeserializer<String> {

  @Override
  public String deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    if (p.currentToken() == JsonToken.START_OBJECT) {
      JsonNode node = p.readValueAsTree();

      JsonNode name = node.get("name");
      if (name != null && name.isTextual()) {
        return name.asText();
      }

      JsonNode identifier = node.get("alternativeIdentifier");
      if (identifier != null && identifier.isTextual()) {
        return identifier.asText();
      }

      return node.toString();
    }

    return p.getValueAsString();
  }
}
