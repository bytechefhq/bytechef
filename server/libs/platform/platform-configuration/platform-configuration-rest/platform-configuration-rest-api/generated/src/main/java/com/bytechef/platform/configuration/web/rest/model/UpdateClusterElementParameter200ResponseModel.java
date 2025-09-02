package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateClusterElementParameter200ResponseModel
 */

@JsonTypeName("updateClusterElementParameter_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-09-02T08:24:03.555464+02:00[Europe/Zagreb]", comments = "Generator version: 7.14.0")
public class UpdateClusterElementParameter200ResponseModel {

  @Valid
  private Map<String, Object> metadata = new HashMap<>();

  @Valid
  private Map<String, Object> parameters = new HashMap<>();

  @Valid
  private Map<String, Boolean> displayConditions = new HashMap<>();

  @Valid
  private List<String> missingRequiredProperties = new ArrayList<>();

  public UpdateClusterElementParameter200ResponseModel metadata(Map<String, Object> metadata) {
    this.metadata = metadata;
    return this;
  }

  public UpdateClusterElementParameter200ResponseModel putMetadataItem(String key, Object metadataItem) {
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

  public UpdateClusterElementParameter200ResponseModel parameters(Map<String, Object> parameters) {
    this.parameters = parameters;
    return this;
  }

  public UpdateClusterElementParameter200ResponseModel putParametersItem(String key, Object parametersItem) {
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

  public UpdateClusterElementParameter200ResponseModel displayConditions(Map<String, Boolean> displayConditions) {
    this.displayConditions = displayConditions;
    return this;
  }

  public UpdateClusterElementParameter200ResponseModel putDisplayConditionsItem(String key, Boolean displayConditionsItem) {
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

  public UpdateClusterElementParameter200ResponseModel missingRequiredProperties(List<String> missingRequiredProperties) {
    this.missingRequiredProperties = missingRequiredProperties;
    return this;
  }

  public UpdateClusterElementParameter200ResponseModel addMissingRequiredPropertiesItem(String missingRequiredPropertiesItem) {
    if (this.missingRequiredProperties == null) {
      this.missingRequiredProperties = new ArrayList<>();
    }
    this.missingRequiredProperties.add(missingRequiredPropertiesItem);
    return this;
  }

  /**
   * Get missingRequiredProperties
   * @return missingRequiredProperties
   */
  
  @Schema(name = "missingRequiredProperties", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("missingRequiredProperties")
  public List<String> getMissingRequiredProperties() {
    return missingRequiredProperties;
  }

  public void setMissingRequiredProperties(List<String> missingRequiredProperties) {
    this.missingRequiredProperties = missingRequiredProperties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateClusterElementParameter200ResponseModel updateClusterElementParameter200Response = (UpdateClusterElementParameter200ResponseModel) o;
    return Objects.equals(this.metadata, updateClusterElementParameter200Response.metadata) &&
        Objects.equals(this.parameters, updateClusterElementParameter200Response.parameters) &&
        Objects.equals(this.displayConditions, updateClusterElementParameter200Response.displayConditions) &&
        Objects.equals(this.missingRequiredProperties, updateClusterElementParameter200Response.missingRequiredProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(metadata, parameters, displayConditions, missingRequiredProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UpdateClusterElementParameter200ResponseModel {\n");
    sb.append("    metadata: ").append(toIndentedString(metadata)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    displayConditions: ").append(toIndentedString(displayConditions)).append("\n");
    sb.append("    missingRequiredProperties: ").append(toIndentedString(missingRequiredProperties)).append("\n");
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

