package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.configuration.public_.web.rest.model.ComponentPropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
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
 * A resolved component property group rendered as one compound input.
 */

@Schema(name = "ComponentPropertyGroup", description = "A resolved component property group rendered as one compound input.")
@JsonTypeName("ComponentPropertyGroup")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-07-02T07:47:45.088419+02:00[Europe/Zagreb]", comments = "Generator version: 7.22.0")
public class ComponentPropertyGroupModel {

  private String name;

  private @Nullable String label;

  @Valid
  private List<@Valid ComponentPropertyModel> properties = new ArrayList<>();

  public ComponentPropertyGroupModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ComponentPropertyGroupModel(String name) {
    this.name = name;
  }

  public ComponentPropertyGroupModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Get name
   * @return name
   */
  @NotNull 
  @Schema(name = "name", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public ComponentPropertyGroupModel label(@Nullable String label) {
    this.label = label;
    return this;
  }

  /**
   * Get label
   * @return label
   */
  
  @Schema(name = "label", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("label")
  public @Nullable String getLabel() {
    return label;
  }

  @JsonProperty("label")
  public void setLabel(@Nullable String label) {
    this.label = label;
  }

  public ComponentPropertyGroupModel properties(List<@Valid ComponentPropertyModel> properties) {
    this.properties = properties;
    return this;
  }

  public ComponentPropertyGroupModel addPropertiesItem(ComponentPropertyModel propertiesItem) {
    if (this.properties == null) {
      this.properties = new ArrayList<>();
    }
    this.properties.add(propertiesItem);
    return this;
  }

  /**
   * Get properties
   * @return properties
   */
  @Valid 
  @Schema(name = "properties", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("properties")
  public List<@Valid ComponentPropertyModel> getProperties() {
    return properties;
  }

  @JsonProperty("properties")
  public void setProperties(List<@Valid ComponentPropertyModel> properties) {
    this.properties = properties;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ComponentPropertyGroupModel componentPropertyGroup = (ComponentPropertyGroupModel) o;
    return Objects.equals(this.name, componentPropertyGroup.name) &&
        Objects.equals(this.label, componentPropertyGroup.label) &&
        Objects.equals(this.properties, componentPropertyGroup.properties);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, label, properties);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ComponentPropertyGroupModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    label: ").append(toIndentedString(label)).append("\n");
    sb.append("    properties: ").append(toIndentedString(properties)).append("\n");
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

