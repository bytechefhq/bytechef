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
 * GetWorkflowNodeParameterDisplayConditions200ResponseModel
 */

@JsonTypeName("getWorkflowNodeParameterDisplayConditions_200_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-05-01T08:31:43.380174+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class GetWorkflowNodeParameterDisplayConditions200ResponseModel {

  @Valid
  private Map<String, Boolean> displayConditions = new HashMap<>();

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    GetWorkflowNodeParameterDisplayConditions200ResponseModel getWorkflowNodeParameterDisplayConditions200Response = (GetWorkflowNodeParameterDisplayConditions200ResponseModel) o;
    return Objects.equals(this.displayConditions, getWorkflowNodeParameterDisplayConditions200Response.displayConditions);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayConditions);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetWorkflowNodeParameterDisplayConditions200ResponseModel {\n");
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

