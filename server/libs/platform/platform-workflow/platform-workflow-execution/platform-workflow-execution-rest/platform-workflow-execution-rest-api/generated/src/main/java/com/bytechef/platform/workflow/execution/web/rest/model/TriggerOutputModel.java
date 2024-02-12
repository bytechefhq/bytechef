package com.bytechef.platform.workflow.execution.web.rest.model;

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
 * The trigger output.
 */

@Schema(name = "TriggerOutput", description = "The trigger output.")
@JsonTypeName("TriggerOutput")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-02-12T06:58:16.211035+01:00[Europe/Zagreb]")
public class TriggerOutputModel {

  private Object value;

  private String triggerName;

  public TriggerOutputModel value(Object value) {
    this.value = value;
    return this;
  }

  /**
   * The trigger output value
   * @return value
  */
  
  @Schema(name = "value", description = "The trigger output value", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("value")
  public Object getValue() {
    return value;
  }

  public void setValue(Object value) {
    this.value = value;
  }

  public TriggerOutputModel triggerName(String triggerName) {
    this.triggerName = triggerName;
    return this;
  }

  /**
   * The task name to which a connection belongs.
   * @return triggerName
  */
  
  @Schema(name = "triggerName", description = "The task name to which a connection belongs.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerName")
  public String getTriggerName() {
    return triggerName;
  }

  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    TriggerOutputModel triggerOutput = (TriggerOutputModel) o;
    return Objects.equals(this.value, triggerOutput.value) &&
        Objects.equals(this.triggerName, triggerOutput.triggerName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(value, triggerName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class TriggerOutputModel {\n");
    sb.append("    value: ").append(toIndentedString(value)).append("\n");
    sb.append("    triggerName: ").append(toIndentedString(triggerName)).append("\n");
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

