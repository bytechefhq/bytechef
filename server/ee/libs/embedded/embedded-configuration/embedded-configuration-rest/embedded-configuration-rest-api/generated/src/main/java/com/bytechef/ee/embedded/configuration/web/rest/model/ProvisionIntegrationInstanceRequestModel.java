package com.bytechef.ee.embedded.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.web.rest.model.ProvisionIntegrationInstanceWorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * The request to provision an integration instance.
 */

@Schema(name = "ProvisionIntegrationInstanceRequest", description = "The request to provision an integration instance.")
@JsonTypeName("ProvisionIntegrationInstanceRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T19:46:00.733909+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ProvisionIntegrationInstanceRequestModel {

  private Long integrationInstanceConfigurationId;

  private Map<String, Object> connectionParameters = new HashMap<>();

  private List<@Valid ProvisionIntegrationInstanceWorkflowModel> workflows = new ArrayList<>();

  public ProvisionIntegrationInstanceRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ProvisionIntegrationInstanceRequestModel(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public ProvisionIntegrationInstanceRequestModel integrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
    return this;
  }

  /**
   * The id of the integration instance configuration to provision onto.
   * @return integrationInstanceConfigurationId
   */
  @NotNull 
  @Schema(name = "integrationInstanceConfigurationId", description = "The id of the integration instance configuration to provision onto.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("integrationInstanceConfigurationId")
  public Long getIntegrationInstanceConfigurationId() {
    return integrationInstanceConfigurationId;
  }

  @JsonProperty("integrationInstanceConfigurationId")
  public void setIntegrationInstanceConfigurationId(Long integrationInstanceConfigurationId) {
    this.integrationInstanceConfigurationId = integrationInstanceConfigurationId;
  }

  public ProvisionIntegrationInstanceRequestModel connectionParameters(Map<String, Object> connectionParameters) {
    this.connectionParameters = connectionParameters;
    return this;
  }

  public ProvisionIntegrationInstanceRequestModel putConnectionParametersItem(String key, Object connectionParametersItem) {
    if (this.connectionParameters == null) {
      this.connectionParameters = new HashMap<>();
    }
    this.connectionParameters.put(key, connectionParametersItem);
    return this;
  }

  /**
   * The connection parameters of the integration instance.
   * @return connectionParameters
   */
  
  @Schema(name = "connectionParameters", description = "The connection parameters of the integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionParameters")
  public Map<String, Object> getConnectionParameters() {
    return connectionParameters;
  }

  @JsonProperty("connectionParameters")
  public void setConnectionParameters(Map<String, Object> connectionParameters) {
    this.connectionParameters = connectionParameters;
  }

  public ProvisionIntegrationInstanceRequestModel workflows(List<@Valid ProvisionIntegrationInstanceWorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public ProvisionIntegrationInstanceRequestModel addWorkflowsItem(ProvisionIntegrationInstanceWorkflowModel workflowsItem) {
    if (this.workflows == null) {
      this.workflows = new ArrayList<>();
    }
    this.workflows.add(workflowsItem);
    return this;
  }

  /**
   * The workflows to enable on the integration instance.
   * @return workflows
   */
  @Valid 
  @Schema(name = "workflows", description = "The workflows to enable on the integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflows")
  public List<@Valid ProvisionIntegrationInstanceWorkflowModel> getWorkflows() {
    return workflows;
  }

  @JsonProperty("workflows")
  public void setWorkflows(List<@Valid ProvisionIntegrationInstanceWorkflowModel> workflows) {
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
    ProvisionIntegrationInstanceRequestModel provisionIntegrationInstanceRequest = (ProvisionIntegrationInstanceRequestModel) o;
    return Objects.equals(this.integrationInstanceConfigurationId, provisionIntegrationInstanceRequest.integrationInstanceConfigurationId) &&
        Objects.equals(this.connectionParameters, provisionIntegrationInstanceRequest.connectionParameters) &&
        Objects.equals(this.workflows, provisionIntegrationInstanceRequest.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(integrationInstanceConfigurationId, connectionParameters, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionIntegrationInstanceRequestModel {\n");
    sb.append("    integrationInstanceConfigurationId: ").append(toIndentedString(integrationInstanceConfigurationId)).append("\n");
    sb.append("    connectionParameters: ").append(toIndentedString(connectionParameters)).append("\n");
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

