package com.bytechef.ee.automation.apiplatform.configuration.web.rest.model;

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
 * The request object that names the project version to move an API collection to.
 */

@Schema(name = "UpdateApiCollectionProjectVersionRequest", description = "The request object that names the project version to move an API collection to.")
@JsonTypeName("UpdateApiCollectionProjectVersionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:38:17.244806+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class UpdateApiCollectionProjectVersionRequestModel {

  private Integer projectVersion;

  private Boolean removeMissingEndpoints = false;

  public UpdateApiCollectionProjectVersionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateApiCollectionProjectVersionRequestModel(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public UpdateApiCollectionProjectVersionRequestModel projectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * The published project version to move to.
   * @return projectVersion
   */
  @NotNull 
  @Schema(name = "projectVersion", description = "The published project version to move to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectVersion")
  public Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public UpdateApiCollectionProjectVersionRequestModel removeMissingEndpoints(Boolean removeMissingEndpoints) {
    this.removeMissingEndpoints = removeMissingEndpoints;
    return this;
  }

  /**
   * Whether to delete the endpoints whose workflow is absent from the new version.
   * @return removeMissingEndpoints
   */
  
  @Schema(name = "removeMissingEndpoints", description = "Whether to delete the endpoints whose workflow is absent from the new version.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("removeMissingEndpoints")
  public Boolean getRemoveMissingEndpoints() {
    return removeMissingEndpoints;
  }

  @JsonProperty("removeMissingEndpoints")
  public void setRemoveMissingEndpoints(Boolean removeMissingEndpoints) {
    this.removeMissingEndpoints = removeMissingEndpoints;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateApiCollectionProjectVersionRequestModel updateApiCollectionProjectVersionRequest = (UpdateApiCollectionProjectVersionRequestModel) o;
    return Objects.equals(this.projectVersion, updateApiCollectionProjectVersionRequest.projectVersion) &&
        Objects.equals(this.removeMissingEndpoints, updateApiCollectionProjectVersionRequest.removeMissingEndpoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectVersion, removeMissingEndpoints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateApiCollectionProjectVersionRequestModel {\n");
    sb.append("    projectVersion: ").append(toIndentedString(projectVersion)).append("\n");
    sb.append("    removeMissingEndpoints: ").append(toIndentedString(removeMissingEndpoints)).append("\n");
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

