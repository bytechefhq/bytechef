package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectComponentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationWorkflowProjectWorkflowInputModel;
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
 * An automation workflow template within an automation workflow project.
 */

@Schema(name = "AutomationWorkflowProjectWorkflowTemplate", description = "An automation workflow template within an automation workflow project.")
@JsonTypeName("AutomationWorkflowProjectWorkflowTemplate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-23T21:34:31.743535+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AutomationWorkflowProjectWorkflowTemplateModel {

  private @Nullable String id;

  private @Nullable String label;

  private @Nullable String description;

  @Valid
  private List<@Valid AutomationWorkflowProjectComponentModel> components = new ArrayList<>();

  @Valid
  private List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs = new ArrayList<>();

  public AutomationWorkflowProjectWorkflowTemplateModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * The workflow UUID.
   * @return id
   */
  
  @Schema(name = "id", description = "The workflow UUID.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable String id) {
    this.id = id;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of the workflow.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the workflow.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel components(List<@Valid AutomationWorkflowProjectComponentModel> components) {
    this.components = components;
    return this;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel addComponentsItem(AutomationWorkflowProjectComponentModel componentsItem) {
    if (this.components == null) {
      this.components = new ArrayList<>();
    }
    this.components.add(componentsItem);
    return this;
  }

  /**
   * The list of components used in this workflow.
   * @return components
   */
  @Valid 
  @Schema(name = "components", description = "The list of components used in this workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("components")
  public List<@Valid AutomationWorkflowProjectComponentModel> getComponents() {
    return components;
  }

  @JsonProperty("components")
  public void setComponents(List<@Valid AutomationWorkflowProjectComponentModel> components) {
    this.components = components;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel inputs(List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public AutomationWorkflowProjectWorkflowTemplateModel addInputsItem(AutomationWorkflowProjectWorkflowInputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * The values the connected user is asked for before this workflow runs.
   * @return inputs
   */
  @Valid 
  @Schema(name = "inputs", description = "The values the connected user is asked for before this workflow runs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid AutomationWorkflowProjectWorkflowInputModel> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(List<@Valid AutomationWorkflowProjectWorkflowInputModel> inputs) {
    this.inputs = inputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationWorkflowProjectWorkflowTemplateModel automationWorkflowProjectWorkflowTemplate = (AutomationWorkflowProjectWorkflowTemplateModel) o;
    return Objects.equals(this.id, automationWorkflowProjectWorkflowTemplate.id) &&
        Objects.equals(this.label, automationWorkflowProjectWorkflowTemplate.label) &&
        Objects.equals(this.description, automationWorkflowProjectWorkflowTemplate.description) &&
        Objects.equals(this.components, automationWorkflowProjectWorkflowTemplate.components) &&
        Objects.equals(this.inputs, automationWorkflowProjectWorkflowTemplate.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label, description, components, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowProjectWorkflowTemplateModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    components: ").append(toIndentedString(components)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
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

