package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.DisplayModel;
import com.bytechef.hermes.definition.registry.web.rest.model.TriggerTypeModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A trigger definition defines ways to trigger workflows from the outside services.
 */

@Schema(name = "TriggerDefinitionBasic", description = "A trigger definition defines ways to trigger workflows from the outside services.")
@JsonTypeName("TriggerDefinitionBasic")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-15T19:47:32.550589+02:00[Europe/Zagreb]")
public class TriggerDefinitionBasicModel {

  private DisplayModel display;

  private String name;

  private TriggerTypeModel type;

  /**
   * Default constructor
   * @deprecated Use {@link TriggerDefinitionBasicModel#TriggerDefinitionBasicModel(DisplayModel, String, TriggerTypeModel)}
   */
  @Deprecated
  public TriggerDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionBasicModel(DisplayModel display, String name, TriggerTypeModel type) {
    this.display = display;
    this.name = name;
    this.type = type;
  }

  public TriggerDefinitionBasicModel display(DisplayModel display) {
    this.display = display;
    return this;
  }

  /**
   * Get display
   * @return display
  */
  @NotNull @Valid 
  @Schema(name = "display", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("display")
  public DisplayModel getDisplay() {
    return display;
  }

  public void setDisplay(DisplayModel display) {
    this.display = display;
  }

  public TriggerDefinitionBasicModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The action name.
   * @return name
  */
  @NotNull 
  @Schema(name = "name", description = "The action name.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  public void setName(String name) {
    this.name = name;
  }

  public TriggerDefinitionBasicModel type(TriggerTypeModel type) {
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
  public TriggerTypeModel getType() {
    return type;
  }

  public void setType(TriggerTypeModel type) {
    this.type = type;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerDefinitionBasicModel triggerDefinitionBasic = (TriggerDefinitionBasicModel) o;
    return Objects.equals(this.display, triggerDefinitionBasic.display) &&
        Objects.equals(this.name, triggerDefinitionBasic.name) &&
        Objects.equals(this.type, triggerDefinitionBasic.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(display, name, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionBasicModel {\n");
    sb.append("    display: ").append(toIndentedString(display)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
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

