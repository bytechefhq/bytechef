package com.bytechef.platform.workflow.execution.web.rest.model;

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
 * ApprovalFormInputModel
 */

@JsonTypeName("ApprovalFormInput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-27T14:10:01.371326+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class ApprovalFormInputModel {

  private @Nullable String defaultValue;

  private @Nullable String fieldDescription;

  private @Nullable String fieldLabel;

  private @Nullable String fieldName;

  @Valid
  private List<com.bytechef.platform.configuration.web.rest.model.@Valid FieldOptionModel> fieldOptions = new ArrayList<>();

  private @Nullable Integer fieldType;

  private @Nullable Integer maxSelection;

  private @Nullable Integer minSelection;

  private @Nullable Boolean multipleChoice;

  private @Nullable String placeholder;

  private @Nullable Boolean required;

  public ApprovalFormInputModel defaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * Get defaultValue
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable String getDefaultValue() {
    return defaultValue;
  }

  @JsonProperty("defaultValue")
  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public ApprovalFormInputModel fieldDescription(@Nullable String fieldDescription) {
    this.fieldDescription = fieldDescription;
    return this;
  }

  /**
   * Get fieldDescription
   * @return fieldDescription
   */
  
  @Schema(name = "fieldDescription", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fieldDescription")
  public @Nullable String getFieldDescription() {
    return fieldDescription;
  }

  @JsonProperty("fieldDescription")
  public void setFieldDescription(@Nullable String fieldDescription) {
    this.fieldDescription = fieldDescription;
  }

  public ApprovalFormInputModel fieldLabel(@Nullable String fieldLabel) {
    this.fieldLabel = fieldLabel;
    return this;
  }

  /**
   * Get fieldLabel
   * @return fieldLabel
   */
  
  @Schema(name = "fieldLabel", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fieldLabel")
  public @Nullable String getFieldLabel() {
    return fieldLabel;
  }

  @JsonProperty("fieldLabel")
  public void setFieldLabel(@Nullable String fieldLabel) {
    this.fieldLabel = fieldLabel;
  }

  public ApprovalFormInputModel fieldName(@Nullable String fieldName) {
    this.fieldName = fieldName;
    return this;
  }

  /**
   * Get fieldName
   * @return fieldName
   */
  
  @Schema(name = "fieldName", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fieldName")
  public @Nullable String getFieldName() {
    return fieldName;
  }

  @JsonProperty("fieldName")
  public void setFieldName(@Nullable String fieldName) {
    this.fieldName = fieldName;
  }

  public ApprovalFormInputModel fieldOptions(List<com.bytechef.platform.configuration.web.rest.model.@Valid FieldOptionModel> fieldOptions) {
    this.fieldOptions = fieldOptions;
    return this;
  }

  public ApprovalFormInputModel addFieldOptionsItem(com.bytechef.platform.configuration.web.rest.model.FieldOptionModel fieldOptionsItem) {
    if (this.fieldOptions == null) {
      this.fieldOptions = new ArrayList<>();
    }
    this.fieldOptions.add(fieldOptionsItem);
    return this;
  }

  /**
   * Get fieldOptions
   * @return fieldOptions
   */
  @Valid 
  @Schema(name = "fieldOptions", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fieldOptions")
  public List<com.bytechef.platform.configuration.web.rest.model.@Valid FieldOptionModel> getFieldOptions() {
    return fieldOptions;
  }

  @JsonProperty("fieldOptions")
  public void setFieldOptions(List<com.bytechef.platform.configuration.web.rest.model.@Valid FieldOptionModel> fieldOptions) {
    this.fieldOptions = fieldOptions;
  }

  public ApprovalFormInputModel fieldType(@Nullable Integer fieldType) {
    this.fieldType = fieldType;
    return this;
  }

  /**
   * Get fieldType
   * @return fieldType
   */
  
  @Schema(name = "fieldType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("fieldType")
  public @Nullable Integer getFieldType() {
    return fieldType;
  }

  @JsonProperty("fieldType")
  public void setFieldType(@Nullable Integer fieldType) {
    this.fieldType = fieldType;
  }

  public ApprovalFormInputModel maxSelection(@Nullable Integer maxSelection) {
    this.maxSelection = maxSelection;
    return this;
  }

  /**
   * Get maxSelection
   * @return maxSelection
   */
  
  @Schema(name = "maxSelection", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("maxSelection")
  public @Nullable Integer getMaxSelection() {
    return maxSelection;
  }

  @JsonProperty("maxSelection")
  public void setMaxSelection(@Nullable Integer maxSelection) {
    this.maxSelection = maxSelection;
  }

  public ApprovalFormInputModel minSelection(@Nullable Integer minSelection) {
    this.minSelection = minSelection;
    return this;
  }

  /**
   * Get minSelection
   * @return minSelection
   */
  
  @Schema(name = "minSelection", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minSelection")
  public @Nullable Integer getMinSelection() {
    return minSelection;
  }

  @JsonProperty("minSelection")
  public void setMinSelection(@Nullable Integer minSelection) {
    this.minSelection = minSelection;
  }

  public ApprovalFormInputModel multipleChoice(@Nullable Boolean multipleChoice) {
    this.multipleChoice = multipleChoice;
    return this;
  }

  /**
   * Get multipleChoice
   * @return multipleChoice
   */
  
  @Schema(name = "multipleChoice", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleChoice")
  public @Nullable Boolean getMultipleChoice() {
    return multipleChoice;
  }

  @JsonProperty("multipleChoice")
  public void setMultipleChoice(@Nullable Boolean multipleChoice) {
    this.multipleChoice = multipleChoice;
  }

  public ApprovalFormInputModel placeholder(@Nullable String placeholder) {
    this.placeholder = placeholder;
    return this;
  }

  /**
   * Get placeholder
   * @return placeholder
   */
  
  @Schema(name = "placeholder", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("placeholder")
  public @Nullable String getPlaceholder() {
    return placeholder;
  }

  @JsonProperty("placeholder")
  public void setPlaceholder(@Nullable String placeholder) {
    this.placeholder = placeholder;
  }

  public ApprovalFormInputModel required(@Nullable Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Get required
   * @return required
   */
  
  @Schema(name = "required", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public @Nullable Boolean getRequired() {
    return required;
  }

  @JsonProperty("required")
  public void setRequired(@Nullable Boolean required) {
    this.required = required;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ApprovalFormInputModel approvalFormInput = (ApprovalFormInputModel) o;
    return Objects.equals(this.defaultValue, approvalFormInput.defaultValue) &&
        Objects.equals(this.fieldDescription, approvalFormInput.fieldDescription) &&
        Objects.equals(this.fieldLabel, approvalFormInput.fieldLabel) &&
        Objects.equals(this.fieldName, approvalFormInput.fieldName) &&
        Objects.equals(this.fieldOptions, approvalFormInput.fieldOptions) &&
        Objects.equals(this.fieldType, approvalFormInput.fieldType) &&
        Objects.equals(this.maxSelection, approvalFormInput.maxSelection) &&
        Objects.equals(this.minSelection, approvalFormInput.minSelection) &&
        Objects.equals(this.multipleChoice, approvalFormInput.multipleChoice) &&
        Objects.equals(this.placeholder, approvalFormInput.placeholder) &&
        Objects.equals(this.required, approvalFormInput.required);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, fieldDescription, fieldLabel, fieldName, fieldOptions, fieldType, maxSelection, minSelection, multipleChoice, placeholder, required);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApprovalFormInputModel {\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    fieldDescription: ").append(toIndentedString(fieldDescription)).append("\n");
    sb.append("    fieldLabel: ").append(toIndentedString(fieldLabel)).append("\n");
    sb.append("    fieldName: ").append(toIndentedString(fieldName)).append("\n");
    sb.append("    fieldOptions: ").append(toIndentedString(fieldOptions)).append("\n");
    sb.append("    fieldType: ").append(toIndentedString(fieldType)).append("\n");
    sb.append("    maxSelection: ").append(toIndentedString(maxSelection)).append("\n");
    sb.append("    minSelection: ").append(toIndentedString(minSelection)).append("\n");
    sb.append("    multipleChoice: ").append(toIndentedString(multipleChoice)).append("\n");
    sb.append("    placeholder: ").append(toIndentedString(placeholder)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
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

