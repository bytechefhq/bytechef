package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.HelpModel;
import com.bytechef.platform.configuration.web.rest.model.TriggerTypeModel;
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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-17T06:47:46.507285+01:00[Europe/Zagreb]")
public class TriggerDefinitionBasicModel {

  private String description;

  private HelpModel help;

  private String name;

  private String title;

  private TriggerTypeModel type;

  public TriggerDefinitionBasicModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TriggerDefinitionBasicModel(String name, TriggerTypeModel type) {
    this.name = name;
    this.type = type;
  }

  public TriggerDefinitionBasicModel description(String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
  */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public String getDescription() {
    return description;
  }

  public void setDescription(String description) {
    this.description = description;
  }

  public TriggerDefinitionBasicModel help(HelpModel help) {
    this.help = help;
    return this;
  }

  /**
   * Get help
   * @return help
  */
  @Valid 
  @Schema(name = "help", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("help")
  public HelpModel getHelp() {
    return help;
  }

  public void setHelp(HelpModel help) {
    this.help = help;
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

  public TriggerDefinitionBasicModel title(String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
  */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public String getTitle() {
    return title;
  }

  public void setTitle(String title) {
    this.title = title;
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
    return Objects.equals(this.description, triggerDefinitionBasic.description) &&
        Objects.equals(this.help, triggerDefinitionBasic.help) &&
        Objects.equals(this.name, triggerDefinitionBasic.name) &&
        Objects.equals(this.title, triggerDefinitionBasic.title) &&
        Objects.equals(this.type, triggerDefinitionBasic.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(description, help, name, title, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerDefinitionBasicModel {\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    help: ").append(toIndentedString(help)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
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

