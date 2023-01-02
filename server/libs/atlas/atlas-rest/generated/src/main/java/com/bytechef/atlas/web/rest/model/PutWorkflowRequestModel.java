package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.atlas.web.rest.model.WorkflowFormatModel;
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
 * PutWorkflowRequestModel
 */

@JsonTypeName("putWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2022-12-26T09:25:21.049913+01:00[Europe/Zagreb]")
public class PutWorkflowRequestModel {

  @JsonProperty("definition")
  private String definition;

  @JsonProperty("format")
  private WorkflowFormatModel format;

  public PutWorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * Definition of the workflow that is executed as a job.
   * @return definition
  */
  @NotNull 
  @Schema(name = "definition", description = "Definition of the workflow that is executed as a job.", required = true)
  public String getDefinition() {
    return definition;
  }

  public void setDefinition(String definition) {
    this.definition = definition;
  }

  public PutWorkflowRequestModel format(WorkflowFormatModel format) {
    this.format = format;
    return this;
  }

  /**
   * Get format
   * @return format
  */
  @NotNull @Valid 
  @Schema(name = "format", required = true)
  public WorkflowFormatModel getFormat() {
    return format;
  }

  public void setFormat(WorkflowFormatModel format) {
    this.format = format;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PutWorkflowRequestModel putWorkflowRequest = (PutWorkflowRequestModel) o;
    return Objects.equals(this.definition, putWorkflowRequest.definition) &&
        Objects.equals(this.format, putWorkflowRequest.format);
  }

  @Override
  public int hashCode() {
    return Objects.hash(definition, format);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PutWorkflowRequestModel {\n");
    sb.append("    definition: ").append(toIndentedString(definition)).append("\n");
    sb.append("    format: ").append(toIndentedString(format)).append("\n");
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

