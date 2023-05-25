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
 * A type of the control to show in UI.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-23T16:33:56.395888+02:00[Europe/Zagreb]")
public enum ControlTypeModel {
  
  CHECKBOX("CHECKBOX"),
  
  CODE_EDITOR("CODE_EDITOR"),
  
  DATE("DATE"),
  
  DATE_TIME("DATE_TIME"),
  
  EMAIL("EMAIL"),
  
  EXPRESSION("EXPRESSION"),
  
  INPUT_EMAIL("INPUT_EMAIL"),
  
  INPUT_INTEGER("INPUT_INTEGER"),
  
  INPUT_NUMBER("INPUT_NUMBER"),
  
  INPUT_PASSWORD("INPUT_PASSWORD"),
  
  INPUT_PHONE("INPUT_PHONE"),
  
  INPUT_TEXT("INPUT_TEXT"),
  
  INPUT_URL("INPUT_URL"),
  
  OBJECT_BUILDER("OBJECT_BUILDER"),
  
  MULTI_SELECT("MULTI_SELECT"),
  
  PHONE("PHONE"),
  
  SCHEMA_DESIGNER("SCHEMA_DESIGNER"),
  
  SELECT("SELECT"),
  
  SUBDOMAIN("SUBDOMAIN"),
  
  TEXT_AREA("TEXT_AREA"),
  
  TIME("TIME"),
  
  URL("URL");

  private String value;

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

