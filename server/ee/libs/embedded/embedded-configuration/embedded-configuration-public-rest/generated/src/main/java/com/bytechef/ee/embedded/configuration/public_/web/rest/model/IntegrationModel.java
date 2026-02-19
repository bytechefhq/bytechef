package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ConnectionConfigModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationWorkflowModel;
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
 * A group of workflows that make one logical integration for a particular service represented by component.
 */

@Schema(name = "Integration", description = "A group of workflows that make one logical integration for a particular service represented by component.")
@JsonTypeName("Integration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-02T08:44:56.374523+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class IntegrationModel {

  private String componentName;

  private @Nullable String description;

  private String icon;

  private @Nullable Long id;

  @Valid
  private List<@Valid IntegrationInstanceModel> integrationInstances = new ArrayList<>();

  private @Nullable Integer integrationVersion;

  private Boolean multipleInstances = false;

  private @Nullable String name;

  private @Nullable ConnectionConfigModel connectionConfig;

  @Valid
  private List<@Valid IntegrationWorkflowModel> workflows = new ArrayList<>();

  public IntegrationModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationModel(String componentName, String icon, Boolean multipleInstances) {
    this.componentName = componentName;
    this.icon = icon;
    this.multipleInstances = multipleInstances;
  }

  public IntegrationModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the integration's component.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the integration's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public IntegrationModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
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
  @NotNull 
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public IntegrationModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationModel integrationInstances(List<@Valid IntegrationInstanceModel> integrationInstances) {
    this.integrationInstances = integrationInstances;
    return this;
  }

  public IntegrationModel addIntegrationInstancesItem(IntegrationInstanceModel integrationInstancesItem) {
    if (this.integrationInstances == null) {
      this.integrationInstances = new ArrayList<>();
    }
    this.integrationInstances.add(integrationInstancesItem);
    return this;
  }

  /**
   * The list of integration instances that represent configured and connected integrations for specific users
   * @return integrationInstances
   */
  @Valid 
  @Schema(name = "integrationInstances", description = "The list of integration instances that represent configured and connected integrations for specific users", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstances")
  public List<@Valid IntegrationInstanceModel> getIntegrationInstances() {
    return integrationInstances;
  }

  public void setIntegrationInstances(List<@Valid IntegrationInstanceModel> integrationInstances) {
    this.integrationInstances = integrationInstances;
  }

  public IntegrationModel integrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
   */
  
  @Schema(name = "integrationVersion", description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public @Nullable Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(@Nullable Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationModel multipleInstances(Boolean multipleInstances) {
    this.multipleInstances = multipleInstances;
    return this;
  }

  /**
   * If multiple instances of an integration are allowed or not.
   * @return multipleInstances
   */
  @NotNull 
  @Schema(name = "multipleInstances", description = "If multiple instances of an integration are allowed or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("multipleInstances")
  public Boolean getMultipleInstances() {
    return multipleInstances;
  }

  public void setMultipleInstances(Boolean multipleInstances) {
    this.multipleInstances = multipleInstances;
  }

  public IntegrationModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an integration.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public IntegrationModel connectionConfig(@Nullable ConnectionConfigModel connectionConfig) {
    this.connectionConfig = connectionConfig;
    return this;
  }

  /**
   * Get connectionConfig
   * @return connectionConfig
   */
  @Valid 
  @Schema(name = "connectionConfig", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionConfig")
  public @Nullable ConnectionConfigModel getConnectionConfig() {
    return connectionConfig;
  }

  public void setConnectionConfig(@Nullable ConnectionConfigModel connectionConfig) {
    this.connectionConfig = connectionConfig;
  }

  public IntegrationModel workflows(List<@Valid IntegrationWorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public IntegrationModel addWorkflowsItem(IntegrationWorkflowModel workflowsItem) {
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
  public List<@Valid IntegrationWorkflowModel> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<@Valid IntegrationWorkflowModel> workflows) {
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
    return Objects.equals(this.componentName, integration.componentName) &&
        Objects.equals(this.description, integration.description) &&
        Objects.equals(this.icon, integration.icon) &&
        Objects.equals(this.id, integration.id) &&
        Objects.equals(this.integrationInstances, integration.integrationInstances) &&
        Objects.equals(this.integrationVersion, integration.integrationVersion) &&
        Objects.equals(this.multipleInstances, integration.multipleInstances) &&
        Objects.equals(this.name, integration.name) &&
        Objects.equals(this.connectionConfig, integration.connectionConfig) &&
        Objects.equals(this.workflows, integration.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, description, icon, id, integrationInstances, integrationVersion, multipleInstances, name, connectionConfig, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationInstances: ").append(toIndentedString(integrationInstances)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    multipleInstances: ").append(toIndentedString(multipleInstances)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    connectionConfig: ").append(toIndentedString(connectionConfig)).append("\n");
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

