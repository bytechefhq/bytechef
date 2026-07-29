package com.bytechef.ee.automation.configuration.public_.web.rest.model;

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
 * The status of a workflow execution.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:02.539074685Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public enum WorkflowExecutionStatusModel {
  
  CREATED("CREATED"),
  
  STARTED("STARTED"),
  
  STOPPED("STOPPED"),
  
  CANCELLED("CANCELLED"),
  
  FAILED("FAILED"),
  
  COMPLETED("COMPLETED");

  private final String value;

  WorkflowExecutionStatusModel(String value) {
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
  public static WorkflowExecutionStatusModel fromValue(String value) {
    for (WorkflowExecutionStatusModel b : WorkflowExecutionStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

