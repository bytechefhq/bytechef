package com.bytechef.platform.user.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * CreateApiKey200ResponseModel
 */

@JsonTypeName("createApiKey_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:20:00.613241+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class CreateApiKey200ResponseModel {

  private String secretKey;

  public CreateApiKey200ResponseModel secretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * The secret API key.
   * @return secretKey
   */
  
  @Schema(name = "secretKey", description = "The secret API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("secretKey")
  public String getSecretKey() {
    return secretKey;
  }

  public void setSecretKey(String secretKey) {
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
    CreateApiKey200ResponseModel createApiKey200Response = (CreateApiKey200ResponseModel) o;
    return Objects.equals(this.secretKey, createApiKey200Response.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateApiKey200ResponseModel {\n");
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

