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
 * The connection to bind and what the app may do with it.
 */

@Schema(name = "AppBindingInput", description = "The connection to bind and what the app may do with it.")
@JsonTypeName("AppBindingInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:30.252991+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class AppBindingInputModel {

  private Long connectionId;

  private List<String> actionNames = new ArrayList<>();

  private List<String> toolNames = new ArrayList<>();

  private @Nullable Boolean proxyEnabled;

  private List<String> proxyAllowedMethods = new ArrayList<>();

  public AppBindingInputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AppBindingInputModel(Long connectionId) {
    this.connectionId = connectionId;
  }

  public AppBindingInputModel connectionId(Long connectionId) {
    this.connectionId = connectionId;
    return this;
  }

  /**
   * Get connectionId
   * @return connectionId
   */
  @NotNull 
  @Schema(name = "connectionId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("connectionId")
  public Long getConnectionId() {
    return connectionId;
  }

  @JsonProperty("connectionId")
  public void setConnectionId(Long connectionId) {
    this.connectionId = connectionId;
  }

  public AppBindingInputModel actionNames(List<String> actionNames) {
    this.actionNames = actionNames;
    return this;
  }

  public AppBindingInputModel addActionNamesItem(String actionNamesItem) {
    if (this.actionNames == null) {
      this.actionNames = new ArrayList<>();
    }
    this.actionNames.add(actionNamesItem);
    return this;
  }

  /**
   * The names of the actions the app may run. Defaults to none.
   * @return actionNames
   */
  
  @Schema(name = "actionNames", description = "The names of the actions the app may run. Defaults to none.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("actionNames")
  public List<String> getActionNames() {
    return actionNames;
  }

  @JsonProperty("actionNames")
  public void setActionNames(List<String> actionNames) {
    this.actionNames = actionNames;
  }

  public AppBindingInputModel toolNames(List<String> toolNames) {
    this.toolNames = toolNames;
    return this;
  }

  public AppBindingInputModel addToolNamesItem(String toolNamesItem) {
    if (this.toolNames == null) {
      this.toolNames = new ArrayList<>();
    }
    this.toolNames.add(toolNamesItem);
    return this;
  }

  /**
   * The names of the agent tools the app may run. Defaults to none.
   * @return toolNames
   */
  
  @Schema(name = "toolNames", description = "The names of the agent tools the app may run. Defaults to none.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("toolNames")
  public List<String> getToolNames() {
    return toolNames;
  }

  @JsonProperty("toolNames")
  public void setToolNames(List<String> toolNames) {
    this.toolNames = toolNames;
  }

  public AppBindingInputModel proxyEnabled(@Nullable Boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
    return this;
  }

  /**
   * Whether the app may proxy HTTP requests through the connection. Defaults to false.
   * @return proxyEnabled
   */
  
  @Schema(name = "proxyEnabled", description = "Whether the app may proxy HTTP requests through the connection. Defaults to false.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("proxyEnabled")
  public @Nullable Boolean getProxyEnabled() {
    return proxyEnabled;
  }

  @JsonProperty("proxyEnabled")
  public void setProxyEnabled(@Nullable Boolean proxyEnabled) {
    this.proxyEnabled = proxyEnabled;
  }

  public AppBindingInputModel proxyAllowedMethods(List<String> proxyAllowedMethods) {
    this.proxyAllowedMethods = proxyAllowedMethods;
    return this;
  }

  public AppBindingInputModel addProxyAllowedMethodsItem(String proxyAllowedMethodsItem) {
    if (this.proxyAllowedMethods == null) {
      this.proxyAllowedMethods = new ArrayList<>();
    }
    this.proxyAllowedMethods.add(proxyAllowedMethodsItem);
    return this;
  }

  /**
   * The HTTP methods the proxy accepts. Defaults to none.
   * @return proxyAllowedMethods
   */
  
  @Schema(name = "proxyAllowedMethods", description = "The HTTP methods the proxy accepts. Defaults to none.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    AppBindingInputModel appBindingInput = (AppBindingInputModel) o;
    return Objects.equals(this.connectionId, appBindingInput.connectionId) &&
        Objects.equals(this.actionNames, appBindingInput.actionNames) &&
        Objects.equals(this.toolNames, appBindingInput.toolNames) &&
        Objects.equals(this.proxyEnabled, appBindingInput.proxyEnabled) &&
        Objects.equals(this.proxyAllowedMethods, appBindingInput.proxyAllowedMethods);
  }

  @Override
  public int hashCode() {
    return Objects.hash(connectionId, actionNames, toolNames, proxyEnabled, proxyAllowedMethods);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AppBindingInputModel {\n");
    sb.append("    connectionId: ").append(toIndentedString(connectionId)).append("\n");
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

