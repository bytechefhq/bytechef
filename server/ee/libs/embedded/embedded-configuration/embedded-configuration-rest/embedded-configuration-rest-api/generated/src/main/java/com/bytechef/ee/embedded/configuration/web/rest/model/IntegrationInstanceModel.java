package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceConfigurationBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationInstanceWorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.time.OffsetDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Contains configurations and connections required for the execution of integration workflows for a connected user.
 */

@Schema(name = "IntegrationInstance", description = "Contains configurations and connections required for the execution of integration workflows for a connected user.")
@JsonTypeName("IntegrationInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:35.556332+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class IntegrationInstanceModel {

  private Long connectionId;

  private @Nullable Long connectedUserId;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private @Nullable Boolean enabled;

  private @Nullable Long environmentId;

  private @Nullable Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastExecutionDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private @Nullable Long integrationInstanceConfigurationId;

  private @Nullable IntegrationInstanceConfigurationBasicModel integrationInstanceConfiguration;

  @Valid
  private List<@Valid IntegrationInstanceWorkflowModel> integrationInstanceWorkflows = new ArrayList<>();

  private @Nullable Integer version;

  public IntegrationInstanceModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceModel(Long connectionId) {
    this.connectionId = connectionId;
  }

  public IntegrationInstanceModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * The id of a connection.
   * @return connectionId
   */
  
  @Schema(name = "connectionId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public IntegrationInstanceModel connectedUserId(@Nullable Long connectedUserId) {
    this.connectedUserId = connectedUserId;
    return this;
  }

  /**
   * The id of a connected user.
   * @return connectedUserId
   */
  
  @Schema(name = "connectedUserId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectedUserId")
  public @Nullable Long getConnectedUserId() {
    return connectedUserId;
  }

  public void setConnectedUserId(@Nullable Long connectedUserId) {
    this.connectedUserId = connectedUserId;
  }

  public IntegrationInstanceModel createdBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
    return this;
  }

  /**
   * The created by.
   * @return createdBy
   */
  
  @Schema(name = "createdBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The created by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("createdBy")
  public @Nullable String getCreatedBy() {
    return createdBy;
  }

  public void setCreatedBy(@Nullable String createdBy) {
    this.createdBy = createdBy;
  }

  public IntegrationInstanceModel createdDate(@Nullable OffsetDateTime createdDate) {
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
  public @Nullable OffsetDateTime getCreatedDate() {
    return createdDate;
  }

  public void setCreatedDate(@Nullable OffsetDateTime createdDate) {
    this.createdDate = createdDate;
  }

  public IntegrationInstanceModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance is enabled or not.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "If an integration instance is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceModel environmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of an environment.
   * @return environmentId
   */
  
  @Schema(name = "environmentId", description = "The id of an environment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environmentId")
  public @Nullable Long getEnvironmentId() {
    return environmentId;
  }

  public void setEnvironmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
  }

  public IntegrationInstanceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceModel lastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
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
  public @Nullable OffsetDateTime getLastExecutionDate() {
    return lastExecutionDate;
  }

  public void setLastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
    this.lastExecutionDate = lastExecutionDate;
  }

  public IntegrationInstanceModel lastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
    return this;
  }

  /**
   * The last modified by.
   * @return lastModifiedBy
   */
  
  @Schema(name = "lastModifiedBy", accessMode = Schema.AccessMode.READ_ONLY, description = "The last modified by.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lastModifiedBy")
  public @Nullable String getLastModifiedBy() {
    return lastModifiedBy;
  }

  public void setLastModifiedBy(@Nullable String lastModifiedBy) {
    this.lastModifiedBy = lastModifiedBy;
  }

  public IntegrationInstanceModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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
  public @Nullable OffsetDateTime getLastModifiedDate() {
    return lastModifiedDate;
  }

  public void setLastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
    this.lastModifiedDate = lastModifiedDate;
  }

  public IntegrationInstanceModel integrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * Th id of an integration instance configuration.
   * @return integrationInstanceConfigurationId
   */
  
  @Schema(name = "integrationInstanceConfigurationId", description = "Th id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public @Nullable Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  public void setIntegrationInstanceConfigurationId(@Nullable Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public IntegrationInstanceModel integrationInstanceConfiguration(@Nullable IntegrationInstanceConfigurationBasicModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
    return this;
  }

  /**
   * Get integrationInstanceConfiguration
   * @return integrationInstanceConfiguration
   */
  @Valid 
  @Schema(name = "integrationInstanceConfiguration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfiguration")
  public @Nullable IntegrationInstanceConfigurationBasicModel getIntegrationInstanceConfiguration() {
    return integrationInstanceConfiguration;
  }

  public void setIntegrationInstanceConfiguration(@Nullable IntegrationInstanceConfigurationBasicModel integrationInstanceConfiguration) {
    this.integrationInstanceConfiguration = integrationInstanceConfiguration;
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
   * The array of integration instance workflows.
   * @return integrationInstanceWorkflows
   */
  @Valid 
  @Schema(name = "integrationInstanceWorkflows", accessMode = Schema.AccessMode.READ_ONLY, description = "The array of integration instance workflows.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceWorkflows")
  public List<@Valid IntegrationInstanceWorkflowModel> getIntegrationInstanceWorkflows() {
    return integrationInstanceWorkflows;
  }

  public void setIntegrationInstanceWorkflows(List<@Valid IntegrationInstanceWorkflowModel> integrationInstanceWorkflows) {
    this.integrationInstanceWorkflows = integrationInstanceWorkflows;
  }

  public IntegrationInstanceModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  public void setVersion(@Nullable Integer version) {
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
    return Objects.equals(this.connectionId, integrationInstance.connectionId) &&
        Objects.equals(this.connectedUserId, integrationInstance.connectedUserId) &&
        Objects.equals(this.createdBy, integrationInstance.createdBy) &&
        Objects.equals(this.createdDate, integrationInstance.createdDate) &&
        Objects.equals(this.enabled, integrationInstance.enabled) &&
        Objects.equals(this.environmentId, integrationInstance.environmentId) &&
        Objects.equals(this.id, integrationInstance.id) &&
        Objects.equals(this.lastExecutionDate, integrationInstance.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, integrationInstance.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstance.lastModifiedDate) &&
        Objects.equals(this.integrationInstanceConfigurationId, integrationInstance.integrationInstanceConfigurationId) &&
        Objects.equals(this.integrationInstanceConfiguration, integrationInstance.integrationInstanceConfiguration) &&
        Objects.equals(this.integrationInstanceWorkflows, integrationInstance.integrationInstanceWorkflows) &&
        Objects.equals(this.version, integrationInstance.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, connectedUserId, createdBy, createdDate, enabled, environmentId, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, integrationInstanceConfigurationId, integrationInstanceConfiguration, integrationInstanceWorkflows, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    connectedUserId: ").append(toIndentedString(connectedUserId)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    integrationInstanceConfigurationId: ").append(toIndentedString(integrationInstanceConfigurationId)).append("\n");
    sb.append("    integrationInstanceConfiguration: ").append(toIndentedString(integrationInstanceConfiguration)).append("\n");
    sb.append("    integrationInstanceWorkflows: ").append(toIndentedString(integrationInstanceWorkflows)).append("\n");
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

