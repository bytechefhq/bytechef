package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * EvaluateWorkflowNodeDisplayConditionRequestModel
 */

@JsonTypeName("evaluateWorkflowNodeDisplayCondition_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-05T15:47:20.085572+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class EvaluateWorkflowNodeDisplayConditionRequestModel {

  private String displayCondition;

  public EvaluateWorkflowNodeDisplayConditionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EvaluateWorkflowNodeDisplayConditionRequestModel(String displayCondition) {
    this.displayCondition = displayCondition;
  }

  public EvaluateWorkflowNodeDisplayConditionRequestModel displayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
    return this;
  }

  /**
   * Get displayCondition
   * @return displayCondition
  */
  @NotNull 
  @Schema(name = "displayCondition", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("displayCondition")
  public String getDisplayCondition() {
    return displayCondition;
  }

  public void setDisplayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EvaluateWorkflowNodeDisplayConditionRequestModel evaluateWorkflowNodeDisplayConditionRequest = (EvaluateWorkflowNodeDisplayConditionRequestModel) o;
    return Objects.equals(this.displayCondition, evaluateWorkflowNodeDisplayConditionRequest.displayCondition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayCondition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EvaluateWorkflowNodeDisplayConditionRequestModel {\n");
    sb.append("    displayCondition: ").append(toIndentedString(displayCondition)).append("\n");
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

