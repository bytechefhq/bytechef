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
 * UpdateWorkflowNodeParameterRequestModel
 */

@JsonTypeName("updateWorkflowNodeParameter_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:33.876560+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class UpdateWorkflowNodeParameterRequestModel {

  private Boolean includeInMetadata = false;

  private String path;

  private String type;

  private @Nullable Object value;

  public UpdateWorkflowNodeParameterRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateWorkflowNodeParameterRequestModel(String path, String type) {
    this.path = path;
    this.type = type;
  }

  public UpdateWorkflowNodeParameterRequestModel includeInMetadata(Boolean includeInMetadata) {
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

  public UpdateWorkflowNodeParameterRequestModel path(String path) {
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

  public UpdateWorkflowNodeParameterRequestModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of a property.
   * @return type
   */
  @NotNull 
  @Schema(name = "type", description = "The type of a property.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public String getType() {
    return type;
  }

  public void setType(String type) {
    this.type = type;
  }

  public UpdateWorkflowNodeParameterRequestModel value(@Nullable Object value) {
    this.value = value;
    return this;
  }

  /**
   * The value.
   * @return value
   */
  
  @Schema(name = "value", description = "The value.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public @Nullable Object getValue() {
    return value;
  }

  public void setValue(@Nullable Object value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateWorkflowNodeParameterRequestModel updateWorkflowNodeParameterRequest = (UpdateWorkflowNodeParameterRequestModel) o;
    return Objects.equals(this.includeInMetadata, updateWorkflowNodeParameterRequest.includeInMetadata) &&
        Objects.equals(this.path, updateWorkflowNodeParameterRequest.path) &&
        Objects.equals(this.type, updateWorkflowNodeParameterRequest.type) &&
        Objects.equals(this.value, updateWorkflowNodeParameterRequest.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(includeInMetadata, path, type, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateWorkflowNodeParameterRequestModel {\n");
    sb.append("    includeInMetadata: ").append(toIndentedString(includeInMetadata)).append("\n");
    sb.append("    path: ").append(toIndentedString(path)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

