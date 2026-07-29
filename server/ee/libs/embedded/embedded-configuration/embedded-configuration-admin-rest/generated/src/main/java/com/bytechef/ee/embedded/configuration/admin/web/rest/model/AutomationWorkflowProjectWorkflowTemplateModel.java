package com.bytechef.ee.embedded.configuration.admin.web.rest.model;

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
 * A catalog workflow template within an automation workflow project.
 */

@Schema(name = "AutomationWorkflowProjectWorkflowTemplate", description = "A catalog workflow template within an automation workflow project.")
@JsonTypeName("AutomationWorkflowProjectWorkflowTemplate")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-28T00:53:58.627309+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AutomationWorkflowProjectWorkflowTemplateModel {

  private @Nullable String label;

  public AutomationWorkflowProjectWorkflowTemplateModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of the workflow.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of the workflow.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationWorkflowProjectWorkflowTemplateModel automationWorkflowProjectWorkflowTemplate = (AutomationWorkflowProjectWorkflowTemplateModel) o;
    return Objects.equals(this.label, automationWorkflowProjectWorkflowTemplate.label);
  }

  @Override
  public int hashCode() {
    return Objects.hash(label);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationWorkflowProjectWorkflowTemplateModel {\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
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

