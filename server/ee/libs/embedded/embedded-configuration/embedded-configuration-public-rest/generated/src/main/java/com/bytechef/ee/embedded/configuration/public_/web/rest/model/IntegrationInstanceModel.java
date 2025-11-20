package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CredentialStatusModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.IntegrationInstanceWorkflowModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * The integration instance represents a configured integration for a specific user, containing connection and status information
 */

@Schema(name = "IntegrationInstance", description = "The integration instance represents a configured integration for a specific user, containing connection and status information")
@JsonTypeName("IntegrationInstance")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-11-20T06:27:31.516923+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class IntegrationInstanceModel {

  private @Nullable Long id;

  private @Nullable CredentialStatusModel credentialStatus;

  private Boolean enabled;

  @Valid
  private List<@Valid IntegrationInstanceWorkflowModel> workflows = new ArrayList<>();

  public IntegrationInstanceModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationInstanceModel(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration instance.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an integration instance.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public IntegrationInstanceModel credentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  @Valid 
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("credentialStatus")
  public @Nullable CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(@Nullable CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public IntegrationInstanceModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration instance is enabled or not
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an integration instance is enabled or not", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationInstanceModel workflows(List<@Valid IntegrationInstanceWorkflowModel> workflows) {
    this.workflows = workflows;
    return this;
  }

  public IntegrationInstanceModel addWorkflowsItem(IntegrationInstanceWorkflowModel workflowsItem) {
    if (this.workflows == null) {
      this.workflows = new ArrayList<>();
    }
    this.workflows.add(workflowsItem);
    return this;
  }

  /**
   * Get workflows
   * @return workflows
   */
  @Valid 
  @Schema(name = "workflows", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("workflows")
  public List<@Valid IntegrationInstanceWorkflowModel> getWorkflows() {
    return workflows;
  }

  public void setWorkflows(List<@Valid IntegrationInstanceWorkflowModel> workflows) {
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
    IntegrationInstanceModel integrationInstance = (IntegrationInstanceModel) o;
    return Objects.equals(this.id, integrationInstance.id) &&
        Objects.equals(this.credentialStatus, integrationInstance.credentialStatus) &&
        Objects.equals(this.enabled, integrationInstance.enabled) &&
        Objects.equals(this.workflows, integrationInstance.workflows);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, credentialStatus, enabled, workflows);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationInstanceModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    workflows: ").append(toIndentedString(workflows)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

