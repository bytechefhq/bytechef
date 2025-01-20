package com.bytechef.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.public_.web.rest.model.WorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A group of workflows that make one logical integration.
 */

@Schema(name = "Integration", description = "A group of workflows that make one logical integration.")
@JsonTypeName("Integration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-20T07:11:57.734213+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class IntegrationModel {

  private Long id;

  private String componentName;

  private String title;

  private String description;

  private String icon;

  private Integer integrationVersion;

  private Boolean allowMultipleInstances = false;

  @Valid
  private List<@Valid WorkflowModel> workflows = new ArrayList<>();

  public IntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationModel(String componentName, String icon, Boolean allowMultipleInstances) {
    this.componentName = componentName;
    this.icon = icon;
    this.allowMultipleInstances = allowMultipleInstances;
  }

  public IntegrationModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the integration's component.
   * @return componentName
   */
  
  @Schema(name = "componentName", accessMode = Schema.AccessMode.READ_ONLY, description = "The name of the integration's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public IntegrationModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of the integration's component.
   * @return title
   */
  
  @Schema(name = "title", accessMode = Schema.AccessMode.READ_ONLY, description = "The title of the integration's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  public IntegrationModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
   */
  
  @Schema(name = "description", accessMode = Schema.AccessMode.READ_ONLY, description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IntegrationModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
   */
  
  @Schema(name = "icon", accessMode = Schema.AccessMode.READ_ONLY, description = "The icon.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public IntegrationModel integrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
   */
  
  @Schema(name = "integrationVersion", accessMode = Schema.AccessMode.READ_ONLY, description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationModel allowMultipleInstances(Boolean allowMultipleInstances) {
    this.allowMultipleInstances = allowMultipleInstances;
    return this;
  }

  /**
   * If multiple instances of an integration are allowed or not.
   * @return allowMultipleInstances
   */
  
  @Schema(name = "allowMultipleInstances", accessMode = Schema.AccessMode.READ_ONLY, description = "If multiple instances of an integration are allowed or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("allowMultipleInstances")
  public Boolean getAllowMultipleInstances() {
    return allowMultipleInstances;
  }

  public void setAllowMultipleInstances(Boolean allowMultipleInstances) {
    this.allowMultipleInstances = allowMultipleInstances;
  }

  public IntegrationModel workflows(List<@Valid WorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public IntegrationModel addWorkflowsItem(WorkflowModel workflowsItem) {
    if (this.workflows == null) {
      this.workflows = new ArrayList<>();
    }
    this.workflows.add(workflowsItem);
    return this;
  }

  /**
   * The list of workflows.
   * @return workflows
   */
  @Valid 
  @Schema(name = "workflows", description = "The list of workflows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflows")
  public List<@Valid WorkflowModel> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<@Valid WorkflowModel> workflows) {
    this.workflows = workflows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationModel integration = (IntegrationModel) o;
    return Objects.equals(this.id, integration.id) &&
        Objects.equals(this.componentName, integration.componentName) &&
        Objects.equals(this.title, integration.title) &&
        Objects.equals(this.description, integration.description) &&
        Objects.equals(this.icon, integration.icon) &&
        Objects.equals(this.integrationVersion, integration.integrationVersion) &&
        Objects.equals(this.allowMultipleInstances, integration.allowMultipleInstances) &&
        Objects.equals(this.workflows, integration.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, componentName, title, description, icon, integrationVersion, allowMultipleInstances, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    allowMultipleInstances: ").append(toIndentedString(allowMultipleInstances)).append("\n");
    sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
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

