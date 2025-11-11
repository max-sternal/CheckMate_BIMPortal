package com.pb40.bimportal.auth;

/** Custom exception for authentication-related errors in BIM Portal API. */
public class AuthenticationException extends Exception {

  public AuthenticationException(String message) {
    super(message);
  }

  public AuthenticationException(String message, Throwable cause) {
    super(message, cause);
  }

  public AuthenticationException(Throwable cause) {
    super(cause);
  }
}
