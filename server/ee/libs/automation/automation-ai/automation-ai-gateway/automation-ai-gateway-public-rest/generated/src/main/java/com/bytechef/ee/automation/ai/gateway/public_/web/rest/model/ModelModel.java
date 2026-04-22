package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

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
 * A model object.
 */

@Schema(name = "Model", description = "A model object.")
@JsonTypeName("Model")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ModelModel {

  private @Nullable String id;

  private @Nullable String _object;

  private @Nullable String ownedBy;

  public ModelModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  public ModelModel _object(@Nullable String _object) {
    this._object = _object;
    return this;
  }

  /**
   * Get _object
   * @return _object
   */
  
  @Schema(name = "object", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("object")
  public @Nullable String getObject() {
    return _object;
  }

  public void setObject(@Nullable String _object) {
    this._object = _object;
  }

  public ModelModel ownedBy(@Nullable String ownedBy) {
    this.ownedBy = ownedBy;
    return this;
  }

  /**
   * Get ownedBy
   * @return ownedBy
   */
  
  @Schema(name = "owned_by", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("owned_by")
  public @Nullable String getOwnedBy() {
    return ownedBy;
  }

  public void setOwnedBy(@Nullable String ownedBy) {
    this.ownedBy = ownedBy;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ModelModel model = (ModelModel) o;
    return Objects.equals(this.id, model.id) &&
        Objects.equals(this._object, model._object) &&
        Objects.equals(this.ownedBy, model.ownedBy);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, _object, ownedBy);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ModelModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    _object: ").append(toIndentedString(_object)).append("\n");
    sb.append("    ownedBy: ").append(toIndentedString(ownedBy)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(@Nullable Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

