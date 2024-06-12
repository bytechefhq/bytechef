package com.bytechef.embedded.configuration.public_.web.rest.model;

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

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T07:17:52.346689+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public enum EnvironmentModel {
  
  test("test"),
  
  production("production");

  private String value;

  EnvironmentModel(String value) {
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
  public static EnvironmentModel fromValue(String value) {
    for (EnvironmentModel b : EnvironmentModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

