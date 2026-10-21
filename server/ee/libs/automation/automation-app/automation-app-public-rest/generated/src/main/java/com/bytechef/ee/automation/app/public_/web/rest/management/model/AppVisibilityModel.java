package com.bytechef.ee.automation.app.public_.web.rest.management.model;

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
 * Who can see the app. Apps do not support every visibility.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public enum AppVisibilityModel {
  
  PRIVATE("PRIVATE"),
  
  WORKSPACE("WORKSPACE"),
  
  ORGANIZATION("ORGANIZATION");

  private final String value;

  AppVisibilityModel(String value) {
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
  public static AppVisibilityModel fromValue(String value) {
    for (AppVisibilityModel b : AppVisibilityModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

