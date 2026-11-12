package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.TriggerSubscriptionErrorCodeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * Details about the error that made the subscription fail.
 */

@Schema(name = "TriggerSubscriptionError", description = "Details about the error that made the subscription fail.")
@JsonTypeName("TriggerSubscriptionError")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-12T00:36:32.396363+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class TriggerSubscriptionErrorModel {

  private TriggerSubscriptionErrorCodeModel code;

  private @Nullable String message;

  public TriggerSubscriptionErrorModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerSubscriptionErrorModel(TriggerSubscriptionErrorCodeModel code) {
    this.code = code;
  }

  public TriggerSubscriptionErrorModel code(TriggerSubscriptionErrorCodeModel code) {
    this.code = code;
    return this;
  }

  /**
   * Get code
   * @return code
   */
  @NotNull @Valid 
  @Schema(name = "code", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("code")
  public TriggerSubscriptionErrorCodeModel getCode() {
    return code;
  }

  @JsonProperty("code")
  public void setCode(TriggerSubscriptionErrorCodeModel code) {
    this.code = code;
  }

  public TriggerSubscriptionErrorModel message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * The error message.
   * @return message
   */
  
  @Schema(name = "message", description = "The error message.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerSubscriptionErrorModel triggerSubscriptionError = (TriggerSubscriptionErrorModel) o;
    return Objects.equals(this.code, triggerSubscriptionError.code) &&
        Objects.equals(this.message, triggerSubscriptionError.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(code, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerSubscriptionErrorModel {\n");
    sb.append("    code: ").append(toIndentedString(code)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

