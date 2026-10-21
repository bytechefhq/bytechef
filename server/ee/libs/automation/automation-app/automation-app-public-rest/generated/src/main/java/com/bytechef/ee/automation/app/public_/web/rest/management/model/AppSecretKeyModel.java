package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An app together with its plaintext secret key, returned once at create and rotate and never again.
 */

@Schema(name = "AppSecretKey", description = "An app together with its plaintext secret key, returned once at create and rotate and never again.")
@JsonTypeName("AppSecretKey")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppSecretKeyModel {

  private @Nullable AppModel app;

  private @Nullable String secretKey;

  public AppSecretKeyModel app(@Nullable AppModel app) {
    this.app = app;
    return this;
  }

  /**
   * Get app
   * @return app
   */
  @Valid 
  @Schema(name = "app", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("app")
  public @Nullable AppModel getApp() {
    return app;
  }

  @JsonProperty("app")
  public void setApp(@Nullable AppModel app) {
    this.app = app;
  }

  public AppSecretKeyModel secretKey(@Nullable String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * Get secretKey
   * @return secretKey
   */
  
  @Schema(name = "secretKey", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKey")
  public @Nullable String getSecretKey() {
    return secretKey;
  }

  @JsonProperty("secretKey")
  public void setSecretKey(@Nullable String secretKey) {
    this.secretKey = secretKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppSecretKeyModel appSecretKey = (AppSecretKeyModel) o;
    return Objects.equals(this.app, appSecretKey.app) &&
        Objects.equals(this.secretKey, appSecretKey.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(app, secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppSecretKeyModel {\n");
    sb.append("    app: ").append(toIndentedString(app)).append("\n");
    sb.append("    secretKey: ").append(toIndentedString(secretKey)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

