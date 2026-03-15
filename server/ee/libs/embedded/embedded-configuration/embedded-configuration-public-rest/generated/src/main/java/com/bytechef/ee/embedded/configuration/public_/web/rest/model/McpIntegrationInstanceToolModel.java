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
 * Contains user configurations for the execution of a particular MCP tool.
 */

@Schema(name = "McpIntegrationInstanceTool", description = "Contains user configurations for the execution of a particular MCP tool.")
@JsonTypeName("McpIntegrationInstanceTool")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-03-13T17:17:09.715729+01:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class McpIntegrationInstanceToolModel {

  private Boolean enabled;

  private Long mcpToolId;

  public McpIntegrationInstanceToolModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public McpIntegrationInstanceToolModel(Boolean enabled, Long mcpToolId) {
    this.enabled = enabled;
    this.mcpToolId = mcpToolId;
  }

  public McpIntegrationInstanceToolModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an MCP tool is enabled or not.
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an MCP tool is enabled or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public McpIntegrationInstanceToolModel mcpToolId(Long mcpToolId) {
    this.mcpToolId = mcpToolId;
    return this;
  }

  /**
   * The id of an MCP tool.
   * @return mcpToolId
   */
  @NotNull 
  @Schema(name = "mcpToolId", description = "The id of an MCP tool.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("mcpToolId")
  public Long getMcpToolId() {
    return mcpToolId;
  }

  public void setMcpToolId(Long mcpToolId) {
    this.mcpToolId = mcpToolId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    McpIntegrationInstanceToolModel mcpIntegrationInstanceTool = (McpIntegrationInstanceToolModel) o;
    return Objects.equals(this.enabled, mcpIntegrationInstanceTool.enabled) &&
        Objects.equals(this.mcpToolId, mcpIntegrationInstanceTool.mcpToolId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(enabled, mcpToolId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class McpIntegrationInstanceToolModel {\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    mcpToolId: ").append(toIndentedString(mcpToolId)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

