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
 * A connection bound to an app under a binding key.
 */

@Schema(name = "AppBinding", description = "A connection bound to an app under a binding key.")
@JsonTypeName("AppBinding")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppBindingModel {

  private @Nullable Long id;

  private @Nullable Long appId;

  private @Nullable String bindingKey;

  private @Nullable Long connectionId;

  private @Nullable String connectionName;

  private @Nullable String componentName;

  private @Nullable Integer connectionVersion;

  private List<String> actionNames = new ArrayList<>();

  private List<String> toolNames = new ArrayList<>();

  private @Nullable Boolean proxyEnabled;

  private List<String> proxyAllowedMethods = new ArrayList<>();

  public AppBindingModel id(@Nullable Long id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable Long getId() {
    return id;
  }

  @JsonProperty("id")
  public void setId(@Nullable Long id) {
    this.id = id;
  }

  public AppBindingModel appId(@Nullable Long appId) {
    this.appId = appId;
    return this;
  }

  /**
   * Get appId
   * @return appId
   */
  
  @Schema(name = "appId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appId")
  public @Nullable Long getAppId() {
    return appId;
  }

  @JsonProperty("appId")
  public void setAppId(@Nullable Long appId) {
    this.appId = appId;
  }

  public AppBindingModel bindingKey(@Nullable String bindingKey) {
    this.bindingKey = bindingKey;
    return this;
  }

  /**
   * Get bindingKey
   * @return bindingKey
   */
  
  @Schema(name = "bindingKey", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("bindingKey")
  public @Nullable String getBindingKey() {
    return bindingKey;
  }

  @JsonProperty("bindingKey")
  public void setBindingKey(@Nullable String bindingKey) {
    this.bindingKey = bindingKey;
  }

  public AppBindingModel connectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * Get connectionId
   * @return connectionId
   */
  
  @Schema(name = "connectionId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionId")
  public @Nullable Long getConnectionId() {
    return connectionId;
  }

  @JsonProperty("connectionId")
  public void setConnectionId(@Nullable Long connectionId) {
    this.connectionId = connectionId;
  }

  public AppBindingModel connectionName(@Nullable String connectionName) {
    this.connectionName = connectionName;
    return this;
  }

  /**
   * Get connectionName
   * @return connectionName
   */
  
  @Schema(name = "connectionName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionName")
  public @Nullable String getConnectionName() {
    return connectionName;
  }

  @JsonProperty("connectionName")
  public void setConnectionName(@Nullable String connectionName) {
    this.connectionName = connectionName;
  }

  public AppBindingModel componentName(@Nullable String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public @Nullable String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(@Nullable String componentName) {
    this.componentName = componentName;
  }

  public AppBindingModel connectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * Get connectionVersion
   * @return connectionVersion
   */
  
  @Schema(name = "connectionVersion", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("connectionVersion")
  public @Nullable Integer getConnectionVersion() {
    return connectionVersion;
  }

  @JsonProperty("connectionVersion")
  public void setConnectionVersion(@Nullable Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public AppBindingModel actionNames(List<String> actionNames) {
    this.actionNames = actionNames;
    return this;
  }

  public AppBindingModel addActionNamesItem(String actionNamesItem) {
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

  public AppBindingModel toolNames(List<String> toolNames) {
    this.toolNames = toolNames;
    return this;
  }

  public AppBindingModel addToolNamesItem(String toolNamesItem) {
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

  public AppBindingModel proxyEnabled(@Nullable Boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  /**
   * Get proxyEnabled
   * @return proxyEnabled
   */
  
  @Schema(name = "proxyEnabled", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("proxyEnabled")
  public @Nullable Boolean getProxyEnabled() {
    return proxyEnabled;
  }

  @JsonProperty("proxyEnabled")
  public void setProxyEnabled(@Nullable Boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
  }

  public AppBindingModel proxyAllowedMethods(List<String> proxyAllowedMethods) {
    this.proxyAllowedMethods = proxyAllowedMethods;
    return this;
  }

  public AppBindingModel addProxyAllowedMethodsItem(String proxyAllowedMethodsItem) {
    if (this.proxyAllowedMethods == null) {
      this.proxyAllowedMethods = new ArrayList<>();
    }
    this.proxyAllowedMethods.add(proxyAllowedMethodsItem);
    return this;
  }

  /**
   * Get proxyAllowedMethods
   * @return proxyAllowedMethods
   */
  
  @Schema(name = "proxyAllowedMethods", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("proxyAllowedMethods")
  public List<String> getProxyAllowedMethods() {
    return proxyAllowedMethods;
  }

  @JsonProperty("proxyAllowedMethods")
  public void setProxyAllowedMethods(List<String> proxyAllowedMethods) {
    this.proxyAllowedMethods = proxyAllowedMethods;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AppBindingModel appBinding = (AppBindingModel) o;
    return Objects.equals(this.id, appBinding.id) &&
        Objects.equals(this.appId, appBinding.appId) &&
        Objects.equals(this.bindingKey, appBinding.bindingKey) &&
        Objects.equals(this.connectionId, appBinding.connectionId) &&
        Objects.equals(this.connectionName, appBinding.connectionName) &&
        Objects.equals(this.componentName, appBinding.componentName) &&
        Objects.equals(this.connectionVersion, appBinding.connectionVersion) &&
        Objects.equals(this.actionNames, appBinding.actionNames) &&
        Objects.equals(this.toolNames, appBinding.toolNames) &&
        Objects.equals(this.proxyEnabled, appBinding.proxyEnabled) &&
        Objects.equals(this.proxyAllowedMethods, appBinding.proxyAllowedMethods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, appId, bindingKey, connectionId, connectionName, componentName, connectionVersion, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppBindingModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    appId: ").append(toIndentedString(appId)).append("\n");
    sb.append("    bindingKey: ").append(toIndentedString(bindingKey)).append("\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
    sb.append("    connectionName: ").append(toIndentedString(connectionName)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    actionNames: ").append(toIndentedString(actionNames)).append("\n");
    sb.append("    toolNames: ").append(toIndentedString(toolNames)).append("\n");
    sb.append("    proxyEnabled: ").append(toIndentedString(proxyEnabled)).append("\n");
    sb.append("    proxyAllowedMethods: ").append(toIndentedString(proxyAllowedMethods)).append("\n");
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

