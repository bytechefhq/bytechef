package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentInputReferenceModel;
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
 * InputModel
 */

@JsonTypeName("Input")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-15T12:50:13.838758+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class InputModel {

  private @Nullable String defaultValue;

  private Boolean internalOnly = false;

  private @Nullable String label;

  private String name;

  private @Nullable String objectName;

  private List<@Valid OptionModel> options = new ArrayList<>();

  private Boolean required = false;

  private InputTypeModel type = InputTypeModel.STRING;

  private @Nullable ComponentInputReferenceModel componentReference;

  public InputModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public InputModel(String name, InputTypeModel type) {
    this.name = name;
    this.type = type;
  }

  public InputModel defaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
    return this;
  }

  /**
   * The default value of an input
   * @return defaultValue
   */
  
  @Schema(name = "defaultValue", description = "The default value of an input", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("defaultValue")
  public @Nullable String getDefaultValue() {
    return defaultValue;
  }

  @JsonProperty("defaultValue")
  public void setDefaultValue(@Nullable String defaultValue) {
    this.defaultValue = defaultValue;
  }

  public InputModel internalOnly(Boolean internalOnly) {
    this.internalOnly = internalOnly;
    return this;
  }

  /**
   * If true, the input is configured in the admin IntegrationInstanceConfigurationDialog; if false (default), it is rendered in the end-user ConnectDialog.
   * @return internalOnly
   */
  
  @Schema(name = "internalOnly", description = "If true, the input is configured in the admin IntegrationInstanceConfigurationDialog; if false (default), it is rendered in the end-user ConnectDialog.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("internalOnly")
  public Boolean getInternalOnly() {
    return internalOnly;
  }

  @JsonProperty("internalOnly")
  public void setInternalOnly(Boolean internalOnly) {
    this.internalOnly = internalOnly;
  }

  public InputModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The descriptive name of an input
   * @return label
   */
  
  @Schema(name = "label", description = "The descriptive name of an input", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public InputModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of an input
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of an input", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public InputModel objectName(@Nullable String objectName) {
    this.objectName = objectName;
    return this;
  }

  /**
   * For FIELD_MAPPING inputs, the object name used to match the SDK mapObjectFields config.
   * @return objectName
   */
  
  @Schema(name = "objectName", description = "For FIELD_MAPPING inputs, the object name used to match the SDK mapObjectFields config.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("objectName")
  public @Nullable String getObjectName() {
    return objectName;
  }

  @JsonProperty("objectName")
  public void setObjectName(@Nullable String objectName) {
    this.objectName = objectName;
  }

  public InputModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public InputModel addOptionsItem(OptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * The static options an input value is chosen from
   * @return options
   */
  
  @Schema(name = "options", description = "The static options an input value is chosen from", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  @JsonProperty("options")
  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  public InputModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If an input is required, or not
   * @return required
   */
  
  @Schema(name = "required", description = "If an input is required, or not", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public Boolean getRequired() {
    return required;
  }

  @JsonProperty("required")
  public void setRequired(Boolean required) {
    this.required = required;
  }

  public InputModel type(InputTypeModel type) {
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

  public InputModel componentReference(@Nullable ComponentInputReferenceModel componentReference) {
    this.componentReference = componentReference;
    return this;
  }

  /**
   * Get componentReference
   * @return componentReference
   */
  @Valid 
  @Schema(name = "componentReference", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentReference")
  public @Nullable ComponentInputReferenceModel getComponentReference() {
    return componentReference;
  }

  @JsonProperty("componentReference")
  public void setComponentReference(@Nullable ComponentInputReferenceModel componentReference) {
    this.componentReference = componentReference;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    InputModel input = (InputModel) o;
    return Objects.equals(this.defaultValue, input.defaultValue) &&
        Objects.equals(this.internalOnly, input.internalOnly) &&
        Objects.equals(this.label, input.label) &&
        Objects.equals(this.name, input.name) &&
        Objects.equals(this.objectName, input.objectName) &&
        Objects.equals(this.options, input.options) &&
        Objects.equals(this.required, input.required) &&
        Objects.equals(this.type, input.type) &&
        Objects.equals(this.componentReference, input.componentReference);
  }

  @Override
  public int hashCode() {
    return Objects.hash(defaultValue, internalOnly, label, name, objectName, options, required, type, componentReference);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InputModel {\n");
    sb.append("    defaultValue: ").append(toIndentedString(defaultValue)).append("\n");
    sb.append("    internalOnly: ").append(toIndentedString(internalOnly)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    objectName: ").append(toIndentedString(objectName)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    componentReference: ").append(toIndentedString(componentReference)).append("\n");
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

