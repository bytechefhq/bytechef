package com.bytechef.embedded.user.web.rest.model;

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
 * CreateSigningKey200ResponseModel
 */

@JsonTypeName("createSigningKey_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-26T21:19:57.600029+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class CreateSigningKey200ResponseModel {

  private String privateKey;

  public CreateSigningKey200ResponseModel privateKey(String privateKey) {
    this.privateKey = privateKey;
    return this;
  }

  /**
   * The private key.
   * @return privateKey
   */
  
  @Schema(name = "privateKey", description = "The private key.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("privateKey")
  public String getPrivateKey() {
    return privateKey;
  }

  public void setPrivateKey(String privateKey) {
    this.privateKey = privateKey;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateSigningKey200ResponseModel createSigningKey200Response = (CreateSigningKey200ResponseModel) o;
    return Objects.equals(this.privateKey, createSigningKey200Response.privateKey);
  }

  @Override
  public int hashCode() {
    return Objects.hash(privateKey);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateSigningKey200ResponseModel {\n");
    sb.append("    privateKey: ").append(toIndentedString(privateKey)).append("\n");
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

