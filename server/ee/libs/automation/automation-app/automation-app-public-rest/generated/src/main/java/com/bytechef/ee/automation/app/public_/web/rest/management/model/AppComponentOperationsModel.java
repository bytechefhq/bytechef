package com.bytechef.ee.automation.app.public_.web.rest.management.model;

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
 * The names of the actions and agent tools of a component version.
 */

@Schema(name = "AppComponentOperations", description = "The names of the actions and agent tools of a component version.")
@JsonTypeName("AppComponentOperations")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppComponentOperationsModel {

  private List<String> actionNames = new ArrayList<>();

  private List<String> toolNames = new ArrayList<>();

  public AppComponentOperationsModel actionNames(List<String> actionNames) {
    this.actionNames = actionNames;
    return this;
  }

  public AppComponentOperationsModel addActionNamesItem(String actionNamesItem) {
    if (this.actionNames == null) {
      this.actionNames = new ArrayList<>();
    }
    this.actionNames.add(actionNamesItem);
    return this;
  }

  /**
   * Get actionNames
   * @return actionNames
   */
  
  @Schema(name = "actionNames", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionNames")
  public List<String> getActionNames() {
    return actionNames;
  }

  @JsonProperty("actionNames")
  public void setActionNames(List<String> actionNames) {
    this.actionNames = actionNames;
  }

  public AppComponentOperationsModel toolNames(List<String> toolNames) {
    this.toolNames = toolNames;
    return this;
  }

  public AppComponentOperationsModel addToolNamesItem(String toolNamesItem) {
    if (this.toolNames == null) {
      this.toolNames = new ArrayList<>();
    }
    this.toolNames.add(toolNamesItem);
    return this;
  }

  /**
   * Get toolNames
   * @return toolNames
   */
  
  @Schema(name = "toolNames", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolNames")
  public List<String> getToolNames() {
    return toolNames;
  }

  @JsonProperty("toolNames")
  public void setToolNames(List<String> toolNames) {
    this.toolNames = toolNames;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppComponentOperationsModel appComponentOperations = (AppComponentOperationsModel) o;
    return Objects.equals(this.actionNames, appComponentOperations.actionNames) &&
        Objects.equals(this.toolNames, appComponentOperations.toolNames);
  }

  @Override
  public int hashCode() {
    return Objects.hash(actionNames, toolNames);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppComponentOperationsModel {\n");
    sb.append("    actionNames: ").append(toIndentedString(actionNames)).append("\n");
    sb.append("    toolNames: ").append(toIndentedString(toolNames)).append("\n");
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

