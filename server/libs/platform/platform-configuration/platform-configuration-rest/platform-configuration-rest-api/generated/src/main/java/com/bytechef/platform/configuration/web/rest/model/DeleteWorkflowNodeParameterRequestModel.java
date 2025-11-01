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
 * DeleteWorkflowNodeParameterRequestModel
 */

@JsonTypeName("deleteWorkflowNodeParameter_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-10-21T12:06:41.161145+02:00[Europe/Zagreb]", comments = "Generator version: 7.16.0")
public class DeleteWorkflowNodeParameterRequestModel {

  private Boolean includeInMetadata = false;

  private String path;

  public DeleteWorkflowNodeParameterRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public DeleteWorkflowNodeParameterRequestModel(String path) {
    this.path = path;
  }

  public DeleteWorkflowNodeParameterRequestModel includeInMetadata(Boolean includeInMetadata) {
    this.includeInMetadata = includeInMetadata;
    return this;
  }

  /**
   * If path and value type should be included in metadata.
   * @return includeInMetadata
   */
  
  @Schema(name = "includeInMetadata", description = "If path and value type should be included in metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("includeInMetadata")
  public Boolean getIncludeInMetadata() {
    return includeInMetadata;
  }

  public void setIncludeInMetadata(Boolean includeInMetadata) {
    this.includeInMetadata = includeInMetadata;
  }

  public DeleteWorkflowNodeParameterRequestModel path(String path) {
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
    DeleteWorkflowNodeParameterRequestModel deleteWorkflowNodeParameterRequest = (DeleteWorkflowNodeParameterRequestModel) o;
    return Objects.equals(this.includeInMetadata, deleteWorkflowNodeParameterRequest.includeInMetadata) &&
        Objects.equals(this.path, deleteWorkflowNodeParameterRequest.path);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includeInMetadata, path);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DeleteWorkflowNodeParameterRequestModel {\n");
    sb.append("    includeInMetadata: ").append(toIndentedString(includeInMetadata)).append("\n");
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

