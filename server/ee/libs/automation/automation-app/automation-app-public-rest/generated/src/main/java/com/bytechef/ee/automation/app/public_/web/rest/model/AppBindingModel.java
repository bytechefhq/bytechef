package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A connection bound to the calling app and what the app may do through it.
 */

@Schema(name = "AppBinding", description = "A connection bound to the calling app and what the app may do through it.")
@JsonTypeName("AppBinding")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppBindingModel {

  private String bindingKey;

  private String componentName;

  private Integer connectionVersion;

  private JsonNullable<Integer> componentVersion = JsonNullable.<Integer>undefined();

  private List<String> actionNames = new ArrayList<>();

  private List<String> toolNames = new ArrayList<>();

  private Boolean proxyEnabled;

  private List<String> proxyAllowedMethods = new ArrayList<>();

  public AppBindingModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppBindingModel(String bindingKey, String componentName, Integer connectionVersion, List<String> actionNames, List<String> toolNames, Boolean proxyEnabled, List<String> proxyAllowedMethods) {
    this.bindingKey = bindingKey;
    this.componentName = componentName;
    this.connectionVersion = connectionVersion;
    this.actionNames = actionNames;
    this.toolNames = toolNames;
    this.proxyEnabled = proxyEnabled;
    this.proxyAllowedMethods = proxyAllowedMethods;
  }

  public AppBindingModel bindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
    return this;
  }

  /**
   * Get bindingKey
   * @return bindingKey
   */
  @NotNull 
  @Schema(name = "bindingKey", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("bindingKey")
  public String getBindingKey() {
    return bindingKey;
  }

  @JsonProperty("bindingKey")
  public void setBindingKey(String bindingKey) {
    this.bindingKey = bindingKey;
  }

  public AppBindingModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * Get componentName
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public AppBindingModel connectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
    return this;
  }

  /**
   * Get connectionVersion
   * @return connectionVersion
   */
  @NotNull 
  @Schema(name = "connectionVersion", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionVersion")
  public Integer getConnectionVersion() {
    return connectionVersion;
  }

  @JsonProperty("connectionVersion")
  public void setConnectionVersion(Integer connectionVersion) {
    this.connectionVersion = connectionVersion;
  }

  public AppBindingModel componentVersion(Integer componentVersion) {
    this.componentVersion = JsonNullable.of(componentVersion);
    return this;
  }

  /**
   * The latest version of the connection's component: the version actions and tools run on and whose operations the binding offers. Null when the component cannot be resolved (deleted, dropped in an upgrade, or missing from the component index); calls through such a binding fail.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The latest version of the connection's component: the version actions and tools run on and whose operations the binding offers. Null when the component cannot be resolved (deleted, dropped in an upgrade, or missing from the component index); calls through such a binding fail.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("componentVersion")
  public JsonNullable<Integer> getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(JsonNullable<Integer> componentVersion) {
    this.componentVersion = componentVersion;
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
  @NotNull 
  @Schema(name = "actionNames", requiredMode = Schema.RequiredMode.REQUIRED)
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
  @NotNull 
  @Schema(name = "toolNames", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("toolNames")
  public List<String> getToolNames() {
    return toolNames;
  }

  @JsonProperty("toolNames")
  public void setToolNames(List<String> toolNames) {
    this.toolNames = toolNames;
  }

  public AppBindingModel proxyEnabled(Boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  /**
   * Get proxyEnabled
   * @return proxyEnabled
   */
  @NotNull 
  @Schema(name = "proxyEnabled", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("proxyEnabled")
  public Boolean getProxyEnabled() {
    return proxyEnabled;
  }

  @JsonProperty("proxyEnabled")
  public void setProxyEnabled(Boolean proxyEnabled) {
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
  @NotNull 
  @Schema(name = "proxyAllowedMethods", requiredMode = Schema.RequiredMode.REQUIRED)
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
    return Objects.equals(this.bindingKey, appBinding.bindingKey) &&
        Objects.equals(this.componentName, appBinding.componentName) &&
        Objects.equals(this.connectionVersion, appBinding.connectionVersion) &&
        equalsNullable(this.componentVersion, appBinding.componentVersion) &&
        Objects.equals(this.actionNames, appBinding.actionNames) &&
        Objects.equals(this.toolNames, appBinding.toolNames) &&
        Objects.equals(this.proxyEnabled, appBinding.proxyEnabled) &&
        Objects.equals(this.proxyAllowedMethods, appBinding.proxyAllowedMethods);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(bindingKey, componentName, connectionVersion, hashCodeNullable(componentVersion), actionNames, toolNames, proxyEnabled, proxyAllowedMethods);
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppBindingModel {\n");
    sb.append("    bindingKey: ").append(toIndentedString(bindingKey)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    connectionVersion: ").append(toIndentedString(connectionVersion)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
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

