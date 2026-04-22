package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
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
 * A single embedding result.
 */

@Schema(name = "EmbeddingData", description = "A single embedding result.")
@JsonTypeName("EmbeddingData")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class EmbeddingDataModel {

  private @Nullable String _object;

  private @Nullable Integer index;

  @Valid
  private List<Float> embedding = new ArrayList<>();

  public EmbeddingDataModel _object(@Nullable String _object) {
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

  public EmbeddingDataModel index(@Nullable Integer index) {
    this.index = index;
    return this;
  }

  /**
   * Get index
   * @return index
   */
  
  @Schema(name = "index", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("index")
  public @Nullable Integer getIndex() {
    return index;
  }

  public void setIndex(@Nullable Integer index) {
    this.index = index;
  }

  public EmbeddingDataModel embedding(List<Float> embedding) {
    this.embedding = embedding;
    return this;
  }

  public EmbeddingDataModel addEmbeddingItem(Float embeddingItem) {
    if (this.embedding == null) {
      this.embedding = new ArrayList<>();
    }
    this.embedding.add(embeddingItem);
    return this;
  }

  /**
   * Get embedding
   * @return embedding
   */
  
  @Schema(name = "embedding", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("embedding")
  public List<Float> getEmbedding() {
    return embedding;
  }

  public void setEmbedding(List<Float> embedding) {
    this.embedding = embedding;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmbeddingDataModel embeddingData = (EmbeddingDataModel) o;
    return Objects.equals(this._object, embeddingData._object) &&
        Objects.equals(this.index, embeddingData.index) &&
        Objects.equals(this.embedding, embeddingData.embedding);
  }

  @Override
  public int hashCode() {
    return Objects.hash(_object, index, embedding);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmbeddingDataModel {\n");
    sb.append("    _object: ").append(toIndentedString(_object)).append("\n");
    sb.append("    index: ").append(toIndentedString(index)).append("\n");
    sb.append("    embedding: ").append(toIndentedString(embedding)).append("\n");
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

