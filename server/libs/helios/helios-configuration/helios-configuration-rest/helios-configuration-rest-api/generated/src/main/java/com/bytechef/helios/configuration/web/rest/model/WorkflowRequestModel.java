package com.bytechef.helios.configuration.web.rest.model;

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
 * WorkflowRequestModel
 */

@JsonTypeName("WorkflowRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-11T16:20:12.810736+01:00[Europe/Zagreb]")
public class WorkflowRequestModel {

  private String definition;

  public WorkflowRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowRequestModel(String definition) {
    this.definition = definition;
  }

  public WorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The definition of a workflow.
   * @return definition
  */
  @NotNull 
  @Schema(name = "definition", description = "The definition of a workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("definition")
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
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
    WorkflowRequestModel workflowRequest = (WorkflowRequestModel) o;
    return Objects.equals(this.definition, workflowRequest.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowRequestModel {\n");
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

