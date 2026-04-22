package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * An embedding request.
 */

@Schema(name = "EmbeddingRequest", description = "An embedding request.")
@JsonTypeName("EmbeddingRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class EmbeddingRequestModel {

  private String model;

  @Valid
  private List<String> input = new ArrayList<>();

  @Valid
  private Map<String, String> tags = new HashMap<>();

  public EmbeddingRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public EmbeddingRequestModel(String model, List<String> input) {
    this.model = model;
    this.input = input;
  }

  public EmbeddingRequestModel model(String model) {
    this.model = model;
    return this;
  }

  /**
   * The model to use in provider/model format.
   * @return model
   */
  @NotNull 
  @Schema(name = "model", description = "The model to use in provider/model format.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("model")
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public EmbeddingRequestModel input(List<String> input) {
    this.input = input;
    return this;
  }

  public EmbeddingRequestModel addInputItem(String inputItem) {
    if (this.input == null) {
      this.input = new ArrayList<>();
    }
    this.input.add(inputItem);
    return this;
  }

  /**
   * The input texts to embed.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The input texts to embed.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public List<String> getInput() {
    return input;
  }

  public void setInput(List<String> input) {
    this.input = input;
  }

  public EmbeddingRequestModel tags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  public EmbeddingRequestModel putTagsItem(String key, String tagsItem) {
    if (this.tags == null) {
      this.tags = new HashMap<>();
    }
    this.tags.put(key, tagsItem);
    return this;
  }

  /**
   * Custom tags for the request.
   * @return tags
   */
  
  @Schema(name = "tags", description = "Custom tags for the request.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
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
    EmbeddingRequestModel embeddingRequest = (EmbeddingRequestModel) o;
    return Objects.equals(this.model, embeddingRequest.model) &&
        Objects.equals(this.input, embeddingRequest.input) &&
        Objects.equals(this.tags, embeddingRequest.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(model, input, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmbeddingRequestModel {\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

