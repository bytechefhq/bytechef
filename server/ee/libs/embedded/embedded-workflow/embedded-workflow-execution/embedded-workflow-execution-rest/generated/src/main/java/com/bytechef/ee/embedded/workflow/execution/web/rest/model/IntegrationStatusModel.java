package com.bytechef.ee.embedded.workflow.execution.web.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The status of an integration.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-05-11T23:14:41.133232+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public enum IntegrationStatusModel {

  DRAFT("DRAFT"),

  PUBLISHED("PUBLISHED");

  private String value;

  IntegrationStatusModel(String value) {
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
  public static IntegrationStatusModel fromValue(String value) {
    for (IntegrationStatusModel b : IntegrationStatusModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

