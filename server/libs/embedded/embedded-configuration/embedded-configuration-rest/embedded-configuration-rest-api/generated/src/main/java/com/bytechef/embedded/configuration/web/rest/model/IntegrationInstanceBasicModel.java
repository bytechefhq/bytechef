package com.bytechef.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.embedded.configuration.web.rest.model.EnvironmentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.LocalDateTime;
import org.springframework.format.annotation.DateTimeFormat;
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

@Schema(name = "IntegrationInstanceBasic", description = "Contains configurations and connections required for the execution of integration workflows for a connected user.")
@JsonTypeName("IntegrationInstanceBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-12T12:40:28.680343+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class IntegrationInstanceBasicModel {

  private Long connectionId;

  private Long connectedUserId;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Boolean enabled;

  private EnvironmentModel environment;

  private Long id;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private Long integrationInstanceConfigurationId;

  public IntegrationInstanceBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceBasicModel(Long connectionId) {
    this.connectionId = connectionId;
  }

  public IntegrationInstanceBasicModel connectionId(Long connectionId) {
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

  public IntegrationInstanceBasicModel connectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
    return this;
  }

  /**
   * The id of a connected user.
   * @return connectedUserId
  */
  
  @Schema(name = "connectedUserId", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of a connected user.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectedUserId")
  public Long getConnectedUserId() {
    return connectedUserId;
  }

  public void setConnectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
  }

  public IntegrationInstanceBasicModel createdBy(String createdBy) {
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

  public IntegrationInstanceBasicModel createdDate(LocalDateTime createdDate) {
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

  public IntegrationInstanceBasicModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance is enabled or not.
   * @return enabled
  */
  
  @Schema(name = "enabled", description = "If an integration instance is enabled or not.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceBasicModel environment(EnvironmentModel environment) {
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

  public IntegrationInstanceBasicModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
  */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationInstanceBasicModel lastExecutionDate(LocalDateTime lastExecutionDate) {
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

  public IntegrationInstanceBasicModel lastModifiedBy(String lastModifiedBy) {
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

  public IntegrationInstanceBasicModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public IntegrationInstanceBasicModel integrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * Th id of an integration instance configuration.
   * @return integrationInstanceConfigurationId
  */
  
  @Schema(name = "integrationInstanceConfigurationId", description = "Th id of an integration instance configuration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  public void setIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationInstanceBasicModel integrationInstanceBasic = (IntegrationInstanceBasicModel) o;
    return Objects.equals(this.connectionId, integrationInstanceBasic.connectionId) &&
        Objects.equals(this.connectedUserId, integrationInstanceBasic.connectedUserId) &&
        Objects.equals(this.createdBy, integrationInstanceBasic.createdBy) &&
        Objects.equals(this.createdDate, integrationInstanceBasic.createdDate) &&
        Objects.equals(this.enabled, integrationInstanceBasic.enabled) &&
        Objects.equals(this.environment, integrationInstanceBasic.environment) &&
        Objects.equals(this.id, integrationInstanceBasic.id) &&
        Objects.equals(this.lastExecutionDate, integrationInstanceBasic.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, integrationInstanceBasic.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, integrationInstanceBasic.lastModifiedDate) &&
        Objects.equals(this.integrationInstanceConfigurationId, integrationInstanceBasic.integrationInstanceConfigurationId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, connectedUserId, createdBy, createdDate, enabled, environment, id, lastExecutionDate, lastModifiedBy, lastModifiedDate, integrationInstanceConfigurationId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceBasicModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    connectedUserId: ").append(toIndentedString(connectedUserId)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    integrationInstanceConfigurationId: ").append(toIndentedString(integrationInstanceConfigurationId)).append("\n");
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

