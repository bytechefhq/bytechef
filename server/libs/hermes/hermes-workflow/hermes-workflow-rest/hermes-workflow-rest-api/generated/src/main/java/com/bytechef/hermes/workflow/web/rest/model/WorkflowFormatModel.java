package com.bytechef.hermes.workflow.web.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * Gets or Sets WorkflowFormat
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-31T07:18:30.644746+02:00[Europe/Zagreb]")
public enum WorkflowFormatModel {

  JSON("JSON"),

  YAML("YAML");

  private String value;

  WorkflowFormatModel(String value) {
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
  public static WorkflowFormatModel fromValue(String value) {
    for (WorkflowFormatModel b : WorkflowFormatModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

