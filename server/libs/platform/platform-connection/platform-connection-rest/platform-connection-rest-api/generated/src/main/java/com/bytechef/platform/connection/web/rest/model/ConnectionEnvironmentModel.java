package com.bytechef.platform.connection.web.rest.model;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonValue;
import jakarta.annotation.Generated;

/**
 * The environment of a connection.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-27T15:02:07.192340+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public enum ConnectionEnvironmentModel {

  DEVELOPMENT("DEVELOPMENT"),

  TEST("TEST"),

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

