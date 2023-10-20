package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ControlTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyTypeModel;
import com.bytechef.hermes.definition.registry.web.rest.model.ValuePropertyModel;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A time property.
 */

@Schema(name = "TimeProperty", description = "A time property.")

@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-26T12:56:34.547448+02:00[Europe/Zagreb]")
public class TimePropertyModel extends ValuePropertyModel {

  private Integer hour;

  private Integer minute;

  private Integer second;

  public TimePropertyModel hour(Integer hour) {
    this.hour = hour;
    return this;
  }

  /**
   * The hour.
   * @return hour
  */
  
  @Schema(name = "hour", description = "The hour.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("hour")
  public Integer getHour() {
    return hour;
  }

  public void setHour(Integer hour) {
    this.hour = hour;
  }

  public TimePropertyModel minute(Integer minute) {
    this.minute = minute;
    return this;
  }

  /**
   * The minute.
   * @return minute
  */
  
  @Schema(name = "minute", description = "The minute.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("minute")
  public Integer getMinute() {
    return minute;
  }

  public void setMinute(Integer minute) {
    this.minute = minute;
  }

  public TimePropertyModel second(Integer second) {
    this.second = second;
    return this;
  }

  /**
   * The second.
   * @return second
  */
  
  @Schema(name = "second", description = "The second.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("second")
  public Integer getSecond() {
    return second;
  }

  public void setSecond(Integer second) {
    this.second = second;
  }

  public TimePropertyModel controlType(ControlTypeModel controlType) {
    super.setControlType(controlType);
    return this;
  }

  public TimePropertyModel defaultValue(Object defaultValue) {
    super.setDefaultValue(defaultValue);
    return this;
  }

  public TimePropertyModel exampleValue(Object exampleValue) {
    super.setExampleValue(exampleValue);
    return this;
  }

  public TimePropertyModel advancedOption(Boolean advancedOption) {
    super.setAdvancedOption(advancedOption);
    return this;
  }

  public TimePropertyModel description(String description) {
    super.setDescription(description);
    return this;
  }

  public TimePropertyModel displayCondition(String displayCondition) {
    super.setDisplayCondition(displayCondition);
    return this;
  }

  public TimePropertyModel expressionDisabled(Boolean expressionDisabled) {
    super.setExpressionDisabled(expressionDisabled);
    return this;
  }

  public TimePropertyModel hidden(Boolean hidden) {
    super.setHidden(hidden);
    return this;
  }

  public TimePropertyModel label(String label) {
    super.setLabel(label);
    return this;
  }

  public TimePropertyModel metadata(Map<String, Object> metadata) {
    super.setMetadata(metadata);
    return this;
  }

  public TimePropertyModel putMetadataItem(String key, Object metadataItem) {
    super.putMetadataItem(key, metadataItem);
    return this;
  }

  public TimePropertyModel name(String name) {
    super.setName(name);
    return this;
  }

  public TimePropertyModel placeholder(String placeholder) {
    super.setPlaceholder(placeholder);
    return this;
  }

  public TimePropertyModel required(Boolean required) {
    super.setRequired(required);
    return this;
  }

  public TimePropertyModel type(PropertyTypeModel type) {
    super.setType(type);
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
    TimePropertyModel timeProperty = (TimePropertyModel) o;
    return Objects.equals(this.hour, timeProperty.hour) &&
        Objects.equals(this.minute, timeProperty.minute) &&
        Objects.equals(this.second, timeProperty.second) &&
        super.equals(o);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hour, minute, second, super.hashCode());
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimePropertyModel {\n");
    sb.append("    ").append(toIndentedString(super.toString())).append("\n");
    sb.append("    hour: ").append(toIndentedString(hour)).append("\n");
    sb.append("    minute: ").append(toIndentedString(minute)).append("\n");
    sb.append("    second: ").append(toIndentedString(second)).append("\n");
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

