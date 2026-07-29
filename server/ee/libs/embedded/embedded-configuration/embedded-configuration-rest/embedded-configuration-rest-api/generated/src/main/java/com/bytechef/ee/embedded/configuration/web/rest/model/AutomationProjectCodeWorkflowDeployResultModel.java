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
 * The result of deploying an automation code workflow into the embedded catalog.
 */

@Schema(name = "AutomationProjectCodeWorkflowDeployResult", description = "The result of deploying an automation code workflow into the embedded catalog.")
@JsonTypeName("AutomationProjectCodeWorkflowDeployResult")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-27T23:40:49.562437+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class AutomationProjectCodeWorkflowDeployResultModel {

  @Valid
  private List<String> warnings = new ArrayList<>();

  public AutomationProjectCodeWorkflowDeployResultModel warnings(List<String> warnings) {
    this.warnings = warnings;
    return this;
  }

  public AutomationProjectCodeWorkflowDeployResultModel addWarningsItem(String warningsItem) {
    if (this.warnings == null) {
      this.warnings = new ArrayList<>();
    }
    this.warnings.add(warningsItem);
    return this;
  }

  /**
   * Deploy-time trigger validation warnings; a deployed workflow is still usable even with warnings.
   * @return warnings
   */
  
  @Schema(name = "warnings", description = "Deploy-time trigger validation warnings; a deployed workflow is still usable even with warnings.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("warnings")
  public List<String> getWarnings() {
    return warnings;
  }

  @JsonProperty("warnings")
  public void setWarnings(List<String> warnings) {
    this.warnings = warnings;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AutomationProjectCodeWorkflowDeployResultModel automationProjectCodeWorkflowDeployResult = (AutomationProjectCodeWorkflowDeployResultModel) o;
    return Objects.equals(this.warnings, automationProjectCodeWorkflowDeployResult.warnings);
  }

  @Override
  public int hashCode() {
    return Objects.hash(warnings);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AutomationProjectCodeWorkflowDeployResultModel {\n");
    sb.append("    warnings: ").append(toIndentedString(warnings)).append("\n");
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

