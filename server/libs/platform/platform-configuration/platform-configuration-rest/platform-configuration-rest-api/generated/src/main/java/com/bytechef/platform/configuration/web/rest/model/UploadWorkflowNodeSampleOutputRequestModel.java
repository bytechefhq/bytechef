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
 * UploadWorkflowNodeSampleOutputRequestModel
 */

@JsonTypeName("uploadWorkflowNodeSampleOutput_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-01T17:41:34.894265+01:00[Europe/Zagreb]")
public class UploadWorkflowNodeSampleOutputRequestModel {

  @Valid
  private Map<String, Object> sampleOutput = new HashMap<>();

  public UploadWorkflowNodeSampleOutputRequestModel sampleOutput(Map<String, Object> sampleOutput) {
    this.sampleOutput = sampleOutput;
    return this;
  }

  public UploadWorkflowNodeSampleOutputRequestModel putSampleOutputItem(String key, Object sampleOutputItem) {
    if (this.sampleOutput == null) {
      this.sampleOutput = new HashMap<>();
    }
    this.sampleOutput.put(key, sampleOutputItem);
    return this;
  }

  /**
   * Get sampleOutput
   * @return sampleOutput
  */
  
  @Schema(name = "sampleOutput", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sampleOutput")
  public Map<String, Object> getSampleOutput() {
    return sampleOutput;
  }

  public void setSampleOutput(Map<String, Object> sampleOutput) {
    this.sampleOutput = sampleOutput;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UploadWorkflowNodeSampleOutputRequestModel uploadWorkflowNodeSampleOutputRequest = (UploadWorkflowNodeSampleOutputRequestModel) o;
    return Objects.equals(this.sampleOutput, uploadWorkflowNodeSampleOutputRequest.sampleOutput);
  }

  @Override
  public int hashCode() {
    return Objects.hash(sampleOutput);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UploadWorkflowNodeSampleOutputRequestModel {\n");
    sb.append("    sampleOutput: ").append(toIndentedString(sampleOutput)).append("\n");
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

