package com.bytechef.automation.workflow.execution.web.rest.model;

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
 * The status of a project.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-10-28T06:11:36.183552+01:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
public enum ProjectStatusModel {
  
  DRAFT("DRAFT"),
  
  PUBLISHED("PUBLISHED");

  private String value;

  ProjectStatusModel(String value) {
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
  public static ProjectStatusModel fromValue(String value) {
    for (ProjectStatusModel b : ProjectStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

