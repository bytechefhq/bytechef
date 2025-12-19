package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A type of a cluster element.
 */

@Schema(name = "ClusterElementType", description = "A type of a cluster element.")
@JsonTypeName("ClusterElementType")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-12-19T14:30:20.578204370+01:00[Europe/Zagreb]", comments = "Generator version: 7.17.0")
public class ClusterElementTypeModel {

  private @Nullable String name;

  private @Nullable String label;

  private @Nullable Boolean required;

  private @Nullable Boolean multipleElements;

  public ClusterElementTypeModel name(@Nullable String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a cluster element type.
   * @return name
   */
  
  @Schema(name = "name", description = "The name of a cluster element type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("name")
  public @Nullable String getName() {
    return name;
  }

  public void setName(@Nullable String name) {
    this.name = name;
  }

  public ClusterElementTypeModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * The label of a cluster element type.
   * @return label
   */
  
  @Schema(name = "label", description = "The label of a cluster element type.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public ClusterElementTypeModel required(@Nullable Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * If the cluster element type is required.
   * @return required
   */
  
  @Schema(name = "required", description = "If the cluster element type is required.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("required")
  public @Nullable Boolean getRequired() {
    return required;
  }

  public void setRequired(@Nullable Boolean required) {
    this.required = required;
  }

  public ClusterElementTypeModel multipleElements(@Nullable Boolean multipleElements) {
    this.multipleElements = multipleElements;
    return this;
  }

  /**
   * If multiple elements can be added.
   * @return multipleElements
   */
  
  @Schema(name = "multipleElements", description = "If multiple elements can be added.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleElements")
  public @Nullable Boolean getMultipleElements() {
    return multipleElements;
  }

  public void setMultipleElements(@Nullable Boolean multipleElements) {
    this.multipleElements = multipleElements;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ClusterElementTypeModel clusterElementType = (ClusterElementTypeModel) o;
    return Objects.equals(this.name, clusterElementType.name) &&
        Objects.equals(this.label, clusterElementType.label) &&
        Objects.equals(this.required, clusterElementType.required) &&
        Objects.equals(this.multipleElements, clusterElementType.multipleElements);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, required, multipleElements);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ClusterElementTypeModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    multipleElements: ").append(toIndentedString(multipleElements)).append("\n");
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

