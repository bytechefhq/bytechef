package com.bytechef.platform.configuration.web.rest.model;

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
 * Gets or Sets credential_status
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-19T05:46:41.627972+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public enum CredentialStatusModel {
  
  VALID("VALID"),
  
  INVALID("INVALID");

  private final String value;

  CredentialStatusModel(String value) {
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
  public static CredentialStatusModel fromValue(String value) {
    for (CredentialStatusModel b : CredentialStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

