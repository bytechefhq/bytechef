package com.bytechef.ee.automation.app.public_.web.rest.management.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.AppInstanceStatusModel;
import com.bytechef.ee.automation.app.public_.web.rest.management.model.EnvironmentModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An instance of the app that lives in another workspace: no bindings, no key hint, no configuration.
 */

@Schema(name = "ForeignAppInstance", description = "An instance of the app that lives in another workspace: no bindings, no key hint, no configuration.")
@JsonTypeName("ForeignAppInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-16T10:32:24.407292+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ForeignAppInstanceModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable Long workspaceId;

  private @Nullable EnvironmentModel environment;

  private @Nullable AppInstanceStatusModel status;

  private @Nullable Long currentAppDeploymentId;

  public ForeignAppInstanceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ForeignAppInstanceModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  
  @Schema(name = "name", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ForeignAppInstanceModel workspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
    return this;
  }

  /**
   * Get workspaceId
   * @return workspaceId
   */
  
  @Schema(name = "workspaceId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workspaceId")
  public @Nullable Long getWorkspaceId() {
    return workspaceId;
  }

  @JsonProperty("workspaceId")
  public void setWorkspaceId(@Nullable Long workspaceId) {
    this.workspaceId = workspaceId;
  }

  public ForeignAppInstanceModel environment(@Nullable EnvironmentModel environment) {
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
  public @Nullable EnvironmentModel getEnvironment() {
    return environment;
  }

  @JsonProperty("environment")
  public void setEnvironment(@Nullable EnvironmentModel environment) {
    this.environment = environment;
  }

  public ForeignAppInstanceModel status(@Nullable AppInstanceStatusModel status) {
    this.status = status;
    return this;
  }

  /**
   * Get status
   * @return status
   */
  @Valid 
  @Schema(name = "status", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("status")
  public @Nullable AppInstanceStatusModel getStatus() {
    return status;
  }

  @JsonProperty("status")
  public void setStatus(@Nullable AppInstanceStatusModel status) {
    this.status = status;
  }

  public ForeignAppInstanceModel currentAppDeploymentId(@Nullable Long currentAppDeploymentId) {
    this.currentAppDeploymentId = currentAppDeploymentId;
    return this;
  }

  /**
   * Get currentAppDeploymentId
   * @return currentAppDeploymentId
   */
  
  @Schema(name = "currentAppDeploymentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("currentAppDeploymentId")
  public @Nullable Long getCurrentAppDeploymentId() {
    return currentAppDeploymentId;
  }

  @JsonProperty("currentAppDeploymentId")
  public void setCurrentAppDeploymentId(@Nullable Long currentAppDeploymentId) {
    this.currentAppDeploymentId = currentAppDeploymentId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ForeignAppInstanceModel foreignAppInstance = (ForeignAppInstanceModel) o;
    return Objects.equals(this.id, foreignAppInstance.id) &&
        Objects.equals(this.name, foreignAppInstance.name) &&
        Objects.equals(this.workspaceId, foreignAppInstance.workspaceId) &&
        Objects.equals(this.environment, foreignAppInstance.environment) &&
        Objects.equals(this.status, foreignAppInstance.status) &&
        Objects.equals(this.currentAppDeploymentId, foreignAppInstance.currentAppDeploymentId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, workspaceId, environment, status, currentAppDeploymentId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ForeignAppInstanceModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    workspaceId: ").append(toIndentedString(workspaceId)).append("\n");
    sb.append("    environment: ").append(toIndentedString(environment)).append("\n");
    sb.append("    status: ").append(toIndentedString(status)).append("\n");
    sb.append("    currentAppDeploymentId: ").append(toIndentedString(currentAppDeploymentId)).append("\n");
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

