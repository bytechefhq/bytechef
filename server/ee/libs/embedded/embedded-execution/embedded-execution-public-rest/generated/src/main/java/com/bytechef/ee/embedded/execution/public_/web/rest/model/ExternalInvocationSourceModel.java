package com.bytechef.ee.embedded.execution.public_.web.rest.model;

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
 * The source a external invocation was made through.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T00:36:32.396363+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum ExternalInvocationSourceModel {
  
  MCP_SERVER("MCP_SERVER"),
  
  API_ACTION("API_ACTION"),
  
  API_TOOL("API_TOOL"),
  
  A2_A_SERVER("A2A_SERVER"),
  
  API_TRIGGER("API_TRIGGER");

  private final String value;

  ExternalInvocationSourceModel(String value) {
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
  public static ExternalInvocationSourceModel fromValue(String value) {
    for (ExternalInvocationSourceModel b : ExternalInvocationSourceModel.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

