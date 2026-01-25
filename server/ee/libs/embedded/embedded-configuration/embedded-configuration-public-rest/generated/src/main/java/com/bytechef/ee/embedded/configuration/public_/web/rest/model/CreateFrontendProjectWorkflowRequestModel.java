package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * CreateFrontendProjectWorkflowRequestModel
 */

@JsonTypeName("createFrontendProjectWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.411987+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class CreateFrontendProjectWorkflowRequestModel {

  private @Nullable String definition;

  public CreateFrontendProjectWorkflowRequestModel definition(@Nullable String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The workflow definition
   * @return definition
   */
  
  @Schema(name = "definition", description = "The workflow definition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("definition")
  public @Nullable String getDefinition() {
    return definition;
  }

  public void setDefinition(@Nullable String definition) {
    this.definition = definition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateFrontendProjectWorkflowRequestModel createFrontendProjectWorkflowRequest = (CreateFrontendProjectWorkflowRequestModel) o;
    return Objects.equals(this.definition, createFrontendProjectWorkflowRequest.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateFrontendProjectWorkflowRequestModel {\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
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

