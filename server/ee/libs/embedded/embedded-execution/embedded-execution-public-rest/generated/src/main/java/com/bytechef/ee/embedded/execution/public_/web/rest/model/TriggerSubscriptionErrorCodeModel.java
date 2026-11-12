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
 * The code of a trigger subscription error.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T00:36:32.396363+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum TriggerSubscriptionErrorCodeModel {
  
  USER_INPUT_ERROR("USER_INPUT_ERROR"),
  
  API_ERROR("API_ERROR"),
  
  TIMEOUT_ERROR("TIMEOUT_ERROR"),
  
  OAUTH_REFRESH("OAUTH_REFRESH"),
  
  NO_TRIGGER("NO_TRIGGER"),
  
  INTERNAL_ERROR("INTERNAL_ERROR");

  private final String value;

  TriggerSubscriptionErrorCodeModel(String value) {
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
  public static TriggerSubscriptionErrorCodeModel fromValue(String value) {
    for (TriggerSubscriptionErrorCodeModel b : TriggerSubscriptionErrorCodeModel.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

