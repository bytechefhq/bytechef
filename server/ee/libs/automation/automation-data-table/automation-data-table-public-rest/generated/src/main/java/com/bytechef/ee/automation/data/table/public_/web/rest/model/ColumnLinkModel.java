package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import com.fasterxml.jackson.annotation.JsonValue;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A many-to-one link to the rows of another data table in the same workspace.
 */

@Schema(name = "ColumnLink", description = "A many-to-one link to the rows of another data table in the same workspace.")
@JsonTypeName("ColumnLink")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-15T03:07:08.789973+01:00[Europe/Zagreb]", comments = "Generator version: 7.27.0")
public class ColumnLinkModel {

  private Long targetDataTableId;

  /**
   * BLOCK refuses deleting a linked row; CLEAR empties the link.
   */
  public enum OnDeleteEnum {
    BLOCK("BLOCK"),
    
    CLEAR("CLEAR");

    private final String value;

    OnDeleteEnum(String value) {
      this.value = value;
    }

    @JsonValue
    public String getValue() {
      return value;
    }

    @Override
    public String toString() {
      return String.valueOf(value);
    }

    @JsonCreator
    public static OnDeleteEnum fromValue(String value) {
      for (OnDeleteEnum b : OnDeleteEnum.values()) {
        if (b.value.equals(value)) {
          return b;
        }
      }
      throw new IllegalArgumentException("Unexpected value '" + value + "'");
    }
  }

  private OnDeleteEnum onDelete = OnDeleteEnum.BLOCK;

  private @Nullable String displayColumnName;

  public ColumnLinkModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ColumnLinkModel(Long targetDataTableId) {
    this.targetDataTableId = targetDataTableId;
  }

  public ColumnLinkModel targetDataTableId(Long targetDataTableId) {
    this.targetDataTableId = targetDataTableId;
    return this;
  }

  /**
   * Get targetDataTableId
   * @return targetDataTableId
   */
  @NotNull 
  @Schema(name = "targetDataTableId", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("targetDataTableId")
  public Long getTargetDataTableId() {
    return targetDataTableId;
  }

  @JsonProperty("targetDataTableId")
  public void setTargetDataTableId(Long targetDataTableId) {
    this.targetDataTableId = targetDataTableId;
  }

  public ColumnLinkModel onDelete(OnDeleteEnum onDelete) {
    this.onDelete = onDelete;
    return this;
  }

  /**
   * BLOCK refuses deleting a linked row; CLEAR empties the link.
   * @return onDelete
   */
  
  @Schema(name = "onDelete", description = "BLOCK refuses deleting a linked row; CLEAR empties the link.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("onDelete")
  public OnDeleteEnum getOnDelete() {
    return onDelete;
  }

  @JsonProperty("onDelete")
  public void setOnDelete(OnDeleteEnum onDelete) {
    this.onDelete = onDelete;
  }

  public ColumnLinkModel displayColumnName(@Nullable String displayColumnName) {
    this.displayColumnName = displayColumnName;
    return this;
  }

  /**
   * The target column shown for a linked row.
   * @return displayColumnName
   */
  
  @Schema(name = "displayColumnName", description = "The target column shown for a linked row.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("displayColumnName")
  public @Nullable String getDisplayColumnName() {
    return displayColumnName;
  }

  @JsonProperty("displayColumnName")
  public void setDisplayColumnName(@Nullable String displayColumnName) {
    this.displayColumnName = displayColumnName;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ColumnLinkModel columnLink = (ColumnLinkModel) o;
    return Objects.equals(this.targetDataTableId, columnLink.targetDataTableId) &&
        Objects.equals(this.onDelete, columnLink.onDelete) &&
        Objects.equals(this.displayColumnName, columnLink.displayColumnName);
  }

  @Override
  public int hashCode() {
    return Objects.hash(targetDataTableId, onDelete, displayColumnName);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ColumnLinkModel {\n");
    sb.append("    targetDataTableId: ").append(toIndentedString(targetDataTableId)).append("\n");
    sb.append("    onDelete: ").append(toIndentedString(onDelete)).append("\n");
    sb.append("    displayColumnName: ").append(toIndentedString(displayColumnName)).append("\n");
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

