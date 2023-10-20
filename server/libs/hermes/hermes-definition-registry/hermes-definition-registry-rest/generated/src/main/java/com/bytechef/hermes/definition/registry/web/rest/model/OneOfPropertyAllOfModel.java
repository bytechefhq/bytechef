package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.PropertyModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * OneOfPropertyAllOfModel
 */

@JsonTypeName("OneOfProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-02-20T11:29:33.968820+01:00[Europe/Zagreb]")
public class OneOfPropertyAllOfModel {

  @JsonProperty("required")
  private Boolean required;

  @JsonProperty("types")
  @Valid
  private List<PropertyModel> types = null;

  @JsonProperty("type")
  private String type;

  public OneOfPropertyAllOfModel required(Boolean required) {
    this.required = required;
    return this;
  }

  /**
   * Get required
   * @return required
  */
  
  @Schema(name = "required", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public Boolean getRequired() {
    return required;
  }

  public void setRequired(Boolean required) {
    this.required = required;
  }

  public OneOfPropertyAllOfModel types(List<PropertyModel> types) {
    this.types = types;
    return this;
  }

  public OneOfPropertyAllOfModel addTypesItem(PropertyModel typesItem) {
    if (this.types == null) {
      this.types = new ArrayList<>();
    }
    this.types.add(typesItem);
    return this;
  }

  /**
   * Possible types of properties that can be used.
   * @return types
  */
  @Valid 
  @Schema(name = "types", description = "Possible types of properties that can be used.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public List<PropertyModel> getTypes() {
    return types;
  }

  public void setTypes(List<PropertyModel> types) {
    this.types = types;
  }

  public OneOfPropertyAllOfModel type(String type) {
    this.type = type;
    return this;
  }

  /**
   * Get type
   * @return type
  */
  
  @Schema(name = "type", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public String getType() {
    return type;
  }

  public void setType(String type) {
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
    OneOfPropertyAllOfModel oneOfPropertyAllOf = (OneOfPropertyAllOfModel) o;
    return Objects.equals(this.required, oneOfPropertyAllOf.required) &&
        Objects.equals(this.types, oneOfPropertyAllOf.types) &&
        Objects.equals(this.type, oneOfPropertyAllOf.type);
  }

  @Override
  public int hashCode() {
    return Objects.hash(required, types, type);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OneOfPropertyAllOfModel {\n");
    sb.append("    required: ").append(toIndentedString(required)).append("\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
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

