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
 * Who deployed the app instance. EXTERNAL is a deployment recorded by someone other than ByteChef; DEPLOY_HOOK is a deployment ByteChef started through the deploy hook of a self-hosted instance.
 */

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T21:34:15.725090+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public enum AppDeploymentProviderModel {
  
  VERCEL("VERCEL"),
  
  EXTERNAL("EXTERNAL"),
  
  DEPLOY_HOOK("DEPLOY_HOOK");

  private final String value;

  AppDeploymentProviderModel(String value) {
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
  public static AppDeploymentProviderModel fromValue(String value) {
    for (AppDeploymentProviderModel b : AppDeploymentProviderModel.values()) {
      if (b.value.equals(value)) {
        return b;
      }
    }
    throw new IllegalArgumentException("Unexpected value '" + value + "'");
  }
}

