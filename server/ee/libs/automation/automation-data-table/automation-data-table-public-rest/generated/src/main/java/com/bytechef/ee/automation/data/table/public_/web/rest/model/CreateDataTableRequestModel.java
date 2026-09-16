package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.data.table.public_.web.rest.model.DataTableColumnModel;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * CreateDataTableRequestModel
 */

@JsonTypeName("CreateDataTableRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class CreateDataTableRequestModel {

  private String name;

  private @Nullable String description;

  private List<@Valid DataTableColumnModel> columns = new ArrayList<>();

  private List<String> tags = new ArrayList<>();

  public CreateDataTableRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateDataTableRequestModel(String name, List<@Valid DataTableColumnModel> columns) {
    this.name = name;
    this.columns = columns;
  }

  public CreateDataTableRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * Lower-case letters, digits and underscores, not starting with a digit or `dt_`.
   * @return name
   */
  @NotNull @Pattern(regexp = "^[a-z_][a-z0-9_]*$") 
  @Schema(name = "name", description = "Lower-case letters, digits and underscores, not starting with a digit or `dt_`.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public CreateDataTableRequestModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * Get description
   * @return description
   */
  
  @Schema(name = "description", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public CreateDataTableRequestModel columns(List<@Valid DataTableColumnModel> columns) {
    this.columns = columns;
    return this;
  }

  public CreateDataTableRequestModel addColumnsItem(DataTableColumnModel columnsItem) {
    if (this.columns == null) {
      this.columns = new ArrayList<>();
    }
    this.columns.add(columnsItem);
    return this;
  }

  /**
   * Get columns
   * @return columns
   */
  @NotNull @Valid @Size(min = 1) 
  @Schema(name = "columns", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("columns")
  public List<@Valid DataTableColumnModel> getColumns() {
    return columns;
  }

  @JsonProperty("columns")
  public void setColumns(List<@Valid DataTableColumnModel> columns) {
    this.columns = columns;
  }

  public CreateDataTableRequestModel tags(List<String> tags) {
    this.tags = tags;
    return this;
  }

  public CreateDataTableRequestModel addTagsItem(String tagsItem) {
    if (this.tags == null) {
      this.tags = new ArrayList<>();
    }
    this.tags.add(tagsItem);
    return this;
  }

  /**
   * Get tags
   * @return tags
   */
  
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public List<String> getTags() {
    return tags;
  }

  @JsonProperty("tags")
  public void setTags(List<String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateDataTableRequestModel createDataTableRequest = (CreateDataTableRequestModel) o;
    return Objects.equals(this.name, createDataTableRequest.name) &&
        Objects.equals(this.description, createDataTableRequest.description) &&
        Objects.equals(this.columns, createDataTableRequest.columns) &&
        Objects.equals(this.tags, createDataTableRequest.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, description, columns, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateDataTableRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    columns: ").append(toIndentedString(columns)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

