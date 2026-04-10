package com.bytechef.automation.configuration.web.rest.model;

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
 * ApproveFormModel
 */

@JsonTypeName("ApproveForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-10T15:49:41.674883+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ApproveFormModel {

  private @Nullable String formDescription;

  private @Nullable String formTitle;

  @Valid
  private List<com.bytechef.platform.configuration.web.rest.model.@Valid TriggerFormInputModel> inputs = new ArrayList<>();

  public ApproveFormModel formDescription(@Nullable String formDescription) {
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

  public ApproveFormModel formTitle(@Nullable String formTitle) {
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

  public ApproveFormModel inputs(List<com.bytechef.platform.configuration.web.rest.model.@Valid TriggerFormInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ApproveFormModel addInputsItem(com.bytechef.platform.configuration.web.rest.model.TriggerFormInputModel inputsItem) {
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
  public List<com.bytechef.platform.configuration.web.rest.model.@Valid TriggerFormInputModel> getInputs() {
    return inputs;
  }

  public void setInputs(List<com.bytechef.platform.configuration.web.rest.model.@Valid TriggerFormInputModel> inputs) {
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
    ApproveFormModel approveForm = (ApproveFormModel) o;
    return Objects.equals(this.formDescription, approveForm.formDescription) &&
        Objects.equals(this.formTitle, approveForm.formTitle) &&
        Objects.equals(this.inputs, approveForm.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(formDescription, formTitle, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApproveFormModel {\n");
    sb.append("    formDescription: ").append(toIndentedString(formDescription)).append("\n");
    sb.append("    formTitle: ").append(toIndentedString(formTitle)).append("\n");
    sb.append("    inputs: ").append(toIndentedString(inputs)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

