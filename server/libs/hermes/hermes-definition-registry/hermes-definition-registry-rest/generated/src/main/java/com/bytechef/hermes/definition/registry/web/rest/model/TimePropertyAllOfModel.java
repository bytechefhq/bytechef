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
 * TimePropertyAllOfModel
 */

@JsonTypeName("TimeProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-05-08T21:31:51.028205+02:00[Europe/Zagreb]")
public class TimePropertyAllOfModel {

  private Integer hour;

  private Integer minute;

  private Integer second;

  public TimePropertyAllOfModel hour(Integer hour) {
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

  public TimePropertyAllOfModel minute(Integer minute) {
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

  public TimePropertyAllOfModel second(Integer second) {
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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TimePropertyAllOfModel timePropertyAllOf = (TimePropertyAllOfModel) o;
    return Objects.equals(this.hour, timePropertyAllOf.hour) &&
        Objects.equals(this.minute, timePropertyAllOf.minute) &&
        Objects.equals(this.second, timePropertyAllOf.second);
  }

  @Override
  public int hashCode() {
    return Objects.hash(hour, minute, second);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TimePropertyAllOfModel {\n");
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

