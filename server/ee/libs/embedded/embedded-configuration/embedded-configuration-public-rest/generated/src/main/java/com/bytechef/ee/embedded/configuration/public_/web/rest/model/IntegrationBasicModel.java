package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.CredentialStatusModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A group of workflows that make one logical integration.
 */

@Schema(name = "IntegrationBasic", description = "A group of workflows that make one logical integration.")
@JsonTypeName("IntegrationBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-06-06T07:17:00.413223+02:00[Europe/Zagreb]", comments = "Generator version: 7.12.0")
public class IntegrationBasicModel {

  private String componentName;

  private CredentialStatusModel credentialStatus;

  private @Nullable String description;

  private Boolean enabled;

  private String icon;

  private @Nullable Long id;

  private @Nullable Integer integrationVersion;

  private Boolean multipleInstances = false;

  private @Nullable String title;

  public IntegrationBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public IntegrationBasicModel(String componentName, CredentialStatusModel credentialStatus, Boolean enabled, String icon, Boolean multipleInstances) {
    this.componentName = componentName;
    this.credentialStatus = credentialStatus;
    this.enabled = enabled;
    this.icon = icon;
    this.multipleInstances = multipleInstances;
  }

  public IntegrationBasicModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the integration's component.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the integration's component.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public IntegrationBasicModel credentialStatus(CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
    return this;
  }

  /**
   * Get credentialStatus
   * @return credentialStatus
   */
  @NotNull @Valid 
  @Schema(name = "credentialStatus", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("credentialStatus")
  public CredentialStatusModel getCredentialStatus() {
    return credentialStatus;
  }

  public void setCredentialStatus(CredentialStatusModel credentialStatus) {
    this.credentialStatus = credentialStatus;
  }

  public IntegrationBasicModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description of a project.
   * @return description
   */
  
  @Schema(name = "description", description = "The description of a project.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public IntegrationBasicModel enabled(Boolean enabled) {
    this.enabled = enabled;
    return this;
  }

  /**
   * If an integration is enabled or not
   * @return enabled
   */
  @NotNull 
  @Schema(name = "enabled", description = "If an integration is enabled or not", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("enabled")
  public Boolean getEnabled() {
    return enabled;
  }

  public void setEnabled(Boolean enabled) {
    this.enabled = enabled;
  }

  public IntegrationBasicModel icon(String icon) {
    this.icon = icon;
    return this;
  }

  /**
   * The icon.
   * @return icon
   */
  @NotNull 
  @Schema(name = "icon", description = "The icon.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("icon")
  public String getIcon() {
    return icon;
  }

  public void setIcon(String icon) {
    this.icon = icon;
  }

  public IntegrationBasicModel id(Long id) {
    this.id = id;
    return this;
  }

  /**
   * The id of an integration.
   * @return id
   */
  
  @Schema(name = "id", description = "The id of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public Long getId() {
    return id;
  }

  public void setId(Long id) {
    this.id = id;
  }

  public IntegrationBasicModel integrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
    return this;
  }

  /**
   * The version of an integration.
   * @return integrationVersion
   */
  
  @Schema(name = "integrationVersion", description = "The version of an integration.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("integrationVersion")
  public Integer getIntegrationVersion() {
    return integrationVersion;
  }

  public void setIntegrationVersion(Integer integrationVersion) {
    this.integrationVersion = integrationVersion;
  }

  public IntegrationBasicModel multipleInstances(Boolean multipleInstances) {
    this.multipleInstances = multipleInstances;
    return this;
  }

  /**
   * If multiple instances of an integration are allowed or not.
   * @return multipleInstances
   */
  @NotNull 
  @Schema(name = "multipleInstances", description = "If multiple instances of an integration are allowed or not.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("multipleInstances")
  public Boolean getMultipleInstances() {
    return multipleInstances;
  }

  public void setMultipleInstances(Boolean multipleInstances) {
    this.multipleInstances = multipleInstances;
  }

  public IntegrationBasicModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title of the integration's component.
   * @return title
   */
  
  @Schema(name = "title", description = "The title of the integration's component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    IntegrationBasicModel integrationBasic = (IntegrationBasicModel) o;
    return Objects.equals(this.componentName, integrationBasic.componentName) &&
        Objects.equals(this.credentialStatus, integrationBasic.credentialStatus) &&
        Objects.equals(this.description, integrationBasic.description) &&
        Objects.equals(this.enabled, integrationBasic.enabled) &&
        Objects.equals(this.icon, integrationBasic.icon) &&
        Objects.equals(this.id, integrationBasic.id) &&
        Objects.equals(this.integrationVersion, integrationBasic.integrationVersion) &&
        Objects.equals(this.multipleInstances, integrationBasic.multipleInstances) &&
        Objects.equals(this.title, integrationBasic.title);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, credentialStatus, description, enabled, icon, id, integrationVersion, multipleInstances, title);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class IntegrationBasicModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    credentialStatus: ").append(toIndentedString(credentialStatus)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    enabled: ").append(toIndentedString(enabled)).append("\n");
    sb.append("    icon: ").append(toIndentedString(icon)).append("\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    integrationVersion: ").append(toIndentedString(integrationVersion)).append("\n");
    sb.append("    multipleInstances: ").append(toIndentedString(multipleInstances)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

