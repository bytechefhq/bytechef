package com.bytechef.ee.automation.apiplatform.configuration.public_.web.rest.model;

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
 * The project version to move an API collection to.
 */

@Schema(name = "ApiCollectionProjectVersionInput", description = "The project version to move an API collection to.")
@JsonTypeName("ApiCollectionProjectVersionInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T01:43:17.216712+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ApiCollectionProjectVersionInputModel {

  private Integer projectVersion;

  private Boolean removeMissingEndpoints = false;

  public ApiCollectionProjectVersionInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ApiCollectionProjectVersionInputModel(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ApiCollectionProjectVersionInputModel projectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
    return this;
  }

  /**
   * Get projectVersion
   * @return projectVersion
   */
  @NotNull 
  @Schema(name = "projectVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("projectVersion")
  public Integer getProjectVersion() {
    return projectVersion;
  }

  @JsonProperty("projectVersion")
  public void setProjectVersion(Integer projectVersion) {
    this.projectVersion = projectVersion;
  }

  public ApiCollectionProjectVersionInputModel removeMissingEndpoints(Boolean removeMissingEndpoints) {
    this.removeMissingEndpoints = removeMissingEndpoints;
    return this;
  }

  /**
   * Delete the endpoints whose workflow the new version lacks instead of refusing the move.
   * @return removeMissingEndpoints
   */
  
  @Schema(name = "removeMissingEndpoints", description = "Delete the endpoints whose workflow the new version lacks instead of refusing the move.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    ApiCollectionProjectVersionInputModel apiCollectionProjectVersionInput = (ApiCollectionProjectVersionInputModel) o;
    return Objects.equals(this.projectVersion, apiCollectionProjectVersionInput.projectVersion) &&
        Objects.equals(this.removeMissingEndpoints, apiCollectionProjectVersionInput.removeMissingEndpoints);
  }

  @Override
  public int hashCode() {
    return Objects.hash(projectVersion, removeMissingEndpoints);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApiCollectionProjectVersionInputModel {\n");
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

