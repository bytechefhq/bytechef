package com.bytechef.ee.embedded.configuration.web.rest.model;

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
 * The status of an integration.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-07T14:49:49.053188+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public enum IntegrationStatusModel {
  
  DRAFT("DRAFT"),
  
  PUBLISHED("PUBLISHED");

  private final String value;

  IntegrationStatusModel(String value) {
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
  public static IntegrationStatusModel fromValue(String value) {
    for (IntegrationStatusModel b : IntegrationStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

