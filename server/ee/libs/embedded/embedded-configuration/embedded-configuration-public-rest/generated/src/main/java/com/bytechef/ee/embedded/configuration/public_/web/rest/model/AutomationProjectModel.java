package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.AutomationProjectWorkflowTemplateModel;
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
 * An automation workflow automation project.
 */

@Schema(name = "AutomationProject", description = "An automation workflow automation project.")
@JsonTypeName("AutomationProject")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-10T16:12:12.795950+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class AutomationProjectModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable String description;

  private @Nullable Boolean automationHubVisible;

  private List<@Valid AutomationProjectWorkflowTemplateModel> workflowTemplates = new ArrayList<>();

  public AutomationProjectModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of the automation project.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of the automation project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public AutomationProjectModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of the automation project.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of the automation project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public AutomationProjectModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of the automation project.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of the automation project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AutomationProjectModel automationHubVisible(@Nullable Boolean automationHubVisible) {
    this.automationHubVisible = automationHubVisible;
    return this;
  }

  /**
   * Whether the project is listed in the Automation Hub. The project listing leaves out hidden projects; a workflow of a hidden project is still available by its workflow uuid.
   * @return automationHubVisible
   */
  
  @Schema(name = "automationHubVisible", description = "Whether the project is listed in the Automation Hub. The project listing leaves out hidden projects; a workflow of a hidden project is still available by its workflow uuid.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("automationHubVisible")
  public @Nullable Boolean getAutomationHubVisible() {
    return automationHubVisible;
  }

  @JsonProperty("automationHubVisible")
  public void setAutomationHubVisible(@Nullable Boolean automationHubVisible) {
    this.automationHubVisible = automationHubVisible;
  }

  public AutomationProjectModel workflowTemplates(List<@Valid AutomationProjectWorkflowTemplateModel> workflowTemplates) {
    this.workflowTemplates = workflowTemplates;
    return this;
  }

  public AutomationProjectModel addWorkflowTemplatesItem(AutomationProjectWorkflowTemplateModel workflowTemplatesItem) {
    if (this.workflowTemplates == null) {
      this.workflowTemplates = new ArrayList<>();
    }
    this.workflowTemplates.add(workflowTemplatesItem);
    return this;
  }

  /**
   * The list of automation workflow templates belonging to this project.
   * @return workflowTemplates
   */
  
  @Schema(name = "workflowTemplates", description = "The list of automation workflow templates belonging to this project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflowTemplates")
  public List<@Valid AutomationProjectWorkflowTemplateModel> getWorkflowTemplates() {
    return workflowTemplates;
  }

  @JsonProperty("workflowTemplates")
  public void setWorkflowTemplates(List<@Valid AutomationProjectWorkflowTemplateModel> workflowTemplates) {
    this.workflowTemplates = workflowTemplates;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationProjectModel automationProject = (AutomationProjectModel) o;
    return Objects.equals(this.id, automationProject.id) &&
        Objects.equals(this.name, automationProject.name) &&
        Objects.equals(this.description, automationProject.description) &&
        Objects.equals(this.automationHubVisible, automationProject.automationHubVisible) &&
        Objects.equals(this.workflowTemplates, automationProject.workflowTemplates);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, description, automationHubVisible, workflowTemplates);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationProjectModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    automationHubVisible: ").append(toIndentedString(automationHubVisible)).append("\n");
    sb.append("    workflowTemplates: ").append(toIndentedString(workflowTemplates)).append("\n");
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

