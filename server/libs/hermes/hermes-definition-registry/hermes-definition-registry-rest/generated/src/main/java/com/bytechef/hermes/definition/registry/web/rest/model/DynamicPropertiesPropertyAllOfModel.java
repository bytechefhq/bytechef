package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import org.openapitools.jackson.nullable.JsonNullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * DynamicPropertiesPropertyAllOfModel
 */

@JsonTypeName("DynamicPropertiesProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-03-05T16:27:34.189599+01:00[Europe/Zagreb]")
public class DynamicPropertiesPropertyAllOfModel {

  @JsonProperty("propertiesDataSource")
  private JsonNullable<Object> propertiesDataSource = JsonNullable.undefined();

  public DynamicPropertiesPropertyAllOfModel propertiesDataSource(Object propertiesDataSource) {
    this.propertiesDataSource = JsonNullable.of(propertiesDataSource);
    return this;
  }

  /**
   * Get propertiesDataSource
   * @return propertiesDataSource
  */
  
  @Schema(name = "propertiesDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  public JsonNullable<Object> getPropertiesDataSource() {
    return propertiesDataSource;
  }

  public void setPropertiesDataSource(JsonNullable<Object> propertiesDataSource) {
    this.propertiesDataSource = propertiesDataSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    DynamicPropertiesPropertyAllOfModel dynamicPropertiesPropertyAllOf = (DynamicPropertiesPropertyAllOfModel) o;
    return equalsNullable(this.propertiesDataSource, dynamicPropertiesPropertyAllOf.propertiesDataSource);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(hashCodeNullable(propertiesDataSource));
  }

  private static <T> int hashCodeNullable(JsonNullable<T> a) {
    if (a == null) {
      return 1;
    }
    return a.isPresent() ? Arrays.deepHashCode(new Object[]{a.get()}) : 31;
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class DynamicPropertiesPropertyAllOfModel {\n");
    sb.append("    propertiesDataSource: ").append(toIndentedString(propertiesDataSource)).append("\n");
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

