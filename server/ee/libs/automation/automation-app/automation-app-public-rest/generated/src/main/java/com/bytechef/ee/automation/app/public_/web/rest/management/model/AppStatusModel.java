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
 * The status of an app. A suspended or archived app rejects every call made with the secret key of any of its instances.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum AppStatusModel {
  
  DRAFT("DRAFT"),
  
  ACTIVE("ACTIVE"),
  
  SUSPENDED("SUSPENDED"),
  
  ARCHIVED("ARCHIVED");

  private final String value;

  AppStatusModel(String value) {
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
  public static AppStatusModel fromValue(String value) {
    for (AppStatusModel b : AppStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

