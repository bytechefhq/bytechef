package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-12-04T07:58:17.397618+01:00[Europe/Zagreb]")
public class OptionModel {

  private String description;

  private String displayCondition;

  private String name;

  private JsonNullable<Object> value = JsonNullable.<Object>undefined();

  public OptionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the option.
   * @return description
  */
  
  @Schema(name = "description", description = "Description of the option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public OptionModel displayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
    return this;
  }

  /**
   * Defines rules when a property should be shown or hidden.
   * @return displayCondition
  */
  
  @Schema(name = "displayCondition", description = "Defines rules when a property should be shown or hidden.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayCondition")
  public String getDisplayCondition() {
    return displayCondition;
  }

  public void setDisplayCondition(String displayCondition) {
    this.displayCondition = displayCondition;
  }

  public OptionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an option.
   * @return name
  */
  
  @Schema(name = "name", description = "The name of an option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
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
        Objects.equals(this.displayCondition, option.displayCondition) &&
        Objects.equals(this.name, option.name) &&
        equalsNullable(this.value, option.value);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, displayCondition, name, hashCodeNullable(value));
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
    sb.append("    displayCondition: ").append(toIndentedString(displayCondition)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
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

