package com.bytechef.ee.automation.data.table.public_.web.rest.model;

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
 * RenameColumnRequestModel
 */

@JsonTypeName("RenameColumnRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class RenameColumnRequestModel {

  private String newName;

  public RenameColumnRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public RenameColumnRequestModel(String newName) {
    this.newName = newName;
  }

  public RenameColumnRequestModel newName(String newName) {
    this.newName = newName;
    return this;
  }

  /**
   * Get newName
   * @return newName
   */
  @NotNull @Pattern(regexp = "^[a-z_][a-z0-9_]*$") 
  @Schema(name = "newName", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("newName")
  public String getNewName() {
    return newName;
  }

  @JsonProperty("newName")
  public void setNewName(String newName) {
    this.newName = newName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    RenameColumnRequestModel renameColumnRequest = (RenameColumnRequestModel) o;
    return Objects.equals(this.newName, renameColumnRequest.newName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(newName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class RenameColumnRequestModel {\n");
    sb.append("    newName: ").append(toIndentedString(newName)).append("\n");
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

