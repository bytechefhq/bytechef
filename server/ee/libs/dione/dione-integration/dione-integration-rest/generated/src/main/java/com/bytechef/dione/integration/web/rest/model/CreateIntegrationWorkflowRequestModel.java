package com.bytechef.dione.integration.web.rest.model;

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
 * CreateIntegrationWorkflowRequestModel
 */

@JsonTypeName("createIntegrationWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-27T06:33:32.145383+02:00[Europe/Zagreb]")
public class CreateIntegrationWorkflowRequestModel {

  private String label;

  private String description;

  private String definition;

  /**
   * Default constructor
   * @deprecated Use {@link CreateIntegrationWorkflowRequestModel#CreateIntegrationWorkflowRequestModel(String)}
   */
  @Deprecated
  public CreateIntegrationWorkflowRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateIntegrationWorkflowRequestModel(String label) {
    this.label = label;
  }

  public CreateIntegrationWorkflowRequestModel label(String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name for a workflow.
   * @return label
  */
  @NotNull 
  @Schema(name = "label", description = "The descriptive name for a workflow.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("label")
  public String getLabel() {
    return label;
  }

  public void setLabel(String label) {
    this.label = label;
  }

  public CreateIntegrationWorkflowRequestModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The workflow description.
   * @return description
  */
  
  @Schema(name = "description", description = "The workflow description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public CreateIntegrationWorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The workflow definition.
   * @return definition
  */
  
  @Schema(name = "definition", description = "The workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    CreateIntegrationWorkflowRequestModel createIntegrationWorkflowRequest = (CreateIntegrationWorkflowRequestModel) o;
    return Objects.equals(this.label, createIntegrationWorkflowRequest.label) &&
        Objects.equals(this.description, createIntegrationWorkflowRequest.description) &&
        Objects.equals(this.definition, createIntegrationWorkflowRequest.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, description, definition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateIntegrationWorkflowRequestModel {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
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

