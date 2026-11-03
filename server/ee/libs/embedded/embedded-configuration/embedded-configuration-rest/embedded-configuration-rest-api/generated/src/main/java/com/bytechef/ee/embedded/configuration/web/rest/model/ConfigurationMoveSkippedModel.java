package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A connected user the move left where it was.
 */

@Schema(name = "ConfigurationMoveSkipped", description = "A connected user the move left where it was.")
@JsonTypeName("ConfigurationMoveSkipped")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T02:20:55.684519+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationMoveSkippedModel {

  private Long connectedUserId;

  private String reason;

  private List<String> workflowLabels = new ArrayList<>();

  public ConfigurationMoveSkippedModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationMoveSkippedModel(Long connectedUserId, String reason, List<String> workflowLabels) {
    this.connectedUserId = connectedUserId;
    this.reason = reason;
    this.workflowLabels = workflowLabels;
  }

  public ConfigurationMoveSkippedModel connectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
    return this;
  }

  /**
   * The id of the connected user.
   * @return connectedUserId
   */
  @NotNull 
  @Schema(name = "connectedUserId", description = "The id of the connected user.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectedUserId")
  public Long getConnectedUserId() {
    return connectedUserId;
  }

  @JsonProperty("connectedUserId")
  public void setConnectedUserId(Long connectedUserId) {
    this.connectedUserId = connectedUserId;
  }

  public ConfigurationMoveSkippedModel reason(String reason) {
    this.reason = reason;
    return this;
  }

  /**
   * Why the connected user was left in place.
   * @return reason
   */
  @NotNull 
  @Schema(name = "reason", description = "Why the connected user was left in place.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("reason")
  public String getReason() {
    return reason;
  }

  @JsonProperty("reason")
  public void setReason(String reason) {
    this.reason = reason;
  }

  public ConfigurationMoveSkippedModel workflowLabels(List<String> workflowLabels) {
    this.workflowLabels = workflowLabels;
    return this;
  }

  public ConfigurationMoveSkippedModel addWorkflowLabelsItem(String workflowLabelsItem) {
    if (this.workflowLabels == null) {
      this.workflowLabels = new ArrayList<>();
    }
    this.workflowLabels.add(workflowLabelsItem);
    return this;
  }

  /**
   * The labels of the workflows the target lacks; empty for the other reasons.
   * @return workflowLabels
   */
  @NotNull 
  @Schema(name = "workflowLabels", description = "The labels of the workflows the target lacks; empty for the other reasons.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflowLabels")
  public List<String> getWorkflowLabels() {
    return workflowLabels;
  }

  @JsonProperty("workflowLabels")
  public void setWorkflowLabels(List<String> workflowLabels) {
    this.workflowLabels = workflowLabels;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationMoveSkippedModel configurationMoveSkipped = (ConfigurationMoveSkippedModel) o;
    return Objects.equals(this.connectedUserId, configurationMoveSkipped.connectedUserId) &&
        Objects.equals(this.reason, configurationMoveSkipped.reason) &&
        Objects.equals(this.workflowLabels, configurationMoveSkipped.workflowLabels);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectedUserId, reason, workflowLabels);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationMoveSkippedModel {\n");
    sb.append("    connectedUserId: ").append(toIndentedString(connectedUserId)).append("\n");
    sb.append("    reason: ").append(toIndentedString(reason)).append("\n");
    sb.append("    workflowLabels: ").append(toIndentedString(workflowLabels)).append("\n");
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

