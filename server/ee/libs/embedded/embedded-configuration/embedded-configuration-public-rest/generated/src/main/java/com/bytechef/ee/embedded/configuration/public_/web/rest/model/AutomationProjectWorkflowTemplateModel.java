package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectComponentModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectWorkflowInputModel;
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
 * An automation workflow template within an automation project.
 */

@Schema(name = "AutomationProjectWorkflowTemplate", description = "An automation workflow template within an automation project.")
@JsonTypeName("AutomationProjectWorkflowTemplate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-10T16:12:12.795950+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AutomationProjectWorkflowTemplateModel {

  private @Nullable String id;

  private @Nullable String label;

  private @Nullable String description;

  private List<@Valid AutomationProjectComponentModel> components = new ArrayList<>();

  private List<@Valid AutomationProjectWorkflowInputModel> inputs = new ArrayList<>();

  public AutomationProjectWorkflowTemplateModel id(@Nullable String id) {
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

  public AutomationProjectWorkflowTemplateModel label(@Nullable String label) {
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

  public AutomationProjectWorkflowTemplateModel description(@Nullable String description) {
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

  public AutomationProjectWorkflowTemplateModel components(List<@Valid AutomationProjectComponentModel> components) {
    this.components = components;
    return this;
  }

  public AutomationProjectWorkflowTemplateModel addComponentsItem(AutomationProjectComponentModel componentsItem) {
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
  
  @Schema(name = "components", description = "The list of components used in this workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("components")
  public List<@Valid AutomationProjectComponentModel> getComponents() {
    return components;
  }

  @JsonProperty("components")
  public void setComponents(List<@Valid AutomationProjectComponentModel> components) {
    this.components = components;
  }

  public AutomationProjectWorkflowTemplateModel inputs(List<@Valid AutomationProjectWorkflowInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public AutomationProjectWorkflowTemplateModel addInputsItem(AutomationProjectWorkflowInputModel inputsItem) {
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
  
  @Schema(name = "inputs", description = "The values the connected user is asked for before this workflow runs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid AutomationProjectWorkflowInputModel> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(List<@Valid AutomationProjectWorkflowInputModel> inputs) {
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
    AutomationProjectWorkflowTemplateModel automationProjectWorkflowTemplate = (AutomationProjectWorkflowTemplateModel) o;
    return Objects.equals(this.id, automationProjectWorkflowTemplate.id) &&
        Objects.equals(this.label, automationProjectWorkflowTemplate.label) &&
        Objects.equals(this.description, automationProjectWorkflowTemplate.description) &&
        Objects.equals(this.components, automationProjectWorkflowTemplate.components) &&
        Objects.equals(this.inputs, automationProjectWorkflowTemplate.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, label, description, components, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationProjectWorkflowTemplateModel {\n");
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

