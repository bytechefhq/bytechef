package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * The authorization type.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-12T08:42:40.890500807+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public enum AuthorizationTypeModel {
  
  API_KEY("API_KEY"),
  
  BASIC_AUTH("BASIC_AUTH"),
  
  BEARER_TOKEN("BEARER_TOKEN"),
  
  CUSTOM("CUSTOM"),
  
  DIGEST_AUTH("DIGEST_AUTH"),
  
  OAUTH2_AUTHORIZATION_CODE("OAUTH2_AUTHORIZATION_CODE"),
  
  OAUTH2_AUTHORIZATION_CODE_PKCE("OAUTH2_AUTHORIZATION_CODE_PKCE"),
  
  OAUTH2_CLIENT_CREDENTIALS("OAUTH2_CLIENT_CREDENTIALS"),
  
  OAUTH2_IMPLICIT_CODE("OAUTH2_IMPLICIT_CODE"),
  
  OAUTH2_RESOURCE_OWNER_PASSWORD("OAUTH2_RESOURCE_OWNER_PASSWORD");

  private final String value;

  AuthorizationTypeModel(String value) {
    this.value = value;
  }

  @JsonValue
  public String getValue() {
    return value;
  }

  @Override
  public String toString() {
    return String.valueOf(value);
  }

  @JsonCreator
  public static AuthorizationTypeModel fromValue(String value) {
    for (AuthorizationTypeModel b : AuthorizationTypeModel.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

