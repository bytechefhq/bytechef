package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.IntegrationBasicModel;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionableIntegrationInstanceConfigurationModel;
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
 * An integration that can be provisioned to a connected user.
 */

@Schema(name = "ProvisionableIntegration", description = "An integration that can be provisioned to a connected user.")
@JsonTypeName("ProvisionableIntegration")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-13T10:21:19.930018+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ProvisionableIntegrationModel {

  private @Nullable IntegrationBasicModel integration;

  private @Nullable Integer componentVersion;

  private List<@Valid ProvisionableIntegrationInstanceConfigurationModel> integrationInstanceConfigurations = new ArrayList<>();

  private List<String> mcpWorkflowUuids = new ArrayList<>();

  private List<String> toolOnlyMcpWorkflowUuids = new ArrayList<>();

  public ProvisionableIntegrationModel integration(@Nullable IntegrationBasicModel integration) {
    this.integration = integration;
    return this;
  }

  /**
   * Get integration
   * @return integration
   */
  @Valid 
  @Schema(name = "integration", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integration")
  public @Nullable IntegrationBasicModel getIntegration() {
    return integration;
  }

  @JsonProperty("integration")
  public void setIntegration(@Nullable IntegrationBasicModel integration) {
    this.integration = integration;
  }

  public ProvisionableIntegrationModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the integration's component.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The version of the integration's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ProvisionableIntegrationModel integrationInstanceConfigurations(List<@Valid ProvisionableIntegrationInstanceConfigurationModel> integrationInstanceConfigurations) {
    this.integrationInstanceConfigurations = integrationInstanceConfigurations;
    return this;
  }

  public ProvisionableIntegrationModel addIntegrationInstanceConfigurationsItem(ProvisionableIntegrationInstanceConfigurationModel integrationInstanceConfigurationsItem) {
    if (this.integrationInstanceConfigurations == null) {
      this.integrationInstanceConfigurations = new ArrayList<>();
    }
    this.integrationInstanceConfigurations.add(integrationInstanceConfigurationsItem);
    return this;
  }

  /**
   * Get integrationInstanceConfigurations
   * @return integrationInstanceConfigurations
   */
  
  @Schema(name = "integrationInstanceConfigurations", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurations")
  public List<@Valid ProvisionableIntegrationInstanceConfigurationModel> getIntegrationInstanceConfigurations() {
    return integrationInstanceConfigurations;
  }

  @JsonProperty("integrationInstanceConfigurations")
  public void setIntegrationInstanceConfigurations(List<@Valid ProvisionableIntegrationInstanceConfigurationModel> integrationInstanceConfigurations) {
    this.integrationInstanceConfigurations = integrationInstanceConfigurations;
  }

  public ProvisionableIntegrationModel mcpWorkflowUuids(List<String> mcpWorkflowUuids) {
    this.mcpWorkflowUuids = mcpWorkflowUuids;
    return this;
  }

  public ProvisionableIntegrationModel addMcpWorkflowUuidsItem(String mcpWorkflowUuidsItem) {
    if (this.mcpWorkflowUuids == null) {
      this.mcpWorkflowUuids = new ArrayList<>();
    }
    this.mcpWorkflowUuids.add(mcpWorkflowUuidsItem);
    return this;
  }

  /**
   * The uuids of the integration's workflows that embedded MCP servers expose as tools and that also have another trigger.
   * @return mcpWorkflowUuids
   */
  
  @Schema(name = "mcpWorkflowUuids", description = "The uuids of the integration's workflows that embedded MCP servers expose as tools and that also have another trigger.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("mcpWorkflowUuids")
  public List<String> getMcpWorkflowUuids() {
    return mcpWorkflowUuids;
  }

  @JsonProperty("mcpWorkflowUuids")
  public void setMcpWorkflowUuids(List<String> mcpWorkflowUuids) {
    this.mcpWorkflowUuids = mcpWorkflowUuids;
  }

  public ProvisionableIntegrationModel toolOnlyMcpWorkflowUuids(List<String> toolOnlyMcpWorkflowUuids) {
    this.toolOnlyMcpWorkflowUuids = toolOnlyMcpWorkflowUuids;
    return this;
  }

  public ProvisionableIntegrationModel addToolOnlyMcpWorkflowUuidsItem(String toolOnlyMcpWorkflowUuidsItem) {
    if (this.toolOnlyMcpWorkflowUuids == null) {
      this.toolOnlyMcpWorkflowUuids = new ArrayList<>();
    }
    this.toolOnlyMcpWorkflowUuids.add(toolOnlyMcpWorkflowUuidsItem);
    return this;
  }

  /**
   * The uuids of the integration's workflows that embedded MCP servers expose as tools and that have no other trigger.
   * @return toolOnlyMcpWorkflowUuids
   */
  
  @Schema(name = "toolOnlyMcpWorkflowUuids", description = "The uuids of the integration's workflows that embedded MCP servers expose as tools and that have no other trigger.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolOnlyMcpWorkflowUuids")
  public List<String> getToolOnlyMcpWorkflowUuids() {
    return toolOnlyMcpWorkflowUuids;
  }

  @JsonProperty("toolOnlyMcpWorkflowUuids")
  public void setToolOnlyMcpWorkflowUuids(List<String> toolOnlyMcpWorkflowUuids) {
    this.toolOnlyMcpWorkflowUuids = toolOnlyMcpWorkflowUuids;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ProvisionableIntegrationModel provisionableIntegration = (ProvisionableIntegrationModel) o;
    return Objects.equals(this.integration, provisionableIntegration.integration) &&
        Objects.equals(this.componentVersion, provisionableIntegration.componentVersion) &&
        Objects.equals(this.integrationInstanceConfigurations, provisionableIntegration.integrationInstanceConfigurations) &&
        Objects.equals(this.mcpWorkflowUuids, provisionableIntegration.mcpWorkflowUuids) &&
        Objects.equals(this.toolOnlyMcpWorkflowUuids, provisionableIntegration.toolOnlyMcpWorkflowUuids);
  }

  @Override
  public int hashCode() {
    return Objects.hash(integration, componentVersion, integrationInstanceConfigurations, mcpWorkflowUuids, toolOnlyMcpWorkflowUuids);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionableIntegrationModel {\n");
    sb.append("    integration: ").append(toIndentedString(integration)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    integrationInstanceConfigurations: ").append(toIndentedString(integrationInstanceConfigurations)).append("\n");
    sb.append("    mcpWorkflowUuids: ").append(toIndentedString(mcpWorkflowUuids)).append("\n");
    sb.append("    toolOnlyMcpWorkflowUuids: ").append(toIndentedString(toolOnlyMcpWorkflowUuids)).append("\n");
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

