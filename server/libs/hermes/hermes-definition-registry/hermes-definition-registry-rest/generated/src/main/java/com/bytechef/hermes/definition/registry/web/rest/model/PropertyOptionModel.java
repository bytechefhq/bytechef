package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayOptionModel;
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

@Schema(name = "PropertyOption", description = "Defines valid property value.")
@JsonTypeName("PropertyOption")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-27T08:02:46.343401+01:00[Europe/Zagreb]")
public class PropertyOptionModel {

  @JsonProperty("description")
  private String description;

  @JsonProperty("displayOption")
  private DisplayOptionModel displayOption;

  @JsonProperty("name")
  private String name;

  @JsonProperty("value")
  private Object value;

  public PropertyOptionModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * Description of the option.
   * @return description
  */
  
  @Schema(name = "description", description = "Description of the option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public PropertyOptionModel displayOption(DisplayOptionModel displayOption) {
    this.displayOption = displayOption;
    return this;
  }

  /**
   * Get displayOption
   * @return displayOption
  */
  @Valid 
  @Schema(name = "displayOption", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public DisplayOptionModel getDisplayOption() {
    return displayOption;
  }

  public void setDisplayOption(DisplayOptionModel displayOption) {
    this.displayOption = displayOption;
  }

  public PropertyOptionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an option.
   * @return name
  */
  
  @Schema(name = "name", description = "The name of an option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public PropertyOptionModel value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * The value of an option.
   * @return value
  */
  
  @Schema(name = "value", description = "The value of an option.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
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
    PropertyOptionModel propertyOption = (PropertyOptionModel) o;
    return Objects.equals(this.description, propertyOption.description) &&
        Objects.equals(this.displayOption, propertyOption.displayOption) &&
        Objects.equals(this.name, propertyOption.name) &&
        Objects.equals(this.value, propertyOption.value);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, displayOption, name, value);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PropertyOptionModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    displayOption: ").append(toIndentedString(displayOption)).append("\n");
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

