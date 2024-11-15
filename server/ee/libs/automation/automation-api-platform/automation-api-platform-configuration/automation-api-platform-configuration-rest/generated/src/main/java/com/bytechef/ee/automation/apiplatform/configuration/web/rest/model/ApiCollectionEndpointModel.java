package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.apiplatform.configuration.web.rest.model.HttpMethodModel;
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
 * An API collection endpoint.
 */

@Schema(name = "ApiCollectionEndpoint", description = "An API collection endpoint.")
@JsonTypeName("ApiCollectionEndpoint")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-11-15T12:48:42.005777+01:00[Europe/Zagreb]", comments = "Generator version: 7.9.0")
public class ApiCollectionEndpointModel {

  private Long apiCollectionId;

  private String createdBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime createdDate;

  private Boolean enabled = false;

  private HttpMethodModel httpMethod;

  private Long id;

  private String name;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastExecutionDate;

  private String lastModifiedBy;

  @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
  private LocalDateTime lastModifiedDate;

  private String path;

  private Long projectInstanceWorkflowId;

  private String workflowReferenceCode;

  private Integer version;

  public ApiCollectionEndpointModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionEndpointModel(Boolean enabled, HttpMethodModel httpMethod, String path, String workflowReferenceCode) {
    this.enabled = enabled;
    this.httpMethod = httpMethod;
    this.path = path;
    this.workflowReferenceCode = workflowReferenceCode;
  }

  public ApiCollectionEndpointModel apiCollectionId(Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
    return this;
  }

  /**
   * The id of an API collection.
   * @return apiCollectionId
   */
  
  @Schema(name = "apiCollectionId", description = "The id of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("apiCollectionId")
  public Long getApiCollectionId() {
    return apiCollectionId;
  }

  public void setApiCollectionId(Long apiCollectionId) {
    this.apiCollectionId = apiCollectionId;
  }

  public ApiCollectionEndpointModel createdBy(String createdBy) {
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

  public ApiCollectionEndpointModel createdDate(LocalDateTime createdDate) {
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

  public ApiCollectionEndpointModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an API collection.
   * @return id
   */
  
  @Schema(name = "id", accessMode = Schema.AccessMode.READ_ONLY, description = "The id of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public ApiCollectionEndpointModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an API collection.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of an API collection.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public ApiCollectionEndpointModel lastExecutionDate(LocalDateTime lastExecutionDate) {
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

  public ApiCollectionEndpointModel lastModifiedBy(String lastModifiedBy) {
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

  public ApiCollectionEndpointModel lastModifiedDate(LocalDateTime lastModifiedDate) {
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

  public ApiCollectionEndpointModel projectInstanceWorkflowId(Long projectInstanceWorkflowId) {
    this.projectInstanceWorkflowId = projectInstanceWorkflowId;
    return this;
  }

  /**
   * The project instance workflow id.
   * @return projectInstanceWorkflowId
   */
  
  @Schema(name = "projectInstanceWorkflowId", accessMode = Schema.AccessMode.READ_ONLY, description = "The project instance workflow id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectInstanceWorkflowId")
  public Long getProjectInstanceWorkflowId() {
    return projectInstanceWorkflowId;
  }

  public void setProjectInstanceWorkflowId(Long projectInstanceWorkflowId) {
    this.projectInstanceWorkflowId = projectInstanceWorkflowId;
  }

  public ApiCollectionEndpointModel workflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
    return this;
  }

  /**
   * The workflow reference code.
   * @return workflowReferenceCode
   */
  @NotNull 
  @Schema(name = "workflowReferenceCode", description = "The workflow reference code.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowReferenceCode")
  public String getWorkflowReferenceCode() {
    return workflowReferenceCode;
  }

  public void setWorkflowReferenceCode(String workflowReferenceCode) {
    this.workflowReferenceCode = workflowReferenceCode;
  }

  public ApiCollectionEndpointModel version(Integer version) {
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
        Objects.equals(this.projectInstanceWorkflowId, apiCollectionEndpoint.projectInstanceWorkflowId) &&
        Objects.equals(this.workflowReferenceCode, apiCollectionEndpoint.workflowReferenceCode) &&
        Objects.equals(this.version, apiCollectionEndpoint.version);
  }

  @Override
  public int hashCode() {
    return Objects.hash(apiCollectionId, createdBy, createdDate, enabled, httpMethod, id, name, lastExecutionDate, lastModifiedBy, lastModifiedDate, path, projectInstanceWorkflowId, workflowReferenceCode, version);
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
    sb.append("    projectInstanceWorkflowId: ").append(toIndentedString(projectInstanceWorkflowId)).append("\n");
    sb.append("    workflowReferenceCode: ").append(toIndentedString(workflowReferenceCode)).append("\n");
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

