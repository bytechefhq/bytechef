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
 * GetWorkflowNodeParameterDisplayConditions200ResponseModel
 */

@JsonTypeName("getWorkflowNodeParameterDisplayConditions_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-24T23:18:46.779804+02:00[Europe/Zagreb]", comments = "Generator version: 7.13.0")
public class GetWorkflowNodeParameterDisplayConditions200ResponseModel {

  @Valid
  private Map<String, Boolean> displayConditions = new HashMap<>();

  @Valid
  private List<String> missingRequiredProperties = new ArrayList<>();

  public GetWorkflowNodeParameterDisplayConditions200ResponseModel displayConditions(Map<String, Boolean> displayConditions) {
    this.displayConditions = displayConditions;
    return this;
  }

  public GetWorkflowNodeParameterDisplayConditions200ResponseModel putDisplayConditionsItem(String key, Boolean displayConditionsItem) {
    if (this.displayConditions == null) {
      this.displayConditions = new HashMap<>();
    }
    this.displayConditions.put(key, displayConditionsItem);
    return this;
  }

  /**
   * Get displayConditions
   * @return displayConditions
   */
  
  @Schema(name = "displayConditions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayConditions")
  public Map<String, Boolean> getDisplayConditions() {
    return displayConditions;
  }

  public void setDisplayConditions(Map<String, Boolean> displayConditions) {
    this.displayConditions = displayConditions;
  }

  public GetWorkflowNodeParameterDisplayConditions200ResponseModel missingRequiredProperties(List<String> missingRequiredProperties) {
    this.missingRequiredProperties = missingRequiredProperties;
    return this;
  }

  public GetWorkflowNodeParameterDisplayConditions200ResponseModel addMissingRequiredPropertiesItem(String missingRequiredPropertiesItem) {
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
    GetWorkflowNodeParameterDisplayConditions200ResponseModel getWorkflowNodeParameterDisplayConditions200Response = (GetWorkflowNodeParameterDisplayConditions200ResponseModel) o;
    return Objects.equals(this.displayConditions, getWorkflowNodeParameterDisplayConditions200Response.displayConditions) &&
        Objects.equals(this.missingRequiredProperties, getWorkflowNodeParameterDisplayConditions200Response.missingRequiredProperties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayConditions, missingRequiredProperties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetWorkflowNodeParameterDisplayConditions200ResponseModel {\n");
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

