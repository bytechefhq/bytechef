package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.platform.configuration.web.rest.model.PropertyModel;
import com.bytechef.platform.configuration.web.rest.model.PropertyTypeModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
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
 * A task property used in task dispatchers.
 */

@Schema(name = "TaskProperty", description = "A task property used in task dispatchers.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-19T14:30:20.578204370+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class TaskPropertyModel extends PropertyModel {

  private @Nullable String name;

  public TaskPropertyModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public TaskPropertyModel(PropertyTypeModel type) {
    super(type);
  }

  public TaskPropertyModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The task name.
   * @return name
   */
  
  @Schema(name = "name", description = "The task name.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }


  public TaskPropertyModel advancedOption(Boolean advancedOption) {
    super.advancedOption(advancedOption);
    return this;
  }

  public TaskPropertyModel description(String description) {
    super.description(description);
    return this;
  }

  public TaskPropertyModel displayCondition(String displayCondition) {
    super.displayCondition(displayCondition);
    return this;
  }

  public TaskPropertyModel expressionEnabled(Boolean expressionEnabled) {
    super.expressionEnabled(expressionEnabled);
    return this;
  }

  public TaskPropertyModel hidden(Boolean hidden) {
    super.hidden(hidden);
    return this;
  }

  public TaskPropertyModel required(Boolean required) {
    super.required(required);
    return this;
  }

  public TaskPropertyModel type(PropertyTypeModel type) {
    super.type(type);
    return this;
  }
  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TaskPropertyModel taskProperty = (TaskPropertyModel) o;
    return Objects.equals(this.name, taskProperty.name) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TaskPropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
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

