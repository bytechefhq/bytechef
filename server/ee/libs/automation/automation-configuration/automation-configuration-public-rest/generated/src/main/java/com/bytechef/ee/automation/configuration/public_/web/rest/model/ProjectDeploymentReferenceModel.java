package com.bytechef.ee.automation.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The project deployment a workflow execution ran under.
 */

@Schema(name = "ProjectDeploymentReference", description = "The project deployment a workflow execution ran under.")
@JsonTypeName("ProjectDeploymentReference")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-18T16:40:02.539074685Z[Etc/UTC]", comments = "Generator version: 7.22.0")
public class ProjectDeploymentReferenceModel {

  private @Nullable Long id;

  private @Nullable String name;

  private @Nullable Integer projectVersion;

  private @Nullable Long environmentId;

  private @Nullable Boolean enabled;

  public ProjectDeploymentReferenceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of a project deployment.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public ProjectDeploymentReferenceModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a project deployment.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of a project deployment.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ProjectDeploymentReferenceModel projectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The deployed project version.
   * @return projectVersion
   */
  
  @Schema(name = "projectVersion", description = "The deployed project version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("projectVersion")
  public @Nullable Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(@Nullable Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ProjectDeploymentReferenceModel environmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * The id of the environment the deployment targets.
   * @return environmentId
   */
  
  @Schema(name = "environmentId", description = "The id of the environment the deployment targets.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environmentId")
  public @Nullable Long getEnvironmentId() {
    return environmentId;
  }

  @JsonProperty("environmentId")
  public void setEnvironmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
  }

  public ProjectDeploymentReferenceModel enabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the deployment is enabled.
   * @return enabled
   */
  
  @Schema(name = "enabled", description = "Whether the deployment is enabled.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("enabled")
  public @Nullable Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(@Nullable Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProjectDeploymentReferenceModel projectDeploymentReference = (ProjectDeploymentReferenceModel) o;
    return Objects.equals(this.id, projectDeploymentReference.id) &&
        Objects.equals(this.name, projectDeploymentReference.name) &&
        Objects.equals(this.projectVersion, projectDeploymentReference.projectVersion) &&
        Objects.equals(this.environmentId, projectDeploymentReference.environmentId) &&
        Objects.equals(this.enabled, projectDeploymentReference.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, name, projectVersion, environmentId, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProjectDeploymentReferenceModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

