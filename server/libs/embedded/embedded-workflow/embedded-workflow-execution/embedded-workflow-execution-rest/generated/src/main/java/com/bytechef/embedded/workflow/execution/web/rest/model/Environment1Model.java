package com.bytechef.embedded.workflow.execution.web.rest.model;

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
 * The environment of an integration.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-30T07:20:53.946578+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public enum Environment1Model {
  
  TEST("TEST"),
  
  PRODUCTION("PRODUCTION");

  private String value;

  Environment1Model(String value) {
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
  public static Environment1Model fromValue(String value) {
    for (Environment1Model b : Environment1Model.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

