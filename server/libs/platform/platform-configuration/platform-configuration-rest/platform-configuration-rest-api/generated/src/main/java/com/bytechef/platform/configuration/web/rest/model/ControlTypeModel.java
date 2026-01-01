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
 * A type of the control to show in UI.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.413708+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public enum ControlTypeModel {
  
  ARRAY_BUILDER("ARRAY_BUILDER"),
  
  CODE_EDITOR("CODE_EDITOR"),
  
  DATE("DATE"),
  
  DATE_TIME("DATE_TIME"),
  
  EMAIL("EMAIL"),
  
  INTEGER("INTEGER"),
  
  JSON_SCHEMA_BUILDER("JSON_SCHEMA_BUILDER"),
  
  FILE_ENTRY("FILE_ENTRY"),
  
  MULTI_SELECT("MULTI_SELECT"),
  
  NUMBER("NUMBER"),
  
  NULL("NULL"),
  
  OBJECT_BUILDER("OBJECT_BUILDER"),
  
  PASSWORD("PASSWORD"),
  
  PHONE("PHONE"),
  
  RICH_TEXT("RICH_TEXT"),
  
  SELECT("SELECT"),
  
  TEXT("TEXT"),
  
  TEXT_AREA("TEXT_AREA"),
  
  TIME("TIME"),
  
  URL("URL");

  private final String value;

  ControlTypeModel(String value) {
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
  public static ControlTypeModel fromValue(String value) {
    for (ControlTypeModel b : ControlTypeModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

