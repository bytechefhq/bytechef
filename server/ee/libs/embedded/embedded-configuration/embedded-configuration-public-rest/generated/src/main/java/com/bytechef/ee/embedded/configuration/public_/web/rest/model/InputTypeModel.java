package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * The type of an input, for example \\\"STRING\\\"
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-02T07:57:54.259635598+02:00[Europe/Zagreb]", comments = "Generator version: 7.15.0")
public enum InputTypeModel {
  
  BOOLEAN("BOOLEAN"),
  
  DATE("DATE"),
  
  DATE_TIME("DATE_TIME"),
  
  INTEGER("INTEGER"),
  
  NUMBER("NUMBER"),
  
  STRING("STRING"),
  
  TIME("TIME");

  private final String value;

  InputTypeModel(String value) {
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
  public static InputTypeModel fromValue(String value) {
    for (InputTypeModel b : InputTypeModel.values()) {
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

