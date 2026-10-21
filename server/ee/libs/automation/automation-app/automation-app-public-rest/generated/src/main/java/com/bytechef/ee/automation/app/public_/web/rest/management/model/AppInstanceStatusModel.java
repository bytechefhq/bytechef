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
 * The status of an app instance. A suspended or terminated instance rejects every call made with its secret key; TERMINATED is final.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum AppInstanceStatusModel {
  
  ACTIVE("ACTIVE"),
  
  SUSPENDED("SUSPENDED"),
  
  TERMINATED("TERMINATED");

  private final String value;

  AppInstanceStatusModel(String value) {
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
  public static AppInstanceStatusModel fromValue(String value) {
    for (AppInstanceStatusModel b : AppInstanceStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

