package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationMoveImpactWorkflowModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ConfigurationMoveSkippedModel;
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
 * What moving connected users to another configuration would change.
 */

@Schema(name = "ConfigurationMoveImpact", description = "What moving connected users to another configuration would change.")
@JsonTypeName("ConfigurationMoveImpact")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-03T03:39:06.802257+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ConfigurationMoveImpactModel {

  private Integer mcpServerCount;

  private List<@Valid ConfigurationMoveSkippedModel> skipped = new ArrayList<>();

  private List<@Valid ConfigurationMoveImpactWorkflowModel> workflows = new ArrayList<>();

  public ConfigurationMoveImpactModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ConfigurationMoveImpactModel(Integer mcpServerCount, List<@Valid ConfigurationMoveSkippedModel> skipped, List<@Valid ConfigurationMoveImpactWorkflowModel> workflows) {
    this.mcpServerCount = mcpServerCount;
    this.skipped = skipped;
    this.workflows = workflows;
  }

  public ConfigurationMoveImpactModel mcpServerCount(Integer mcpServerCount) {
    this.mcpServerCount = mcpServerCount;
    return this;
  }

  /**
   * The number of distinct MCP servers that expose the source or the target configuration.
   * @return mcpServerCount
   */
  @NotNull 
  @Schema(name = "mcpServerCount", description = "The number of distinct MCP servers that expose the source or the target configuration.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("mcpServerCount")
  public Integer getMcpServerCount() {
    return mcpServerCount;
  }

  @JsonProperty("mcpServerCount")
  public void setMcpServerCount(Integer mcpServerCount) {
    this.mcpServerCount = mcpServerCount;
  }

  public ConfigurationMoveImpactModel skipped(List<@Valid ConfigurationMoveSkippedModel> skipped) {
    this.skipped = skipped;
    return this;
  }

  public ConfigurationMoveImpactModel addSkippedItem(ConfigurationMoveSkippedModel skippedItem) {
    if (this.skipped == null) {
      this.skipped = new ArrayList<>();
    }
    this.skipped.add(skippedItem);
    return this;
  }

  /**
   * The connected users the move would leave in place, with the reason.
   * @return skipped
   */
  @NotNull 
  @Schema(name = "skipped", description = "The connected users the move would leave in place, with the reason.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("skipped")
  public List<@Valid ConfigurationMoveSkippedModel> getSkipped() {
    return skipped;
  }

  @JsonProperty("skipped")
  public void setSkipped(List<@Valid ConfigurationMoveSkippedModel> skipped) {
    this.skipped = skipped;
  }

  public ConfigurationMoveImpactModel workflows(List<@Valid ConfigurationMoveImpactWorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public ConfigurationMoveImpactModel addWorkflowsItem(ConfigurationMoveImpactWorkflowModel workflowsItem) {
    if (this.workflows == null) {
      this.workflows = new ArrayList<>();
    }
    this.workflows.add(workflowsItem);
    return this;
  }

  /**
   * The workflows of the target that need new required inputs or connections.
   * @return workflows
   */
  @NotNull 
  @Schema(name = "workflows", description = "The workflows of the target that need new required inputs or connections.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("workflows")
  public List<@Valid ConfigurationMoveImpactWorkflowModel> getWorkflows() {
    return workflows;
  }

  @JsonProperty("workflows")
  public void setWorkflows(List<@Valid ConfigurationMoveImpactWorkflowModel> workflows) {
    this.workflows = workflows;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ConfigurationMoveImpactModel configurationMoveImpact = (ConfigurationMoveImpactModel) o;
    return Objects.equals(this.mcpServerCount, configurationMoveImpact.mcpServerCount) &&
        Objects.equals(this.skipped, configurationMoveImpact.skipped) &&
        Objects.equals(this.workflows, configurationMoveImpact.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(mcpServerCount, skipped, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ConfigurationMoveImpactModel {\n");
    sb.append("    mcpServerCount: ").append(toIndentedString(mcpServerCount)).append("\n");
    sb.append("    skipped: ").append(toIndentedString(skipped)).append("\n");
    sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
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

