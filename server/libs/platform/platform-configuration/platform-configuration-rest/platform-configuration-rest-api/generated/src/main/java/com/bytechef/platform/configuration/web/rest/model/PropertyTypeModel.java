package com.bytechef.platform.configuration.web.rest.model;

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
 * A type of property.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public enum PropertyTypeModel {
  
  ARRAY("ARRAY"),
  
  BOOLEAN("BOOLEAN"),
  
  DATE("DATE"),
  
  DATE_TIME("DATE_TIME"),
  
  DYNAMIC_PROPERTIES("DYNAMIC_PROPERTIES"),
  
  FILE_ENTRY("FILE_ENTRY"),
  
  INTEGER("INTEGER"),
  
  NULL("NULL"),
  
  NUMBER("NUMBER"),
  
  STRING("STRING"),
  
  OBJECT("OBJECT"),
  
  TASK("TASK"),
  
  TIME("TIME");

  private final String value;

  PropertyTypeModel(String value) {
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
  public static PropertyTypeModel fromValue(String value) {
    for (PropertyTypeModel b : PropertyTypeModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

