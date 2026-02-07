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
 * Defines valid property value.
 */

@Schema(name = "Option", description = "Defines valid property value.")
@JsonTypeName("Option")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-02-07T09:52:01.007100+01:00[Europe/Zagreb]", comments = "Generator version: 7.19.0")
public class OptionModel {

  private @Nullable String description;

  private @Nullable String label;

  private JsonNullable<Object> value = JsonNullable.<Object>undefined();

  public OptionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the option.
   * @return description
   */
  
  @Schema(name = "description", description = "Description of the option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public OptionModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of an option.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of an option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public OptionModel value(Object value) {
    this.value = JsonNullable.of(value);
    return this;
  }

  /**
   * Can be anything: string, number, array, object, etc. (except `null`)
   * @return value
   */
  
  @Schema(name = "value", description = "Can be anything: string, number, array, object, etc. (except `null`)", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    OptionModel option = (OptionModel) o;
    return Objects.equals(this.description, option.description) &&
        Objects.equals(this.label, option.label) &&
        equalsNullable(this.value, option.value);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, label, hashCodeNullable(value));
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
    sb.append("class OptionModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

