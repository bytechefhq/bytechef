package com.bytechef.ee.platform.apiconnector.configuration.web.rest.model;

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
 * The HTTP method.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-10-20T13:40:08.019330+02:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
public enum HttpMethodModel {
  
  DELETE("DELETE"),
  
  GET("GET"),
  
  POST("POST"),
  
  PUT("PUT"),
  
  PATCH("PATCH");

  private String value;

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

