package com.bimportal.hackathon.ifc;

/**
 * An IFC model could not be read.
 *
 * <p>Thrown instead of handing back an empty model: an empty model checks as "0 Objekte" and every
 * LOIN as "nicht prüfbar", which reads like a finding about the model rather than a failure to read
 * it. The message is written for the person who chose the file.
 */
public final class ModelLoadException extends RuntimeException {

  public ModelLoadException(String message) {
    super(message);
  }

  public ModelLoadException(String message, Throwable cause) {
    super(message, cause);
  }
}
