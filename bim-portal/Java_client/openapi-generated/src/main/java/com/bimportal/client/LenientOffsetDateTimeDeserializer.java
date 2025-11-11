package com.bimportal.client;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.DeserializationContext;
import com.fasterxml.jackson.databind.JsonDeserializer;
import java.io.IOException;
import java.time.OffsetDateTime;

public class LenientOffsetDateTimeDeserializer extends JsonDeserializer<OffsetDateTime> {

  @Override
  public OffsetDateTime deserialize(JsonParser p, DeserializationContext ctxt) throws IOException {
    String dateString = p.getText();
    if (dateString == null || dateString.isEmpty()) {
      return null;
    }

    try {
      return parseFlexibleDateTime(dateString);
    } catch (Exception e) {
      throw new IOException("Failed to parse date: " + dateString, e);
    }
  }

  private OffsetDateTime parseFlexibleDateTime(String dateString) {
    // Handle the specific format: "2025-09-05T19:14:36.27571144" (no timezone, 8-digit fractional
    // seconds)

    if (dateString.contains(".")
        && !dateString.endsWith("Z")
        && !dateString.contains("+")
        && dateString.length() > 19) {

      int dotIndex = dateString.indexOf('.');
      String timePart = dateString.substring(dotIndex + 1);

      // Normalize fractional seconds to nanoseconds (9 digits)
      String normalizedFractional;
      if (timePart.length() > 9) {
        normalizedFractional = timePart.substring(0, 9); // Truncate to 9 digits
      } else if (timePart.length() < 9) {
        normalizedFractional = String.format("%-9s", timePart).replace(' ', '0'); // Pad to 9 digits
      } else {
        normalizedFractional = timePart;
      }

      // Reconstruct with normalized fractional seconds and assume UTC timezone
      String normalizedDate = dateString.substring(0, dotIndex + 1) + normalizedFractional + "Z";

      try {
        return OffsetDateTime.parse(normalizedDate);
      } catch (Exception e) {
        // If parsing still fails, try without timezone
        String localDate = dateString.substring(0, dotIndex + 1) + normalizedFractional;
        return OffsetDateTime.parse(localDate + "Z"); // Force UTC
      }
    }

    // For other formats, try standard parsing
    try {
      return OffsetDateTime.parse(dateString);
    } catch (Exception e) {
      // If standard parsing fails, try adding UTC timezone
      if (!dateString.endsWith("Z") && !dateString.contains("+") && !dateString.contains("-")) {
        return OffsetDateTime.parse(dateString + "Z");
      }
      throw e;
    }
  }
}
