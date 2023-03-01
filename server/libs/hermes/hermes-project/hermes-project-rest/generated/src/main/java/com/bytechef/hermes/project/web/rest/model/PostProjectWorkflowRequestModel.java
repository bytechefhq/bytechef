package com.bytechef.hermes.project.web.rest.model;

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
 * PostProjectWorkflowRequestModel
 */

@JsonTypeName("postProjectWorkflow_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-01T11:35:39.488347+01:00[Europe/Zagreb]")
public class PostProjectWorkflowRequestModel {

  @JsonProperty("name")
  private String name;

  @JsonProperty("description")
  private String description;

  @JsonProperty("definition")
  private String definition;

  public PostProjectWorkflowRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The workflow name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The workflow name.", requiredMode = Schema.RequiredMode.REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PostProjectWorkflowRequestModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The workflow description.
   * @return description
  */
  
  @Schema(name = "description", description = "The workflow description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PostProjectWorkflowRequestModel definition(String definition) {
    this.definition = definition;
    return this;
  }

  /**
   * The workflow definition.
   * @return definition
  */
  
  @Schema(name = "definition", description = "The workflow definition.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    PostProjectWorkflowRequestModel postProjectWorkflowRequest = (PostProjectWorkflowRequestModel) o;
    return Objects.equals(this.name, postProjectWorkflowRequest.name) &&
        Objects.equals(this.description, postProjectWorkflowRequest.description) &&
        Objects.equals(this.definition, postProjectWorkflowRequest.definition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, definition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PostProjectWorkflowRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

