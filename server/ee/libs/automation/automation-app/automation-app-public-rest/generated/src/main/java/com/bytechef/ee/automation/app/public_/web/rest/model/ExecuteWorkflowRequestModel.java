package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
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
 * ExecuteWorkflowRequestModel
 */

@JsonTypeName("ExecuteWorkflowRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ExecuteWorkflowRequestModel {

  private Map<String, Object> input = new HashMap<>();

  private @Nullable Integer waitSeconds;

  public ExecuteWorkflowRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExecuteWorkflowRequestModel(Map<String, Object> input) {
    this.input = input;
  }

  public ExecuteWorkflowRequestModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public ExecuteWorkflowRequestModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The run input; validated against the workflow's inputSchema.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The run input; validated against the workflow's inputSchema.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  public ExecuteWorkflowRequestModel waitSeconds(@Nullable Integer waitSeconds) {
    this.waitSeconds = waitSeconds;
    return this;
  }

  /**
   * How long to wait for the run before answering 202, from 0 to the server's maximum; the server's default when omitted.
   * @return waitSeconds
   */
  
  @Schema(name = "waitSeconds", description = "How long to wait for the run before answering 202, from 0 to the server's maximum; the server's default when omitted.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("waitSeconds")
  public @Nullable Integer getWaitSeconds() {
    return waitSeconds;
  }

  @JsonProperty("waitSeconds")
  public void setWaitSeconds(@Nullable Integer waitSeconds) {
    this.waitSeconds = waitSeconds;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecuteWorkflowRequestModel executeWorkflowRequest = (ExecuteWorkflowRequestModel) o;
    return Objects.equals(this.input, executeWorkflowRequest.input) &&
        Objects.equals(this.waitSeconds, executeWorkflowRequest.waitSeconds);
  }

  @Override
  public int hashCode() {
    return Objects.hash(input, waitSeconds);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteWorkflowRequestModel {\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    waitSeconds: ").append(toIndentedString(waitSeconds)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

