package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

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
 * The endpoint HTTP method.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.369127+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public enum HttpMethodModel {
  
  DELETE("DELETE"),
  
  GET("GET"),
  
  PATCH("PATCH"),
  
  POST("POST"),
  
  PUT("PUT");

  private final String value;

  HttpMethodModel(String value) {
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
  public static HttpMethodModel fromValue(String value) {
    for (HttpMethodModel b : HttpMethodModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

