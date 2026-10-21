package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceModel;
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
 * An app instance together with its plaintext secret key, returned once at create and rotate and never again.
 */

@Schema(name = "AppInstanceSecretKey", description = "An app instance together with its plaintext secret key, returned once at create and rotate and never again.")
@JsonTypeName("AppInstanceSecretKey")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AppInstanceSecretKeyModel {

  private @Nullable AppInstanceModel appInstance;

  private @Nullable String secretKey;

  public AppInstanceSecretKeyModel appInstance(@Nullable AppInstanceModel appInstance) {
    this.appInstance = appInstance;
    return this;
  }

  /**
   * Get appInstance
   * @return appInstance
   */
  @Valid 
  @Schema(name = "appInstance", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appInstance")
  public @Nullable AppInstanceModel getAppInstance() {
    return appInstance;
  }

  @JsonProperty("appInstance")
  public void setAppInstance(@Nullable AppInstanceModel appInstance) {
    this.appInstance = appInstance;
  }

  public AppInstanceSecretKeyModel secretKey(@Nullable String secretKey) {
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
    AppInstanceSecretKeyModel appInstanceSecretKey = (AppInstanceSecretKeyModel) o;
    return Objects.equals(this.appInstance, appInstanceSecretKey.appInstance) &&
        Objects.equals(this.secretKey, appInstanceSecretKey.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(appInstance, secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppInstanceSecretKeyModel {\n");
    sb.append("    appInstance: ").append(toIndentedString(appInstance)).append("\n");
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

