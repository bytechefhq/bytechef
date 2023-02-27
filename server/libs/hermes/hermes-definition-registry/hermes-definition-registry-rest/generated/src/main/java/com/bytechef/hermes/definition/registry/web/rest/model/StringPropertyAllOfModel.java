package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.ControlTypeModel;
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
 * StringPropertyAllOfModel
 */

@JsonTypeName("StringProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-27T08:02:46.343401+01:00[Europe/Zagreb]")
public class StringPropertyAllOfModel {

  @JsonProperty("controlType")
  private ControlTypeModel controlType;

  public StringPropertyAllOfModel controlType(ControlTypeModel controlType) {
    this.controlType = controlType;
    return this;
  }

  /**
   * Get controlType
   * @return controlType
  */
  @Valid 
  @Schema(name = "controlType", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public ControlTypeModel getControlType() {
    return controlType;
  }

  public void setControlType(ControlTypeModel controlType) {
    this.controlType = controlType;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StringPropertyAllOfModel stringPropertyAllOf = (StringPropertyAllOfModel) o;
    return Objects.equals(this.controlType, stringPropertyAllOf.controlType);
  }

  @Override
  public int hashCode() {
    return Objects.hash(controlType);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StringPropertyAllOfModel {\n");
    sb.append("    controlType: ").append(toIndentedString(controlType)).append("\n");
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

