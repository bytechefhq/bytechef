package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
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
 * ComponentInputOptionsRequestModel
 */

@JsonTypeName("ComponentInputOptionsRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-03T17:58:15.470562+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ComponentInputOptionsRequestModel {

  private String componentName;

  private Integer componentVersion;

  private String groupName;

  private String propertyName;

  @Valid
  private Map<String, Object> lookupDependsOnValues = new HashMap<>();

  private @Nullable String searchText;

  public ComponentInputOptionsRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentInputOptionsRequestModel(String componentName, Integer componentVersion, String groupName, String propertyName) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.groupName = groupName;
    this.propertyName = propertyName;
  }

  public ComponentInputOptionsRequestModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The component the input group belongs to.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The component the input group belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public ComponentInputOptionsRequestModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The component version.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ComponentInputOptionsRequestModel groupName(String groupName) {
    this.groupName = groupName;
    return this;
  }

  /**
   * The component input group the property belongs to.
   * @return groupName
   */
  @NotNull 
  @Schema(name = "groupName", description = "The component input group the property belongs to.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("groupName")
  public String getGroupName() {
    return groupName;
  }

  @JsonProperty("groupName")
  public void setGroupName(String groupName) {
    this.groupName = groupName;
  }

  public ComponentInputOptionsRequestModel propertyName(String propertyName) {
    this.propertyName = propertyName;
    return this;
  }

  /**
   * The group member property whose options to resolve.
   * @return propertyName
   */
  @NotNull 
  @Schema(name = "propertyName", description = "The group member property whose options to resolve.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("propertyName")
  public String getPropertyName() {
    return propertyName;
  }

  @JsonProperty("propertyName")
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public ComponentInputOptionsRequestModel lookupDependsOnValues(Map<String, Object> lookupDependsOnValues) {
    this.lookupDependsOnValues = lookupDependsOnValues;
    return this;
  }

  public ComponentInputOptionsRequestModel putLookupDependsOnValuesItem(String key, Object lookupDependsOnValuesItem) {
    if (this.lookupDependsOnValues == null) {
      this.lookupDependsOnValues = new HashMap<>();
    }
    this.lookupDependsOnValues.put(key, lookupDependsOnValuesItem);
    return this;
  }

  /**
   * Current values of the properties this lookup depends on.
   * @return lookupDependsOnValues
   */
  
  @Schema(name = "lookupDependsOnValues", description = "Current values of the properties this lookup depends on.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("lookupDependsOnValues")
  public Map<String, Object> getLookupDependsOnValues() {
    return lookupDependsOnValues;
  }

  @JsonProperty("lookupDependsOnValues")
  public void setLookupDependsOnValues(Map<String, Object> lookupDependsOnValues) {
    this.lookupDependsOnValues = lookupDependsOnValues;
  }

  public ComponentInputOptionsRequestModel searchText(@Nullable String searchText) {
    this.searchText = searchText;
    return this;
  }

  /**
   * Get searchText
   * @return searchText
   */
  
  @Schema(name = "searchText", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("searchText")
  public @Nullable String getSearchText() {
    return searchText;
  }

  @JsonProperty("searchText")
  public void setSearchText(@Nullable String searchText) {
    this.searchText = searchText;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentInputOptionsRequestModel componentInputOptionsRequest = (ComponentInputOptionsRequestModel) o;
    return Objects.equals(this.componentName, componentInputOptionsRequest.componentName) &&
        Objects.equals(this.componentVersion, componentInputOptionsRequest.componentVersion) &&
        Objects.equals(this.groupName, componentInputOptionsRequest.groupName) &&
        Objects.equals(this.propertyName, componentInputOptionsRequest.propertyName) &&
        Objects.equals(this.lookupDependsOnValues, componentInputOptionsRequest.lookupDependsOnValues) &&
        Objects.equals(this.searchText, componentInputOptionsRequest.searchText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion, groupName, propertyName, lookupDependsOnValues, searchText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentInputOptionsRequestModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    groupName: ").append(toIndentedString(groupName)).append("\n");
    sb.append("    propertyName: ").append(toIndentedString(propertyName)).append("\n");
    sb.append("    lookupDependsOnValues: ").append(toIndentedString(lookupDependsOnValues)).append("\n");
    sb.append("    searchText: ").append(toIndentedString(searchText)).append("\n");
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

