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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-19T07:23:07.589358527Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public enum ToolInvocationSurfaceModel {
  
  MCP_AUTOMATION("MCP_AUTOMATION"),
  
  MCP_EMBEDDED("MCP_EMBEDDED"),
  
  MCP_MANAGEMENT("MCP_MANAGEMENT"),
  
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

