package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceIntegrationModel;
import com.bytechef.embedded.configuration.web.rest.model.IntegrationInstanceWorkflowModel;
import com.bytechef.embedded.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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

@Schema(name = "IntegrationInstance", description = "Contains configurations and connections required for the execution of integration workflows.")
@JsonTypeName("IntegrationInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-26T05:59:04.433309+01:00[Europe/Zagreb]")
public class IntegrationInstanceModel {

  private String description;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String name;

  private IntegrationInstanceIntegrationModel integration;

  private Long integrationId;

  @Valid
  private List<@Valid IntegrationInstanceWorkflowModel> integrationInstanceWorkflows;

  private Boolean enabled;

  @Valid
  private List<@Valid TagModel> tags;

  private Integer version;

  public IntegrationInstanceModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceModel(String name) {
    this.name = name;
  }

  public IntegrationInstanceModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a integration instance.
   * @return description
  */
  
  @Schema(name = "description", description = "The description of a integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IntegrationInstanceModel createdBy(String createdBy) {
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

  public IntegrationInstanceModel createdDate(LocalDateTime createdDate) {
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

  public IntegrationInstanceModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a integration instance.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationInstanceModel lastExecutionDate(LocalDateTime lastExecutionDate) {
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

  public IntegrationInstanceModel lastModifiedBy(String lastModifiedBy) {
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

  public IntegrationInstanceModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public IntegrationInstanceModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a integration instance.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The name of a integration instance.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public IntegrationInstanceModel integration(IntegrationInstanceIntegrationModel integration) {
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
  public IntegrationInstanceIntegrationModel getIntegration() {
    return integration;
  }

  public void setIntegration(IntegrationInstanceIntegrationModel integration) {
    this.integration = integration;
  }

  public IntegrationInstanceModel integrationId(Long integrationId) {
    this.integrationId = integrationId;
    return this;
  }

  /**
   * Th id of a integration.
   * @return integrationId
  */
  
  @Schema(name = "integrationId", description = "Th id of a integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationId")
  public Long getIntegrationId() {
    return integrationId;
  }

  public void setIntegrationId(Long integrationId) {
    this.integrationId = integrationId;
  }

  public IntegrationInstanceModel integrationInstanceWorkflows(List<@Valid IntegrationInstanceWorkflowModel> integrationInstanceWorkflows) {
    this.integrationInstanceWorkflows = integrationInstanceWorkflows;
    return this;
  }

  public IntegrationInstanceModel addIntegrationInstanceWorkflowsItem(IntegrationInstanceWorkflowModel integrationInstanceWorkflowsItem) {
    if (this.integrationInstanceWorkflows == null) {
      this.integrationInstanceWorkflows = new ArrayList<>();
    }
    this.integrationInstanceWorkflows.add(integrationInstanceWorkflowsItem);
    return this;
  }

  /**
   * Get integrationInstanceWorkflows
   * @return integrationInstanceWorkflows
  */
  @Valid 
  @Schema(name = "integrationInstanceWorkflows", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceWorkflows")
  public List<@Valid IntegrationInstanceWorkflowModel> getIntegrationInstanceWorkflows() {
    return integrationInstanceWorkflows;
  }

  public void setIntegrationInstanceWorkflows(List<@Valid IntegrationInstanceWorkflowModel> integrationInstanceWorkflows) {
    this.integrationInstanceWorkflows = integrationInstanceWorkflows;
  }

  public IntegrationInstanceModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If a workflow is enabled or not in the integration instance.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If a workflow is enabled or not in the integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public IntegrationInstanceModel addTagsItem(TagModel tagsItem) {
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

  public IntegrationInstanceModel version(Integer version) {
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
    IntegrationInstanceModel integrationInstance = (IntegrationInstanceModel) o;
    return Objects.equals(this.description, integrationInstance.description) &&
        Objects.equals(this.createdBy, integrationInstance.createdBy) &&
        Objects.equals(this.createdDate, integrationInstance.createdDate) &&
        Objects.equals(this.id, integrationInstance.id) &&
        Objects.equals(this.lastExecutionDate, integrationInstance.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, integrationInstance.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstance.lastModifiedDate) &&
        Objects.equals(this.name, integrationInstance.name) &&
        Objects.equals(this.integration, integrationInstance.integration) &&
        Objects.equals(this.integrationId, integrationInstance.integrationId) &&
        Objects.equals(this.integrationInstanceWorkflows, integrationInstance.integrationInstanceWorkflows) &&
        Objects.equals(this.enabled, integrationInstance.enabled) &&
        Objects.equals(this.tags, integrationInstance.tags) &&
        Objects.equals(this.version, integrationInstance.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, createdBy, createdDate, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, name, integration, integrationId, integrationInstanceWorkflows, enabled, tags, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    integration: ").append(toIndentedString(integration)).append("\n");
    sb.append("    integrationId: ").append(toIndentedString(integrationId)).append("\n");
    sb.append("    integrationInstanceWorkflows: ").append(toIndentedString(integrationInstanceWorkflows)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

