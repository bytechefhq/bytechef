package com.bytechef.ee.embedded.connection.web.rest.model;

import com.fasterxml.jackson.annotation.JsonValue;


import jakarta.annotation.Generated;

import com.fasterxml.jackson.annotation.JsonCreator;

/**
 * The environment of a connection.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-05-11T23:12:21.081303+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public enum ConnectionEnvironmentModel {

  DEVELOPMENT("DEVELOPMENT"),

  STAGING("STAGING"),

  PRODUCTION("PRODUCTION");

  private String value;

  ConnectionEnvironmentModel(String value) {
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
  public static ConnectionEnvironmentModel fromValue(String value) {
    for (ConnectionEnvironmentModel b : ConnectionEnvironmentModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

