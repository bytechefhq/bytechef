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
 * Returned when the connected user has already copied an automation workflow template.
 */

@Schema(name = "WorkflowTemplateAlreadyCopiedError", description = "Returned when the connected user has already copied an automation workflow template.")
@JsonTypeName("WorkflowTemplateAlreadyCopiedError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-11T13:57:43.209403+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class WorkflowTemplateAlreadyCopiedErrorModel {

  private @Nullable String copiedWorkflowUuid;

  public WorkflowTemplateAlreadyCopiedErrorModel copiedWorkflowUuid(@Nullable String copiedWorkflowUuid) {
    this.copiedWorkflowUuid = copiedWorkflowUuid;
    return this;
  }

  /**
   * The uuid of the connected user's existing copy of the template.
   * @return copiedWorkflowUuid
   */
  
  @Schema(name = "copiedWorkflowUuid", description = "The uuid of the connected user's existing copy of the template.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("copiedWorkflowUuid")
  public @Nullable String getCopiedWorkflowUuid() {
    return copiedWorkflowUuid;
  }

  @JsonProperty("copiedWorkflowUuid")
  public void setCopiedWorkflowUuid(@Nullable String copiedWorkflowUuid) {
    this.copiedWorkflowUuid = copiedWorkflowUuid;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    WorkflowTemplateAlreadyCopiedErrorModel workflowTemplateAlreadyCopiedError = (WorkflowTemplateAlreadyCopiedErrorModel) o;
    return Objects.equals(this.copiedWorkflowUuid, workflowTemplateAlreadyCopiedError.copiedWorkflowUuid);
  }

  @Override
  public int hashCode() {
    return Objects.hash(copiedWorkflowUuid);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowTemplateAlreadyCopiedErrorModel {\n");
    sb.append("    copiedWorkflowUuid: ").append(toIndentedString(copiedWorkflowUuid)).append("\n");
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

