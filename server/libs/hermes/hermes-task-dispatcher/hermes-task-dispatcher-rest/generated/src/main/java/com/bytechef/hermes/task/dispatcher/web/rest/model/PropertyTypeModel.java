package com.bytechef.hermes.task.dispatcher.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import javax.validation.Valid;
import javax.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import javax.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;

/**
 * Gets or Sets PropertyType
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-10-27T16:28:51.543539+02:00[Europe/Zagreb]")
public enum PropertyTypeModel {
  
  ANY("ANY"),
  
  ARRAY("ARRAY"),
  
  BOOLEAN("BOOLEAN"),
  
  DATE_TIME("DATE_TIME"),
  
  INTEGER("INTEGER"),
  
  NULL("null"),
  
  NUMBER("NUMBER"),
  
  OBJECT("OBJECT"),
  
  OPTION("OPTION"),
  
  STRING("STRING");

  private String value;

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

