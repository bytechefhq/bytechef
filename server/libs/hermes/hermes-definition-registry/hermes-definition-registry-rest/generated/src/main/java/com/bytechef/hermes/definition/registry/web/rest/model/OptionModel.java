package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-05T18:35:34.469553+02:00[Europe/Zagreb]")
public class OptionModel {

  private String description;

  private String displayCondition;

  private String name;

  private Object value;

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
    this.value = value;
    return this;
  }

  /**
   * The value of an option.
   * @return value
  */
  
  @Schema(name = "value", description = "The value of an option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
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
        Objects.equals(this.value, option.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, displayCondition, name, value);
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

