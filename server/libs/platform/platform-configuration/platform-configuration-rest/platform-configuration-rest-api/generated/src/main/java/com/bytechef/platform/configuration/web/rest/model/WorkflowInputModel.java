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
 * WorkflowInputModel
 */

@JsonTypeName("WorkflowInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-27T14:10:00.953014+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class WorkflowInputModel {

  private @Nullable String label;

  private String name;

  private Boolean required = false;

  private String type = "string";

  public WorkflowInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowInputModel(String name) {
    this.name = name;
  }

  public WorkflowInputModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name of an input
   * @return label
   */
  
  @Schema(name = "label", description = "The descriptive name of an input", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public WorkflowInputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an input
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an input", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public WorkflowInputModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If an input is required, or not
   * @return required
   */
  
  @Schema(name = "required", description = "If an input is required, or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  @JsonProperty("required")
  public void setRequired(Boolean required) {
    this.required = required;
  }

  public WorkflowInputModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of an input, for example \\\"string\\\"
   * @return type
   */
  
  @Schema(name = "type", description = "The type of an input, for example \\\"string\\\"", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(String type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowInputModel workflowInput = (WorkflowInputModel) o;
    return Objects.equals(this.label, workflowInput.label) &&
        Objects.equals(this.name, workflowInput.name) &&
        Objects.equals(this.required, workflowInput.required) &&
        Objects.equals(this.type, workflowInput.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label, name, required, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowInputModel {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

