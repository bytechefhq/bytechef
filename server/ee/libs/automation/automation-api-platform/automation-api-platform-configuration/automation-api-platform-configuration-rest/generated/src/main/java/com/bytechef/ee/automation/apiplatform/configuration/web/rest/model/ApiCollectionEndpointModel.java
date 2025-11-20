package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.HttpMethodModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.time.OffsetDateTime;
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
 * An API collection endpoint.
 */

@Schema(name = "ApiCollectionEndpoint", description = "An API collection endpoint.")
@JsonTypeName("ApiCollectionEndpoint")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:35.285159+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ApiCollectionEndpointModel {

  private @Nullable Long apiCollectionId;

  private @Nullable String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime createdDate;

  private Boolean enabled = false;

  private HttpMethodModel httpMethod;

  private @Nullable Long id;

  private @Nullable String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastExecutionDate;

  private @Nullable String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private @Nullable OffsetDateTime lastModifiedDate;

  private String path;

  private @Nullable Long projectDeploymentWorkflowId;

  private String workflowUuid;

  private @Nullable Integer version;

  public ApiCollectionEndpointModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionEndpointModel(Boolean enabled, HttpMethodModel httpMethod, String path, String workflowUuid) {
    this.enabled = enabled;
    this.httpMethod = httpMethod;
    this.path = path;
    this.workflowUuid = workflowUuid;
  }

  public ApiCollectionEndpointModel apiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
    return this;
  }

  /**
   * The id of an API collection.
   * @return apiCollectionId
   */
  
  @Schema(name = "apiCollectionId", description = "The id of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiCollectionId")
  public @Nullable Long getApiCollectionId() {
    return apiCollectionId;
  }

  public void setApiCollectionId(@Nullable Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
  }

  public ApiCollectionEndpointModel createdBy(@Nullable String createdBy) {
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

  public ApiCollectionEndpointModel createdDate(@Nullable OffsetDateTime createdDate) {
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

  public ApiCollectionEndpointModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an API collection is enabled or not.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an API collection is enabled or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public ApiCollectionEndpointModel httpMethod(HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
    return this;
  }

  /**
   * Get httpMethod
   * @return httpMethod
   */
  @NotNull @Valid 
  @Schema(name = "httpMethod", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("httpMethod")
  public HttpMethodModel getHttpMethod() {
    return httpMethod;
  }

  public void setHttpMethod(HttpMethodModel httpMethod) {
    this.httpMethod = httpMethod;
  }

  public ApiCollectionEndpointModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an API collection.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ApiCollectionEndpointModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API collection.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ApiCollectionEndpointModel lastExecutionDate(@Nullable OffsetDateTime lastExecutionDate) {
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

  public ApiCollectionEndpointModel lastModifiedBy(@Nullable String lastModifiedBy) {
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

  public ApiCollectionEndpointModel lastModifiedDate(@Nullable OffsetDateTime lastModifiedDate) {
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

  public ApiCollectionEndpointModel path(String path) {
    this.path = path;
    return this;
  }

  /**
   * The endpoint path.
   * @return path
   */
  @NotNull 
  @Schema(name = "path", description = "The endpoint path.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  public ApiCollectionEndpointModel projectDeploymentWorkflowId(@Nullable Long projectDeploymentWorkflowId) {
    this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
    return this;
  }

  /**
   * The project deployment workflow id.
   * @return projectDeploymentWorkflowId
   */
  
  @Schema(name = "projectDeploymentWorkflowId", accessMode = Schema.AccessMode.READ_ONLY, description = "The project deployment workflow id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectDeploymentWorkflowId")
  public @Nullable Long getProjectDeploymentWorkflowId() {
    return projectDeploymentWorkflowId;
  }

  public void setProjectDeploymentWorkflowId(@Nullable Long projectDeploymentWorkflowId) {
    this.projectDeploymentWorkflowId = projectDeploymentWorkflowId;
  }

  public ApiCollectionEndpointModel workflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
    return this;
  }

  /**
   * The workflow uuid.
   * @return workflowUuid
   */
  @NotNull 
  @Schema(name = "workflowUuid", description = "The workflow uuid.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowUuid")
  public String getWorkflowUuid() {
    return workflowUuid;
  }

  public void setWorkflowUuid(String workflowUuid) {
    this.workflowUuid = workflowUuid;
  }

  public ApiCollectionEndpointModel version(@Nullable Integer version) {
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
    ApiCollectionEndpointModel apiCollectionEndpoint = (ApiCollectionEndpointModel) o;
    return Objects.equals(this.apiCollectionId, apiCollectionEndpoint.apiCollectionId) &&
        Objects.equals(this.createdBy, apiCollectionEndpoint.createdBy) &&
        Objects.equals(this.createdDate, apiCollectionEndpoint.createdDate) &&
        Objects.equals(this.enabled, apiCollectionEndpoint.enabled) &&
        Objects.equals(this.httpMethod, apiCollectionEndpoint.httpMethod) &&
        Objects.equals(this.id, apiCollectionEndpoint.id) &&
        Objects.equals(this.name, apiCollectionEndpoint.name) &&
        Objects.equals(this.lastExecutionDate, apiCollectionEndpoint.lastExecutionDate) &&
        Objects.equals(this.lastModifiedBy, apiCollectionEndpoint.lastModifiedBy) &&
        Objects.equals(this.lastModifiedDate, apiCollectionEndpoint.lastModifiedDate) &&
        Objects.equals(this.path, apiCollectionEndpoint.path) &&
        Objects.equals(this.projectDeploymentWorkflowId, apiCollectionEndpoint.projectDeploymentWorkflowId) &&
        Objects.equals(this.workflowUuid, apiCollectionEndpoint.workflowUuid) &&
        Objects.equals(this.version, apiCollectionEndpoint.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiCollectionId, createdBy, createdDate, enabled, httpMethod, id, name, lastExecutionDate, lastModifiedBy, lastModifiedDate, path, projectDeploymentWorkflowId, workflowUuid, version);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionEndpointModel {\n");
    sb.append("    apiCollectionId: ").append(toIndentedString(apiCollectionId)).append("\n");
    sb.append("    createdBy: ").append(toIndentedString(createdBy)).append("\n");
    sb.append("    createdDate: ").append(toIndentedString(createdDate)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    httpMethod: ").append(toIndentedString(httpMethod)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    lastExecutionDate: ").append(toIndentedString(lastExecutionDate)).append("\n");
    sb.append("    lastModifiedBy: ").append(toIndentedString(lastModifiedBy)).append("\n");
    sb.append("    lastModifiedDate: ").append(toIndentedString(lastModifiedDate)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    projectDeploymentWorkflowId: ").append(toIndentedString(projectDeploymentWorkflowId)).append("\n");
    sb.append("    workflowUuid: ").append(toIndentedString(workflowUuid)).append("\n");
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

