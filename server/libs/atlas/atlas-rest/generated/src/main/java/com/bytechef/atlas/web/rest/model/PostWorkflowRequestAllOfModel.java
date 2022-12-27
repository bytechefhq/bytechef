package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * PostWorkflowRequestAllOfModel
 */

@JsonTypeName("postWorkflow_request_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-18T09:37:42.127301+01:00[Europe/Zagreb]")
public class PostWorkflowRequestAllOfModel {

  /**
   * Gets or Sets providerType
   */
  public enum ProviderTypeEnum {
    JDBC("JDBC");

    private String value;

    ProviderTypeEnum(String value) {
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
    public static ProviderTypeEnum fromValue(String value) {
      for (ProviderTypeEnum b : ProviderTypeEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  @JsonProperty("providerType")
  private ProviderTypeEnum providerType;

  public PostWorkflowRequestAllOfModel providerType(ProviderTypeEnum providerType) {
    this.providerType = providerType;
    return this;
  }

  /**
   * Get providerType
   * @return providerType
  */
  
  @Schema(name = "providerType", required = false)
  public ProviderTypeEnum getProviderType() {
    return providerType;
  }

  public void setProviderType(ProviderTypeEnum providerType) {
    this.providerType = providerType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PostWorkflowRequestAllOfModel postWorkflowRequestAllOf = (PostWorkflowRequestAllOfModel) o;
    return Objects.equals(this.providerType, postWorkflowRequestAllOf.providerType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(providerType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostWorkflowRequestAllOfModel {\n");
    sb.append("    providerType: ").append(toIndentedString(providerType)).append("\n");
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

