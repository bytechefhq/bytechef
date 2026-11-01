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
 * The surface a tool invocation was made through.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-01T09:08:13.694310+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public enum ToolInvocationSurfaceModel {
  
  MCP_EMBEDDED("MCP_EMBEDDED"),
  
  EMBEDDED_API_ACTION("EMBEDDED_API_ACTION"),
  
  EMBEDDED_API_TOOL("EMBEDDED_API_TOOL");

  private final String value;

  ToolInvocationSurfaceModel(String value) {
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
  public static ToolInvocationSurfaceModel fromValue(String value) {
    for (ToolInvocationSurfaceModel b : ToolInvocationSurfaceModel.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

