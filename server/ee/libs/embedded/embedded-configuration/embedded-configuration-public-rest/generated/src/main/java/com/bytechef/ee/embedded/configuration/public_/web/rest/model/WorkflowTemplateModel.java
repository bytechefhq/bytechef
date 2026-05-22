package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.WorkflowTemplateComponentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A workflow template available to connected users.
 */

@Schema(name = "WorkflowTemplate", description = "A workflow template available to connected users.")
@JsonTypeName("WorkflowTemplate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-05-22T10:23:02.305542+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class WorkflowTemplateModel {

  private @Nullable String id;

  private @Nullable String label;

  private @Nullable String description;

  @Valid
  private List<@Valid WorkflowTemplateComponentModel> components = new ArrayList<>();

  public WorkflowTemplateModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The template workflow UUID.
   * @return id
   */
  
  @Schema(name = "id", description = "The template workflow UUID.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable String id) {
    this.id = id;
  }

  public WorkflowTemplateModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  
  @Schema(name = "label", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public WorkflowTemplateModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public WorkflowTemplateModel components(List<@Valid WorkflowTemplateComponentModel> components) {
    this.components = components;
    return this;
  }

  public WorkflowTemplateModel addComponentsItem(WorkflowTemplateComponentModel componentsItem) {
    if (this.components == null) {
      this.components = new ArrayList<>();
    }
    this.components.add(componentsItem);
    return this;
  }

  /**
   * Get components
   * @return components
   */
  @Valid 
  @Schema(name = "components", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("components")
  public List<@Valid WorkflowTemplateComponentModel> getComponents() {
    return components;
  }

  @JsonProperty("components")
  public void setComponents(List<@Valid WorkflowTemplateComponentModel> components) {
    this.components = components;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTemplateModel workflowTemplate = (WorkflowTemplateModel) o;
    return Objects.equals(this.id, workflowTemplate.id) &&
        Objects.equals(this.label, workflowTemplate.label) &&
        Objects.equals(this.description, workflowTemplate.description) &&
        Objects.equals(this.components, workflowTemplate.components);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label, description, components);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTemplateModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    components: ").append(toIndentedString(components)).append("\n");
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

