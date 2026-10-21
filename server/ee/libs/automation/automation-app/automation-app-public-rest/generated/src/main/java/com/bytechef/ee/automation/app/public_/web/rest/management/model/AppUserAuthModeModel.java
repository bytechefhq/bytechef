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
 * Whether the app instance signs its users in.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum AppUserAuthModeModel {
  
  NONE("NONE"),
  
  OPTIONAL("OPTIONAL"),
  
  REQUIRED("REQUIRED");

  private final String value;

  AppUserAuthModeModel(String value) {
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
  public static AppUserAuthModeModel fromValue(String value) {
    for (AppUserAuthModeModel b : AppUserAuthModeModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

