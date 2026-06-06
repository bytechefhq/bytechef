package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
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
 * SaveWorkflowTestConfigurationInputsRequestModel
 */

@JsonTypeName("saveWorkflowTestConfigurationInputs_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T14:23:01.526728+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class SaveWorkflowTestConfigurationInputsRequestModel {

  private @Nullable String key;

  private JsonNullable<Object> value = JsonNullable.<Object>undefined();

  public SaveWorkflowTestConfigurationInputsRequestModel key(@Nullable String key) {
    this.key = key;
    return this;
  }

  /**
   * Get key
   * @return key
   */
  
  @Schema(name = "key", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("key")
  public @Nullable String getKey() {
    return key;
  }

  @JsonProperty("key")
  public void setKey(@Nullable String key) {
    this.key = key;
  }

  public SaveWorkflowTestConfigurationInputsRequestModel value(Object value) {
    this.value = JsonNullable.of(value);
    return this;
  }

  /**
   * The input value; a primitive for primitive inputs, or a nested object for component-property inputs.
   * @return value
   */
  
  @Schema(name = "value", description = "The input value; a primitive for primitive inputs, or a nested object for component-property inputs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public JsonNullable<Object> getValue() {
    return value;
  }

  public void setValue(JsonNullable<Object> value) {
    this.value = value;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    SaveWorkflowTestConfigurationInputsRequestModel saveWorkflowTestConfigurationInputsRequest = (SaveWorkflowTestConfigurationInputsRequestModel) o;
    return Objects.equals(this.key, saveWorkflowTestConfigurationInputsRequest.key) &&
        equalsNullable(this.value, saveWorkflowTestConfigurationInputsRequest.value);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(key, hashCodeNullable(value));
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
    sb.append("class SaveWorkflowTestConfigurationInputsRequestModel {\n");
    sb.append("    key: ").append(toIndentedString(key)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

