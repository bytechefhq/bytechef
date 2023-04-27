package com.bytechef.hermes.definition.registry.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionModel;
import com.bytechef.hermes.definition.registry.web.rest.model.OptionsDataSourceModel;
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
 * ArrayPropertyAllOfModel
 */

@JsonTypeName("ArrayProperty_allOf")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-04-27T08:00:36.899199+02:00[Europe/Zagreb]")
public class ArrayPropertyAllOfModel {

  @Valid
  private List<@Valid PropertyModel> items;

  private Boolean multipleValues;

  @Valid
  private List<@Valid OptionModel> options;

  private OptionsDataSourceModel optionsDataSource;

  public ArrayPropertyAllOfModel items(List<@Valid PropertyModel> items) {
    this.items = items;
    return this;
  }

  public ArrayPropertyAllOfModel addItemsItem(PropertyModel itemsItem) {
    if (this.items == null) {
      this.items = new ArrayList<>();
    }
    this.items.add(itemsItem);
    return this;
  }

  /**
   * Types of the array items.
   * @return items
  */
  @Valid 
  @Schema(name = "items", description = "Types of the array items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("items")
  public List<@Valid PropertyModel> getItems() {
    return items;
  }

  public void setItems(List<@Valid PropertyModel> items) {
    this.items = items;
  }

  public ArrayPropertyAllOfModel multipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
    return this;
  }

  /**
   * If the array can contain multiple items.
   * @return multipleValues
  */
  
  @Schema(name = "multipleValues", description = "If the array can contain multiple items.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("multipleValues")
  public Boolean getMultipleValues() {
    return multipleValues;
  }

  public void setMultipleValues(Boolean multipleValues) {
    this.multipleValues = multipleValues;
  }

  public ArrayPropertyAllOfModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public ArrayPropertyAllOfModel addOptionsItem(OptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * The list of valid property options.
   * @return options
  */
  @Valid 
  @Schema(name = "options", description = "The list of valid property options.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  public ArrayPropertyAllOfModel optionsDataSource(OptionsDataSourceModel optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
    return this;
  }

  /**
   * Get optionsDataSource
   * @return optionsDataSource
  */
  @Valid 
  @Schema(name = "optionsDataSource", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("optionsDataSource")
  public OptionsDataSourceModel getOptionsDataSource() {
    return optionsDataSource;
  }

  public void setOptionsDataSource(OptionsDataSourceModel optionsDataSource) {
    this.optionsDataSource = optionsDataSource;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ArrayPropertyAllOfModel arrayPropertyAllOf = (ArrayPropertyAllOfModel) o;
    return Objects.equals(this.items, arrayPropertyAllOf.items) &&
        Objects.equals(this.multipleValues, arrayPropertyAllOf.multipleValues) &&
        Objects.equals(this.options, arrayPropertyAllOf.options) &&
        Objects.equals(this.optionsDataSource, arrayPropertyAllOf.optionsDataSource);
  }

  @Override
  public int hashCode() {
    return Objects.hash(items, multipleValues, options, optionsDataSource);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ArrayPropertyAllOfModel {\n");
    sb.append("    items: ").append(toIndentedString(items)).append("\n");
    sb.append("    multipleValues: ").append(toIndentedString(multipleValues)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
    sb.append("    optionsDataSource: ").append(toIndentedString(optionsDataSource)).append("\n");
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

