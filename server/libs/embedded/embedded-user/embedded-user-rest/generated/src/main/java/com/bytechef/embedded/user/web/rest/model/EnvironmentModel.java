package com.bytechef.embedded.user.web.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The environment of a project.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-29T08:47:23.140561+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public enum EnvironmentModel {

  TEST("TEST"),

  PRODUCTION("PRODUCTION");

  private String value;

  EnvironmentModel(String value) {
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
  public static EnvironmentModel fromValue(String value) {
    for (EnvironmentModel b : EnvironmentModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

