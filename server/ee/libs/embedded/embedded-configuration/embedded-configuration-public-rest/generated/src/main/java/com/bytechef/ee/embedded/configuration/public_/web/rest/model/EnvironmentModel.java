package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The environment of a project.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-05-11T23:12:20.851038+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public enum EnvironmentModel {

  DEVELOPMENT("DEVELOPMENT"),

  STAGING("STAGING"),

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
      if (b.value.equalsIgnoreCase(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

