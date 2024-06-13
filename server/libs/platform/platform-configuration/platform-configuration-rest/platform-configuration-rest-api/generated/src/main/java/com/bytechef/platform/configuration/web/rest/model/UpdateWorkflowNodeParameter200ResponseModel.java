package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateWorkflowNodeParameter200ResponseModel
 */

@JsonTypeName("updateWorkflowNodeParameter_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-06-13T06:45:36.300303+02:00[Europe/Zagreb]", comments = "Generator version: 7.5.0")
public class UpdateWorkflowNodeParameter200ResponseModel {

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private Map<String, Boolean> displayConditions = new HashMap<>();

  public UpdateWorkflowNodeParameter200ResponseModel metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public UpdateWorkflowNodeParameter200ResponseModel putMetadataItem(String key, Object metadataItem) {
    if (this.metadata == null) {
      this.metadata = new HashMap<>();
    }
    this.metadata.put(key, metadataItem);
    return this;
  }

  /**
   * Key-value map of metadata.
   * @return metadata
  */
  
  @Schema(name = "metadata", description = "Key-value map of metadata.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("metadata")
  public Map<String, Object> getMetadata() {
    return metadata;
  }

  public void setMetadata(Map<String, Object> metadata) {
    this.metadata = metadata;
  }

  public UpdateWorkflowNodeParameter200ResponseModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public UpdateWorkflowNodeParameter200ResponseModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null) {
      this.parameters = new HashMap<>();
    }
    this.parameters.put(key, parametersItem);
    return this;
  }

  /**
   * Key-value map of parameters.
   * @return parameters
  */
  
  @Schema(name = "parameters", description = "Key-value map of parameters.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("parameters")
  public Map<String, Object> getParameters() {
    return parameters;
  }

  public void setParameters(Map<String, Object> parameters) {
    this.parameters = parameters;
  }

  public UpdateWorkflowNodeParameter200ResponseModel displayConditions(Map<String, Boolean> displayConditions) {
    this.displayConditions = displayConditions;
    return this;
  }

  public UpdateWorkflowNodeParameter200ResponseModel putDisplayConditionsItem(String key, Boolean displayConditionsItem) {
    if (this.displayConditions == null) {
      this.displayConditions = new HashMap<>();
    }
    this.displayConditions.put(key, displayConditionsItem);
    return this;
  }

  /**
   * Key-value map of display condition rules.
   * @return displayConditions
  */
  
  @Schema(name = "displayConditions", description = "Key-value map of display condition rules.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayConditions")
  public Map<String, Boolean> getDisplayConditions() {
    return displayConditions;
  }

  public void setDisplayConditions(Map<String, Boolean> displayConditions) {
    this.displayConditions = displayConditions;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateWorkflowNodeParameter200ResponseModel updateWorkflowNodeParameter200Response = (UpdateWorkflowNodeParameter200ResponseModel) o;
    return Objects.equals(this.metadata, updateWorkflowNodeParameter200Response.metadata) &&
        Objects.equals(this.parameters, updateWorkflowNodeParameter200Response.parameters) &&
        Objects.equals(this.displayConditions, updateWorkflowNodeParameter200Response.displayConditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, parameters, displayConditions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateWorkflowNodeParameter200ResponseModel {\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    displayConditions: ").append(toIndentedString(displayConditions)).append("\n");
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

