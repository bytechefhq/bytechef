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
 * WorkflowInputOptionsRequestModel
 */

@JsonTypeName("WorkflowInputOptionsRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-01T09:51:24.564853+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class WorkflowInputOptionsRequestModel {

  private String inputName;

  private String propertyName;

  @Valid
  private Map<String, Object> lookupDependsOnValues = new HashMap<>();

  private @Nullable String searchText;

  public WorkflowInputOptionsRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public WorkflowInputOptionsRequestModel(String inputName, String propertyName) {
    this.inputName = inputName;
    this.propertyName = propertyName;
  }

  public WorkflowInputOptionsRequestModel inputName(String inputName) {
    this.inputName = inputName;
    return this;
  }

  /**
   * The workflow input whose referenced component the options resolve against.
   * @return inputName
   */
  @NotNull 
  @Schema(name = "inputName", description = "The workflow input whose referenced component the options resolve against.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("inputName")
  public String getInputName() {
    return inputName;
  }

  @JsonProperty("inputName")
  public void setInputName(String inputName) {
    this.inputName = inputName;
  }

  public WorkflowInputOptionsRequestModel propertyName(String propertyName) {
    this.propertyName = propertyName;
    return this;
  }

  /**
   * The component property whose options to resolve.
   * @return propertyName
   */
  @NotNull 
  @Schema(name = "propertyName", description = "The component property whose options to resolve.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("propertyName")
  public String getPropertyName() {
    return propertyName;
  }

  @JsonProperty("propertyName")
  public void setPropertyName(String propertyName) {
    this.propertyName = propertyName;
  }

  public WorkflowInputOptionsRequestModel lookupDependsOnValues(Map<String, Object> lookupDependsOnValues) {
    this.lookupDependsOnValues = lookupDependsOnValues;
    return this;
  }

  public WorkflowInputOptionsRequestModel putLookupDependsOnValuesItem(String key, Object lookupDependsOnValuesItem) {
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

  public WorkflowInputOptionsRequestModel searchText(@Nullable String searchText) {
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
    WorkflowInputOptionsRequestModel workflowInputOptionsRequest = (WorkflowInputOptionsRequestModel) o;
    return Objects.equals(this.inputName, workflowInputOptionsRequest.inputName) &&
        Objects.equals(this.propertyName, workflowInputOptionsRequest.propertyName) &&
        Objects.equals(this.lookupDependsOnValues, workflowInputOptionsRequest.lookupDependsOnValues) &&
        Objects.equals(this.searchText, workflowInputOptionsRequest.searchText);
  }

  @Override
  public int hashCode() {
    return Objects.hash(inputName, propertyName, lookupDependsOnValues, searchText);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class WorkflowInputOptionsRequestModel {\n");
    sb.append("    inputName: ").append(toIndentedString(inputName)).append("\n");
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

