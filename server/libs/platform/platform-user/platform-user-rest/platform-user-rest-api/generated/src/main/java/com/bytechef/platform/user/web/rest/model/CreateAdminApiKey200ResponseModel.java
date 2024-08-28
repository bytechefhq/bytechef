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
 * CreateAdminApiKey200ResponseModel
 */

@JsonTypeName("createAdminApiKey_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-08-23T15:57:36.320238+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class CreateAdminApiKey200ResponseModel {

  private String secretKey;

  public CreateAdminApiKey200ResponseModel secretKey(String secretKey) {
    this.secretKey = secretKey;
    return this;
  }

  /**
   * The secret admin API key.
   * @return secretKey
  */
  
  @Schema(name = "secretKey", description = "The secret admin API key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    CreateAdminApiKey200ResponseModel createAdminApiKey200Response = (CreateAdminApiKey200ResponseModel) o;
    return Objects.equals(this.secretKey, createAdminApiKey200Response.secretKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(secretKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateAdminApiKey200ResponseModel {\n");
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

