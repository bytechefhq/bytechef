package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.InputTypeModel;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.OptionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
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
 * A resolved component input property the SDK renders.
 */

@Schema(name = "ComponentProperty", description = "A resolved component input property the SDK renders.")
@JsonTypeName("ComponentProperty")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-01T08:27:45.813792006+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ComponentPropertyModel {

  private String name;

  private @Nullable String label;

  private InputTypeModel type = InputTypeModel.STRING;

  private @Nullable String controlType;

  private @Nullable Boolean required;

  @Valid
  private List<@Valid OptionModel> options = new ArrayList<>();

  private @Nullable Boolean dynamicOptions;

  @Valid
  private List<String> optionsLookupDependsOn = new ArrayList<>();

  public ComponentPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentPropertyModel(String name, InputTypeModel type) {
    this.name = name;
    this.type = type;
  }

  public ComponentPropertyModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ComponentPropertyModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  
  @Schema(name = "label", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public ComponentPropertyModel type(InputTypeModel type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
   */
  @NotNull @Valid 
  @Schema(name = "type", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("type")
  public InputTypeModel getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(InputTypeModel type) {
    this.type = type;
  }

  public ComponentPropertyModel controlType(@Nullable String controlType) {
    this.controlType = controlType;
    return this;
  }

  /**
   * Get controlType
   * @return controlType
   */
  
  @Schema(name = "controlType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("controlType")
  public @Nullable String getControlType() {
    return controlType;
  }

  @JsonProperty("controlType")
  public void setControlType(@Nullable String controlType) {
    this.controlType = controlType;
  }

  public ComponentPropertyModel required(@Nullable Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Get required
   * @return required
   */
  
  @Schema(name = "required", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public @Nullable Boolean getRequired() {
    return required;
  }

  @JsonProperty("required")
  public void setRequired(@Nullable Boolean required) {
    this.required = required;
  }

  public ComponentPropertyModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public ComponentPropertyModel addOptionsItem(OptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * Get options
   * @return options
   */
  @Valid 
  @Schema(name = "options", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  @JsonProperty("options")
  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  public ComponentPropertyModel dynamicOptions(@Nullable Boolean dynamicOptions) {
    this.dynamicOptions = dynamicOptions;
    return this;
  }

  /**
   * True when options must be fetched from the options endpoint.
   * @return dynamicOptions
   */
  
  @Schema(name = "dynamicOptions", description = "True when options must be fetched from the options endpoint.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("dynamicOptions")
  public @Nullable Boolean getDynamicOptions() {
    return dynamicOptions;
  }

  @JsonProperty("dynamicOptions")
  public void setDynamicOptions(@Nullable Boolean dynamicOptions) {
    this.dynamicOptions = dynamicOptions;
  }

  public ComponentPropertyModel optionsLookupDependsOn(List<String> optionsLookupDependsOn) {
    this.optionsLookupDependsOn = optionsLookupDependsOn;
    return this;
  }

  public ComponentPropertyModel addOptionsLookupDependsOnItem(String optionsLookupDependsOnItem) {
    if (this.optionsLookupDependsOn == null) {
      this.optionsLookupDependsOn = new ArrayList<>();
    }
    this.optionsLookupDependsOn.add(optionsLookupDependsOnItem);
    return this;
  }

  /**
   * Get optionsLookupDependsOn
   * @return optionsLookupDependsOn
   */
  
  @Schema(name = "optionsLookupDependsOn", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("optionsLookupDependsOn")
  public List<String> getOptionsLookupDependsOn() {
    return optionsLookupDependsOn;
  }

  @JsonProperty("optionsLookupDependsOn")
  public void setOptionsLookupDependsOn(List<String> optionsLookupDependsOn) {
    this.optionsLookupDependsOn = optionsLookupDependsOn;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentPropertyModel componentProperty = (ComponentPropertyModel) o;
    return Objects.equals(this.name, componentProperty.name) &&
        Objects.equals(this.label, componentProperty.label) &&
        Objects.equals(this.type, componentProperty.type) &&
        Objects.equals(this.controlType, componentProperty.controlType) &&
        Objects.equals(this.required, componentProperty.required) &&
        Objects.equals(this.options, componentProperty.options) &&
        Objects.equals(this.dynamicOptions, componentProperty.dynamicOptions) &&
        Objects.equals(this.optionsLookupDependsOn, componentProperty.optionsLookupDependsOn);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, type, controlType, required, options, dynamicOptions, optionsLookupDependsOn);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentPropertyModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    controlType: ").append(toIndentedString(controlType)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    dynamicOptions: ").append(toIndentedString(dynamicOptions)).append("\n");
    sb.append("    optionsLookupDependsOn: ").append(toIndentedString(optionsLookupDependsOn)).append("\n");
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

