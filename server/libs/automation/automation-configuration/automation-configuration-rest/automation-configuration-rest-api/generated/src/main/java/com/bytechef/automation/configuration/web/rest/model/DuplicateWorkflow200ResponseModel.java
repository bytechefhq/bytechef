package com.bytechef.automation.configuration.web.rest.model;

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
 * DuplicateWorkflow200ResponseModel
 */

@JsonTypeName("duplicateWorkflow_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-28T12:19:12.459673+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class DuplicateWorkflow200ResponseModel {

  private @Nullable String id;

  public DuplicateWorkflow200ResponseModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the duplicated workflow.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the duplicated workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DuplicateWorkflow200ResponseModel duplicateWorkflow200Response = (DuplicateWorkflow200ResponseModel) o;
    return Objects.equals(this.id, duplicateWorkflow200Response.id);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DuplicateWorkflow200ResponseModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
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

