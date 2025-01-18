package com.bytechef.automation.configuration.web.rest.model;

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
 * Gets or Sets IncludeField
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-09T19:56:34.112361+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public enum IncludeFieldModel {
  
  ALL("ALL"),
  
  BASIC("BASIC");

  private String value;

  IncludeFieldModel(String value) {
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
  public static IncludeFieldModel fromValue(String value) {
    for (IncludeFieldModel b : IncludeFieldModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}
