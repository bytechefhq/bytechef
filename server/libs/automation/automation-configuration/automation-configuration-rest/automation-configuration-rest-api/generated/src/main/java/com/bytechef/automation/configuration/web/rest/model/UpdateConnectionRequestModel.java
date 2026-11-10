package com.bytechef.automation.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.automation.configuration.web.rest.model.TagModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * Contains all connection parameters that can be updated.
 */

@Schema(name = "UpdateConnectionRequest", description = "Contains all connection parameters that can be updated.")
@JsonTypeName("UpdateConnectionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-10T14:37:46.870052+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class UpdateConnectionRequestModel {

  private String name;

  private JsonNullable<Map<String, Object>> parameters = JsonNullable.<Map<String, Object>>undefined();

  private List<@Valid TagModel> tags = new ArrayList<>();

  private @Nullable Integer version;

  public UpdateConnectionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public UpdateConnectionRequestModel(String name, List<@Valid TagModel> tags) {
    this.name = name;
    this.tags = tags;
  }

  public UpdateConnectionRequestModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The name of a connection.
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The name of a connection.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public UpdateConnectionRequestModel parameters(Map<String, Object> parameters) {
    this.parameters = JsonNullable.of(parameters);
    return this;
  }

  public UpdateConnectionRequestModel putParametersItem(String key, Object parametersItem) {
    if (this.parameters == null || !this.parameters.isPresent() || this.parameters.get() == null) {
      this.parameters = JsonNullable.of(new HashMap<>());
    }
    this.parameters.get().put(key, parametersItem);
    return this;
  }

  /**
   * The values of the connection's own properties, excluding its authorization. When present, they replace the stored values and a property left out is cleared; when absent, the stored values are kept.
   * @return parameters
   */
  
  @Schema(name = "parameters", description = "The values of the connection's own properties, excluding its authorization. When present, they replace the stored values and a property left out is cleared; when absent, the stored values are kept.", requiredMode = Schema.RequiredMode.NOT_REQUIRED, nullable = true)
  @JsonProperty("parameters")
  public JsonNullable<Map<String, Object>> getParameters() {
    return parameters;
  }

  public void setParameters(JsonNullable<Map<String, Object>> parameters) {
    this.parameters = parameters;
  }

  public UpdateConnectionRequestModel tags(List<@Valid TagModel> tags) {
    this.tags = tags;
    return this;
  }

  public UpdateConnectionRequestModel addTagsItem(TagModel tagsItem) {
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
  @NotNull 
  @Schema(name = "tags", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("tags")
  public List<@Valid TagModel> getTags() {
    return tags;
  }

  @JsonProperty("tags")
  public void setTags(List<@Valid TagModel> tags) {
    this.tags = tags;
  }

  public UpdateConnectionRequestModel version(@Nullable Integer version) {
    this.version = version;
    return this;
  }

  /**
   * Get version
   * @return version
   */
  
  @Schema(name = "__version", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("__version")
  public @Nullable Integer getVersion() {
    return version;
  }

  @JsonProperty("__version")
  public void setVersion(@Nullable Integer version) {
    this.version = version;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    UpdateConnectionRequestModel updateConnectionRequest = (UpdateConnectionRequestModel) o;
    return Objects.equals(this.name, updateConnectionRequest.name) &&
        equalsNullable(this.parameters, updateConnectionRequest.parameters) &&
        Objects.equals(this.tags, updateConnectionRequest.tags) &&
        Objects.equals(this.version, updateConnectionRequest.version);
  }

  private static <T> boolean equalsNullable(JsonNullable<T> a, JsonNullable<T> b) {
    return a == b || (a != null && b != null && a.isPresent() && b.isPresent() && Objects.deepEquals(a.get(), b.get()));
  }

  @Override
  public int hashCode() {
    return Objects.hash(name, hashCodeNullable(parameters), tags, version);
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
    sb.append("class UpdateConnectionRequestModel {\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    parameters: ").append(toIndentedString(parameters)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
    sb.append("    version: ").append(toIndentedString(version)).append("\n");
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

