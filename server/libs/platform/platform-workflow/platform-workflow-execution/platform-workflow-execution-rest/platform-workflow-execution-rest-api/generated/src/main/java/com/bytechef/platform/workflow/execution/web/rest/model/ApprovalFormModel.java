package com.bytechef.platform.workflow.execution.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.workflow.execution.web.rest.model.ApprovalFormInputModel;
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
 * ApprovalFormModel
 */

@JsonTypeName("ApprovalForm")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-27T14:10:01.371326+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class ApprovalFormModel {

  private @Nullable Long environmentId;

  private @Nullable String formDescription;

  private @Nullable String formTitle;

  @Valid
  private List<@Valid ApprovalFormInputModel> inputs = new ArrayList<>();

  public ApprovalFormModel environmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
    return this;
  }

  /**
   * Get environmentId
   * @return environmentId
   */
  
  @Schema(name = "environmentId", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("environmentId")
  public @Nullable Long getEnvironmentId() {
    return environmentId;
  }

  @JsonProperty("environmentId")
  public void setEnvironmentId(@Nullable Long environmentId) {
    this.environmentId = environmentId;
  }

  public ApprovalFormModel formDescription(@Nullable String formDescription) {
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

  @JsonProperty("formDescription")
  public void setFormDescription(@Nullable String formDescription) {
    this.formDescription = formDescription;
  }

  public ApprovalFormModel formTitle(@Nullable String formTitle) {
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

  @JsonProperty("formTitle")
  public void setFormTitle(@Nullable String formTitle) {
    this.formTitle = formTitle;
  }

  public ApprovalFormModel inputs(List<@Valid ApprovalFormInputModel> inputs) {
    this.inputs = inputs;
    return this;
  }

  public ApprovalFormModel addInputsItem(ApprovalFormInputModel inputsItem) {
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
  public List<@Valid ApprovalFormInputModel> getInputs() {
    return inputs;
  }

  @JsonProperty("inputs")
  public void setInputs(List<@Valid ApprovalFormInputModel> inputs) {
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
    ApprovalFormModel approvalForm = (ApprovalFormModel) o;
    return Objects.equals(this.environmentId, approvalForm.environmentId) &&
        Objects.equals(this.formDescription, approvalForm.formDescription) &&
        Objects.equals(this.formTitle, approvalForm.formTitle) &&
        Objects.equals(this.inputs, approvalForm.inputs);
  }

  @Override
  public int hashCode() {
    return Objects.hash(environmentId, formDescription, formTitle, inputs);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ApprovalFormModel {\n");
    sb.append("    environmentId: ").append(toIndentedString(environmentId)).append("\n");
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
    return o == null ? "null" : o.toString().replace("\n", "\n    ");
  }
}

