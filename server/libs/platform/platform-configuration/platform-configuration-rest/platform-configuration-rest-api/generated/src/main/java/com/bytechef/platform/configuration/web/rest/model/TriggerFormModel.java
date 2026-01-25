package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.TriggerFormInputModel;
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
 * TriggerFormModel
 */

@JsonTypeName("TriggerForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:36:59.494207+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class TriggerFormModel {

  private @Nullable String buttonLabel;

  private @Nullable String customFormStyling;

  private @Nullable String formDescription;

  private @Nullable String formPath;

  private @Nullable String formTitle;

  private @Nullable Boolean appendAttribution;

  private @Nullable Boolean ignoreBots;

  private @Nullable Boolean useWorkflowTimezone;

  @Valid
  private List<@Valid TriggerFormInputModel> inputs = new ArrayList<>();

  public TriggerFormModel buttonLabel(@Nullable String buttonLabel) {
    this.buttonLabel = buttonLabel;
    return this;
  }

  /**
   * Get buttonLabel
   * @return buttonLabel
   */
  
  @Schema(name = "buttonLabel", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("buttonLabel")
  public @Nullable String getButtonLabel() {
    return buttonLabel;
  }

  public void setButtonLabel(@Nullable String buttonLabel) {
    this.buttonLabel = buttonLabel;
  }

  public TriggerFormModel customFormStyling(@Nullable String customFormStyling) {
    this.customFormStyling = customFormStyling;
    return this;
  }

  /**
   * Get customFormStyling
   * @return customFormStyling
   */
  
  @Schema(name = "customFormStyling", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("customFormStyling")
  public @Nullable String getCustomFormStyling() {
    return customFormStyling;
  }

  public void setCustomFormStyling(@Nullable String customFormStyling) {
    this.customFormStyling = customFormStyling;
  }

  public TriggerFormModel formDescription(@Nullable String formDescription) {
    this.formDescription = formDescription;
    return this;
  }

  /**
   * Get formDescription
   * @return formDescription
   */
  
  @Schema(name = "formDescription", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("formDescription")
  public @Nullable String getFormDescription() {
    return formDescription;
  }

  public void setFormDescription(@Nullable String formDescription) {
    this.formDescription = formDescription;
  }

  public TriggerFormModel formPath(@Nullable String formPath) {
    this.formPath = formPath;
    return this;
  }

  /**
   * Get formPath
   * @return formPath
   */
  
  @Schema(name = "formPath", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("formPath")
  public @Nullable String getFormPath() {
    return formPath;
  }

  public void setFormPath(@Nullable String formPath) {
    this.formPath = formPath;
  }

  public TriggerFormModel formTitle(@Nullable String formTitle) {
    this.formTitle = formTitle;
    return this;
  }

  /**
   * Get formTitle
   * @return formTitle
   */
  
  @Schema(name = "formTitle", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("formTitle")
  public @Nullable String getFormTitle() {
    return formTitle;
  }

  public void setFormTitle(@Nullable String formTitle) {
    this.formTitle = formTitle;
  }

  public TriggerFormModel appendAttribution(@Nullable Boolean appendAttribution) {
    this.appendAttribution = appendAttribution;
    return this;
  }

  /**
   * Get appendAttribution
   * @return appendAttribution
   */
  
  @Schema(name = "appendAttribution", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("appendAttribution")
  public @Nullable Boolean getAppendAttribution() {
    return appendAttribution;
  }

  public void setAppendAttribution(@Nullable Boolean appendAttribution) {
    this.appendAttribution = appendAttribution;
  }

  public TriggerFormModel ignoreBots(@Nullable Boolean ignoreBots) {
    this.ignoreBots = ignoreBots;
    return this;
  }

  /**
   * Get ignoreBots
   * @return ignoreBots
   */
  
  @Schema(name = "ignoreBots", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("ignoreBots")
  public @Nullable Boolean getIgnoreBots() {
    return ignoreBots;
  }

  public void setIgnoreBots(@Nullable Boolean ignoreBots) {
    this.ignoreBots = ignoreBots;
  }

  public TriggerFormModel useWorkflowTimezone(@Nullable Boolean useWorkflowTimezone) {
    this.useWorkflowTimezone = useWorkflowTimezone;
    return this;
  }

  /**
   * Get useWorkflowTimezone
   * @return useWorkflowTimezone
   */
  
  @Schema(name = "useWorkflowTimezone", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("useWorkflowTimezone")
  public @Nullable Boolean getUseWorkflowTimezone() {
    return useWorkflowTimezone;
  }

  public void setUseWorkflowTimezone(@Nullable Boolean useWorkflowTimezone) {
    this.useWorkflowTimezone = useWorkflowTimezone;
  }

  public TriggerFormModel inputs(List<@Valid TriggerFormInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public TriggerFormModel addInputsItem(TriggerFormInputModel inputsItem) {
    if (this.inputs == null) {
      this.inputs = new ArrayList<>();
    }
    this.inputs.add(inputsItem);
    return this;
  }

  /**
   * Get inputs
   * @return inputs
   */
  @Valid 
  @Schema(name = "inputs", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("inputs")
  public List<@Valid TriggerFormInputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<@Valid TriggerFormInputModel> inputs) {
    this.inputs = inputs;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerFormModel triggerForm = (TriggerFormModel) o;
    return Objects.equals(this.buttonLabel, triggerForm.buttonLabel) &&
        Objects.equals(this.customFormStyling, triggerForm.customFormStyling) &&
        Objects.equals(this.formDescription, triggerForm.formDescription) &&
        Objects.equals(this.formPath, triggerForm.formPath) &&
        Objects.equals(this.formTitle, triggerForm.formTitle) &&
        Objects.equals(this.appendAttribution, triggerForm.appendAttribution) &&
        Objects.equals(this.ignoreBots, triggerForm.ignoreBots) &&
        Objects.equals(this.useWorkflowTimezone, triggerForm.useWorkflowTimezone) &&
        Objects.equals(this.inputs, triggerForm.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(buttonLabel, customFormStyling, formDescription, formPath, formTitle, appendAttribution, ignoreBots, useWorkflowTimezone, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerFormModel {\n");
    sb.append("    buttonLabel: ").append(toIndentedString(buttonLabel)).append("\n");
    sb.append("    customFormStyling: ").append(toIndentedString(customFormStyling)).append("\n");
    sb.append("    formDescription: ").append(toIndentedString(formDescription)).append("\n");
    sb.append("    formPath: ").append(toIndentedString(formPath)).append("\n");
    sb.append("    formTitle: ").append(toIndentedString(formTitle)).append("\n");
    sb.append("    appendAttribution: ").append(toIndentedString(appendAttribution)).append("\n");
    sb.append("    ignoreBots: ").append(toIndentedString(ignoreBots)).append("\n");
    sb.append("    useWorkflowTimezone: ").append(toIndentedString(useWorkflowTimezone)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
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

