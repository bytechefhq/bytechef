package com.bytechef.automation.connection.web.rest.model;

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
 * The environment of a connection.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:56.229186+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public enum ConnectionEnvironmentModel {
  
  DEVELOPMENT("DEVELOPMENT"),
  
  TEST("TEST"),
  
  PRODUCTION("PRODUCTION");

  private String value;

  ConnectionEnvironmentModel(String value) {
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
  public static ConnectionEnvironmentModel fromValue(String value) {
    for (ConnectionEnvironmentModel b : ConnectionEnvironmentModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

