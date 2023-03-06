package com.bytechef.dione.integration.web.rest.model;

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
 * A status of an integration.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-04T17:08:22.844227+01:00[Europe/Zagreb]")
public enum StatusModel {
  
  PUBLISHED("PUBLISHED"),
  
  UNPUBLISHED("UNPUBLISHED");

  private String value;

  StatusModel(String value) {
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
  public static StatusModel fromValue(String value) {
    for (StatusModel b : StatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

