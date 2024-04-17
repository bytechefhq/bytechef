package com.bytechef.platform.configuration.web.rest.model;

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
 * The source/destination data stream component.
 */

@Schema(name = "DataStreamComponent", description = "The source/destination data stream component.")
@JsonTypeName("DataStreamComponent")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-04-17T17:52:35.553216+02:00[Europe/Zagreb]", comments = "Generator version: 7.4.0")
public class DataStreamComponentModel {

  private String componentName;

  private Integer componentVersion;

  public DataStreamComponentModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of a component.
   * @return componentName
  */
  
  @Schema(name = "componentName", description = "The name of a component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public DataStreamComponentModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of a component.
   * @return componentVersion
  */
  
  @Schema(name = "componentVersion", description = "The version of a component.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DataStreamComponentModel dataStreamComponent = (DataStreamComponentModel) o;
    return Objects.equals(this.componentName, dataStreamComponent.componentName) &&
        Objects.equals(this.componentVersion, dataStreamComponent.componentVersion);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentName, componentVersion);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DataStreamComponentModel {\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
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

