package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ErrorsInnerModel;
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
 * An error of a trigger subscription operation. errors[].code is a trigger subscription error code (USER_INPUT_ERROR, API_ERROR, TIMEOUT_ERROR, OAUTH_REFRESH, NO_TRIGGER, INTERNAL_ERROR) or NOT_FOUND.
 */

@Schema(name = "TriggerSubscriptionFailure", description = "An error of a trigger subscription operation. errors[].code is a trigger subscription error code (USER_INPUT_ERROR, API_ERROR, TIMEOUT_ERROR, OAUTH_REFRESH, NO_TRIGGER, INTERNAL_ERROR) or NOT_FOUND.")
@JsonTypeName("TriggerSubscriptionFailure")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T01:42:14.080303+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class TriggerSubscriptionFailureModel {

  private List<@Valid ErrorsInnerModel> errors = new ArrayList<>();

  private @Nullable Long triggerSubscriptionId;

  public TriggerSubscriptionFailureModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerSubscriptionFailureModel(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
  }

  public TriggerSubscriptionFailureModel errors(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
    return this;
  }

  public TriggerSubscriptionFailureModel addErrorsItem(ErrorsInnerModel errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * Get errors
   * @return errors
   */
  @NotNull 
  @Schema(name = "errors", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("errors")
  public List<@Valid ErrorsInnerModel> getErrors() {
    return errors;
  }

  @JsonProperty("errors")
  public void setErrors(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
  }

  public TriggerSubscriptionFailureModel triggerSubscriptionId(@Nullable Long triggerSubscriptionId) {
    this.triggerSubscriptionId = triggerSubscriptionId;
    return this;
  }

  /**
   * The id of the subscription whose trigger could not be registered, unregistered or redeployed on enable or update. Absent for validation errors, NO_TRIGGER and NOT_FOUND, when a subscribe call failed and the subscription was removed, and when an update of a subscription that was not ENABLED failed.
   * @return triggerSubscriptionId
   */
  
  @Schema(name = "triggerSubscriptionId", description = "The id of the subscription whose trigger could not be registered, unregistered or redeployed on enable or update. Absent for validation errors, NO_TRIGGER and NOT_FOUND, when a subscribe call failed and the subscription was removed, and when an update of a subscription that was not ENABLED failed.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerSubscriptionId")
  public @Nullable Long getTriggerSubscriptionId() {
    return triggerSubscriptionId;
  }

  @JsonProperty("triggerSubscriptionId")
  public void setTriggerSubscriptionId(@Nullable Long triggerSubscriptionId) {
    this.triggerSubscriptionId = triggerSubscriptionId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerSubscriptionFailureModel triggerSubscriptionFailure = (TriggerSubscriptionFailureModel) o;
    return Objects.equals(this.errors, triggerSubscriptionFailure.errors) &&
        Objects.equals(this.triggerSubscriptionId, triggerSubscriptionFailure.triggerSubscriptionId);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errors, triggerSubscriptionId);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerSubscriptionFailureModel {\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
    sb.append("    triggerSubscriptionId: ").append(toIndentedString(triggerSubscriptionId)).append("\n");
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

