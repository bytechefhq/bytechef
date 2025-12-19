package com.bytechef.platform.configuration.web.rest.model;

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
 * DeleteClusterElementParameterRequestModel
 */

@JsonTypeName("deleteClusterElementParameter_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-19T14:30:20.578204370+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class DeleteClusterElementParameterRequestModel {

  private Boolean fromAiInMetadata = false;

  private String path;

  public DeleteClusterElementParameterRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DeleteClusterElementParameterRequestModel(String path) {
    this.path = path;
  }

  public DeleteClusterElementParameterRequestModel fromAiInMetadata(Boolean fromAiInMetadata) {
    this.fromAiInMetadata = fromAiInMetadata;
    return this;
  }

  /**
   * If fromAi should be included in metadata.
   * @return fromAiInMetadata
   */
  
  @Schema(name = "fromAiInMetadata", description = "If fromAi should be included in metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fromAiInMetadata")
  public Boolean getFromAiInMetadata() {
    return fromAiInMetadata;
  }

  public void setFromAiInMetadata(Boolean fromAiInMetadata) {
    this.fromAiInMetadata = fromAiInMetadata;
  }

  public DeleteClusterElementParameterRequestModel path(String path) {
    this.path = path;
    return this;
  }

  /**
   * The workflow node parameter path.
   * @return path
   */
  @NotNull 
  @Schema(name = "path", description = "The workflow node parameter path.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("path")
  public String getPath() {
    return path;
  }

  public void setPath(String path) {
    this.path = path;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DeleteClusterElementParameterRequestModel deleteClusterElementParameterRequest = (DeleteClusterElementParameterRequestModel) o;
    return Objects.equals(this.fromAiInMetadata, deleteClusterElementParameterRequest.fromAiInMetadata) &&
        Objects.equals(this.path, deleteClusterElementParameterRequest.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(fromAiInMetadata, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteClusterElementParameterRequestModel {\n");
    sb.append("    fromAiInMetadata: ").append(toIndentedString(fromAiInMetadata)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
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

