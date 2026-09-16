package com.bytechef.ee.automation.data.table.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.Arrays;
import java.util.HashMap;
import java.util.Map;
import org.openapitools.jackson.nullable.JsonNullable;
import org.springframework.lang.Nullable;
import java.util.NoSuchElementException;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * UpdateRowRequestModel
 */

@JsonTypeName("UpdateRowRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-09-03T11:52:44.578282+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class UpdateRowRequestModel {

  private Map<String, Object> values = new HashMap<>();

  private JsonNullable<@Size(max = 255) String> externalId = JsonNullable.<String>undefined();

  public UpdateRowRequestModel values(Map<String, Object> values) {
    this.values = values;
    return this;
  }

  public UpdateRowRequestModel putValuesItem(String key, Object valuesItem) {
    if (this.values == null) {
      this.values = new HashMap<>();
    }
    this.values.put(key, valuesItem);
    return this;
  }

  /**
   * Get values
   * @return values
   */
  
  @Schema(name = "values", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("values")
  public Map<String, Object> getValues() {
    return values;
  }

  @JsonProperty("values")
  public void setValues(Map<String, Object> values) {
    this.values = values;
  }

  public UpdateRowRequestModel externalId(String externalId) {
    this.externalId = JsonNullable.of(externalId);
    return this;
  }

  /**
   * Set the key; send null to clear it; omit to leave it.
   * @return externalId
   */
  @Size(max = 255) 
  @Schema(name = "externalId", description = "Set the key; send null to clear it; omit to leave it.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("externalId")
  public JsonNullable<@Size(max = 255) String> getExternalId() {
    return externalId;
  }

  public void setExternalId(JsonNullable<String> externalId) {
    this.externalId = externalId;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateRowRequestModel updateRowRequest = (UpdateRowRequestModel) o;
    return Objects.equals(this.values, updateRowRequest.values) &&
        equalsNullable(this.externalId, updateRowRequest.externalId);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(values, hashCodeNullable(externalId));
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
    sb.append("class UpdateRowRequestModel {\n");
    sb.append("    values: ").append(toIndentedString(values)).append("\n");
    sb.append("    externalId: ").append(toIndentedString(externalId)).append("\n");
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

