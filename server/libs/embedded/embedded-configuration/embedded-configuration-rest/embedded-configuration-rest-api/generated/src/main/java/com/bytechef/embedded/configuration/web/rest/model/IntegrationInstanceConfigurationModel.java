package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationAllOfIntegrationModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationWorkflowModel;
import com.bytechef.embedded.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains configurations and connections required for the execution of integration workflows.
 */

@Schema(name = "IntegrationInstanceConfiguration", description = "Contains configurations and connections required for the execution of integration workflows.")
@JsonTypeName("IntegrationInstanceConfiguration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-03-19T18:35:35.986779+01:00[Europe/Zagreb]")
public class IntegrationInstanceConfigurationModel {

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private String description;

  private EnvironmentModel environment;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Long integrationId;

  private Boolean enabled;

  private IntegrationInstanceConfigurationAllOfIntegrationModel integration;

  @Valid
  private List<@Valid IntegrationInstanceConfigurationWorkflowModel> integrationInstanceConfigurationWorkflows;

  @Valid
  private List<@Valid TagModel> tags;

  private Integer version;

  public IntegrationInstanceConfigurationModel createdBy(String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
  */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(String createdBy) {
    this.createdBy = createdBy;
  }

  public IntegrationInstanceConfigurationModel createdDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
    return this;
  }

  /**
   * The created date.
   * @return createdDate
  */
  @Valid 
  @Schema(name = "createdDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The created date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdDate")
  public LocalDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(LocalDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public IntegrationInstanceConfigurationModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IntegrationInstanceConfigurationModel environment(EnvironmentModel environment) {
    this.environment = environment;
    return this;
  }

  /**
   * Get environment
   * @return environment
  */
  @Valid 
  @Schema(name = "environment", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environment")
  public EnvironmentModel getEnvironment() {
    return environment;
  }

  public void setEnvironment(EnvironmentModel environment) {
    this.environment = environment;
  }

  public IntegrationInstanceConfigurationModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance configuration.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationInstanceConfigurationModel lastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
    return this;
  }

  /**
   * The last execution date.
   * @return lastExecutionDate
  */
  @Valid 
  @Schema(name = "lastExecutionDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last execution date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastExecutionDate")
  public LocalDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(LocalDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public IntegrationInstanceConfigurationModel lastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
  */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public IntegrationInstanceConfigurationModel lastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
    return this;
  }

  /**
   * The last modified date.
   * @return lastModifiedDate
  */
  @Valid 
  @Schema(name = "lastModifiedDate", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified date.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedDate")
  public LocalDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(LocalDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public IntegrationInstanceConfigurationModel integrationId(Long integrationId) {
    this.integrationId = integrationId;
    return this;
  }

  /**
   * Th id of an integration.
   * @return integrationId
  */
  
  @Schema(name = "integrationId", description = "Th id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationId")
  public Long getIntegrationId() {
    return integrationId;
  }

  public void setIntegrationId(Long integrationId) {
    this.integrationId = integrationId;
  }

  public IntegrationInstanceConfigurationModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance configuration is enabled or not.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If an integration instance configuration is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceConfigurationModel integration(IntegrationInstanceConfigurationAllOfIntegrationModel integration) {
    this.integration = integration;
    return this;
  }

  /**
   * Get integration
   * @return integration
  */
  @Valid 
  @Schema(name = "integration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integration")
  public IntegrationInstanceConfigurationAllOfIntegrationModel getIntegration() {
    return integration;
  }

  public void setIntegration(IntegrationInstanceConfigurationAllOfIntegrationModel integration) {
    this.integration = integration;
  }

  public IntegrationInstanceConfigurationModel integrationInstanceConfigurationWorkflows(List<@Valid IntegrationInstanceConfigurationWorkflowModel> integrationInstanceConfigurationWorkflows) {
    this.integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflows;
    return this;
  }

  public IntegrationInstanceConfigurationModel addIntegrationInstanceConfigurationWorkflowsItem(IntegrationInstanceConfigurationWorkflowModel integrationInstanceConfigurationWorkflowsItem) {
    if (this.integrationInstanceConfigurationWorkflows == null) {
      this.integrationInstanceConfigurationWorkflows = new ArrayList<>();
    }
    this.integrationInstanceConfigurationWorkflows.add(integrationInstanceConfigurationWorkflowsItem);
    return this;
  }

  /**
   * Get integrationInstanceConfigurationWorkflows
   * @return integrationInstanceConfigurationWorkflows
  */
  @Valid 
  @Schema(name = "integrationInstanceConfigurationWorkflows", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationWorkflows")
  public List<@Valid IntegrationInstanceConfigurationWorkflowModel> getIntegrationInstanceConfigurationWorkflows() {
    return integrationInstanceConfigurationWorkflows;
  }

  public void setIntegrationInstanceConfigurationWorkflows(List<@Valid IntegrationInstanceConfigurationWorkflowModel> integrationInstanceConfigurationWorkflows) {
    this.integrationInstanceConfigurationWorkflows = integrationInstanceConfigurationWorkflows;
  }

  public IntegrationInstanceConfigurationModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public IntegrationInstanceConfigurationModel addTagsItem(TagModel tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
  */
  @Valid 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public IntegrationInstanceConfigurationModel version(Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
  */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public Integer getVersion() {
    return version;
  }

  public void setVersion(Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceConfigurationModel integrationInstanceConfiguration = (IntegrationInstanceConfigurationModel) o;
    return Objects.equals(this.createdBy, integrationInstanceConfiguration.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceConfiguration.createdDate) &&
        Objects.equals(this.description, integrationInstanceConfiguration.description) &&
        Objects.equals(this.environment, integrationInstanceConfiguration.environment) &&
        Objects.equals(this.id, integrationInstanceConfiguration.id) &&
        Objects.equals(this.lastExecutionDate, integrationInstanceConfiguration.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceConfiguration.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceConfiguration.lastModifiedDate) &&
        Objects.equals(this.integrationId, integrationInstanceConfiguration.integrationId) &&
        Objects.equals(this.enabled, integrationInstanceConfiguration.enabled) &&
        Objects.equals(this.integration, integrationInstanceConfiguration.integration) &&
        Objects.equals(this.integrationInstanceConfigurationWorkflows, integrationInstanceConfiguration.integrationInstanceConfigurationWorkflows) &&
        Objects.equals(this.tags, integrationInstanceConfiguration.tags) &&
        Objects.equals(this.version, integrationInstanceConfiguration.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(createdBy, createdDate, description, environment, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, integrationId, enabled, integration, integrationInstanceConfigurationWorkflows, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceConfigurationModel {\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    integrationId: ").append(toIndentedString(integrationId)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    integration: ").append(toIndentedString(integration)).append("\n");
    sb.append("    integrationInstanceConfigurationWorkflows: ").append(toIndentedString(integrationInstanceConfigurationWorkflows)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

