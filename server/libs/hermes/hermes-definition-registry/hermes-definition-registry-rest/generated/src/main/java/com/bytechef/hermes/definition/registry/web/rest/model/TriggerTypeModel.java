package com.bytechef.hermes.definition.registry.web.rest.model;

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
 * Gets or Sets TriggerType
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-05T18:35:34.469553+02:00[Europe/Zagreb]")
public enum TriggerTypeModel {
  
  DYNAMIC_WEBHOOK("DYNAMIC_WEBHOOK"),
  
  HYBRID("HYBRID"),
  
  MANUAL("MANUAL"),
  
  POLLING("POLLING"),
  
  STATIC_WEBHOOK("STATIC_WEBHOOK");

  private String value;

  TriggerTypeModel(String value) {
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
  public static TriggerTypeModel fromValue(String value) {
    for (TriggerTypeModel b : TriggerTypeModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

