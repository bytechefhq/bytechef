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
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-02T18:38:21.432374+01:00[Europe/Zagreb]")
public class OneOfPropertyAllOfModel {

  @JsonProperty("types")
  @Valid
  private List<PropertyModel> types = null;

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

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OneOfPropertyAllOfModel oneOfPropertyAllOf = (OneOfPropertyAllOfModel) o;
    return Objects.equals(this.types, oneOfPropertyAllOf.types);
  }

  @Override
  public int hashCode() {
    return Objects.hash(types);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OneOfPropertyAllOfModel {\n");
    sb.append("    types: ").append(toIndentedString(types)).append("\n");
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

