package com.bytechef.platform.configuration.web.rest.model;

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
 * CheckWorkflowNodeTestOutputExists200ResponseModel
 */

@JsonTypeName("checkWorkflowNodeTestOutputExists_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-07T09:52:01.007100+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class CheckWorkflowNodeTestOutputExists200ResponseModel {

  private Boolean exists;

  public CheckWorkflowNodeTestOutputExists200ResponseModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CheckWorkflowNodeTestOutputExists200ResponseModel(Boolean exists) {
    this.exists = exists;
  }

  public CheckWorkflowNodeTestOutputExists200ResponseModel exists(Boolean exists) {
    this.exists = exists;
    return this;
  }

  /**
   * Get exists
   * @return exists
   */
  @NotNull 
  @Schema(name = "exists", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("exists")
  public Boolean getExists() {
    return exists;
  }

  public void setExists(Boolean exists) {
    this.exists = exists;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CheckWorkflowNodeTestOutputExists200ResponseModel checkWorkflowNodeTestOutputExists200Response = (CheckWorkflowNodeTestOutputExists200ResponseModel) o;
    return Objects.equals(this.exists, checkWorkflowNodeTestOutputExists200Response.exists);
  }

  @Override
  public int hashCode() {
    return Objects.hash(exists);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CheckWorkflowNodeTestOutputExists200ResponseModel {\n");
    sb.append("    exists: ").append(toIndentedString(exists)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

