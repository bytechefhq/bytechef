package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * The outcome of enabling a workflow.
 */

@Schema(name = "EnableConnectedUserWorkflowResponse", description = "The outcome of enabling a workflow.")
@JsonTypeName("EnableConnectedUserWorkflowResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-29T14:51:45.918422+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class EnableConnectedUserWorkflowResponseModel {

  private @Nullable String attentionReason;

  private Boolean enabled;

  public EnableConnectedUserWorkflowResponseModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EnableConnectedUserWorkflowResponseModel(Boolean enabled) {
    this.enabled = enabled;
  }

  public EnableConnectedUserWorkflowResponseModel attentionReason(@Nullable String attentionReason) {
    this.attentionReason = attentionReason;
    return this;
  }

  /**
   * Why the workflow stays paused: TURNED_OFF_BY_ADMINISTRATOR, UPDATE_PENDING, MISSING_CONNECTION:<component>, INPUT_REQUIRED:<input> or INVALID_WORKFLOW:<errorCount>. Null when nothing blocks it.
   * @return attentionReason
   */
  
  @Schema(name = "attentionReason", description = "Why the workflow stays paused: TURNED_OFF_BY_ADMINISTRATOR, UPDATE_PENDING, MISSING_CONNECTION:<component>, INPUT_REQUIRED:<input> or INVALID_WORKFLOW:<errorCount>. Null when nothing blocks it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("attentionReason")
  public @Nullable String getAttentionReason() {
    return attentionReason;
  }

  @JsonProperty("attentionReason")
  public void setAttentionReason(@Nullable String attentionReason) {
    this.attentionReason = attentionReason;
  }

  public EnableConnectedUserWorkflowResponseModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * Whether the workflow is enabled for the connected user.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "Whether the workflow is enabled for the connected user.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  @JsonProperty("enabled")
  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EnableConnectedUserWorkflowResponseModel enableConnectedUserWorkflowResponse = (EnableConnectedUserWorkflowResponseModel) o;
    return Objects.equals(this.attentionReason, enableConnectedUserWorkflowResponse.attentionReason) &&
        Objects.equals(this.enabled, enableConnectedUserWorkflowResponse.enabled);
  }

  @Override
  public int hashCode() {
    return Objects.hash(attentionReason, enabled);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EnableConnectedUserWorkflowResponseModel {\n");
    sb.append("    attentionReason: ").append(toIndentedString(attentionReason)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
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

