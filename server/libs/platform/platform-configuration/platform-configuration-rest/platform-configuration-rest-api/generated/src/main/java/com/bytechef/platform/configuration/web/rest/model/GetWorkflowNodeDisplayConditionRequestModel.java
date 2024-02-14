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
 * GetWorkflowNodeDisplayConditionRequestModel
 */

@JsonTypeName("getWorkflowNodeDisplayCondition_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-14T16:08:31.382431+01:00[Europe/Zagreb]")
public class GetWorkflowNodeDisplayConditionRequestModel {

  private String displayCondition;

  public GetWorkflowNodeDisplayConditionRequestModel displayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
    return this;
  }

  /**
   * Get displayCondition
   * @return displayCondition
  */
  
  @Schema(name = "displayCondition", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    GetWorkflowNodeDisplayConditionRequestModel getWorkflowNodeDisplayConditionRequest = (GetWorkflowNodeDisplayConditionRequestModel) o;
    return Objects.equals(this.displayCondition, getWorkflowNodeDisplayConditionRequest.displayCondition);
  }

  @Override
  public int hashCode() {
    return Objects.hash(displayCondition);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class GetWorkflowNodeDisplayConditionRequestModel {\n");
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

