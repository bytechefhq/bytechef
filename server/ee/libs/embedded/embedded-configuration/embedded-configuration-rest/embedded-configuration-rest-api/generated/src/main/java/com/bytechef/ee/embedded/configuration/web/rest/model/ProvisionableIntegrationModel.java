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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-23T19:46:00.733909+02:00[Europe/Zagreb]", comments = "Generator version: 7.25.0")
public class ProvisionableIntegrationModel {

  private @Nullable IntegrationBasicModel integration;

  private @Nullable Integer componentVersion;

  private List<@Valid ProvisionableIntegrationInstanceConfigurationModel> integrationInstanceConfigurations = new ArrayList<>();

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
  @Valid 
  @Schema(name = "integrationInstanceConfigurations", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationInstanceConfigurations")
  public List<@Valid ProvisionableIntegrationInstanceConfigurationModel> getIntegrationInstanceConfigurations() {
    return integrationInstanceConfigurations;
  }

  @JsonProperty("integrationInstanceConfigurations")
  public void setIntegrationInstanceConfigurations(List<@Valid ProvisionableIntegrationInstanceConfigurationModel> integrationInstanceConfigurations) {
    this.integrationInstanceConfigurations = integrationInstanceConfigurations;
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
        Objects.equals(this.integrationInstanceConfigurations, provisionableIntegration.integrationInstanceConfigurations);
  }

  @Override
  public int hashCode() {
    return Objects.hash(integration, componentVersion, integrationInstanceConfigurations);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ProvisionableIntegrationModel {\n");
    sb.append("    integration: ").append(toIndentedString(integration)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    integrationInstanceConfigurations: ").append(toIndentedString(integrationInstanceConfigurations)).append("\n");
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

