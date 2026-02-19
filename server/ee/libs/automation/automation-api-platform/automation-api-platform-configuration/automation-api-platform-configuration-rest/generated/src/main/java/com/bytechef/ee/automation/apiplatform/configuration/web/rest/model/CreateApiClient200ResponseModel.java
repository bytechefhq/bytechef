package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * CreateApiClient200ResponseModel
 */

@JsonTypeName("createApiClient_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.369127+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class CreateApiClient200ResponseModel {

  private @Nullable String secretKey;

  public CreateApiClient200ResponseModel secretKey(@Nullable String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * The secret API key.
   * @return secretKey
   */
  
  @Schema(name = "secretKey", description = "The secret API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKey")
  public @Nullable String getSecretKey() {
    return secretKey;
  }

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
    CreateApiClient200ResponseModel createApiClient200Response = (CreateApiClient200ResponseModel) o;
    return Objects.equals(this.secretKey, createApiClient200Response.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateApiClient200ResponseModel {\n");
    sb.append("    secretKey: ").append(toIndentedString(secretKey)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

