package com.bimportal.hackathon.ids;

/**
 * The requirements for a check could not be had: no LOIN with that ID, no IDS export, an IDS file
 * that is missing or not IDS, or no specification for the model's schema.
 *
 * <p>The message is written for whoever started the check, in German like the rest of the report,
 * and names the LOIN or file. Each of these cases would otherwise end as an empty report, which
 * reads like a pass.
 */
public final class RequirementsException extends RuntimeException {

  public RequirementsException(String message) {
    super(message);
  }

  public RequirementsException(String message, Throwable cause) {
    super(message, cause);
  }
}
