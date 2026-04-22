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
 * Token usage for embedding.
 */

@Schema(name = "EmbeddingUsage", description = "Token usage for embedding.")
@JsonTypeName("EmbeddingUsage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class EmbeddingUsageModel {

  private @Nullable Integer promptTokens;

  private @Nullable Integer totalTokens;

  public EmbeddingUsageModel promptTokens(@Nullable Integer promptTokens) {
    this.promptTokens = promptTokens;
    return this;
  }

  /**
   * Get promptTokens
   * @return promptTokens
   */
  
  @Schema(name = "prompt_tokens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("prompt_tokens")
  public @Nullable Integer getPromptTokens() {
    return promptTokens;
  }

  public void setPromptTokens(@Nullable Integer promptTokens) {
    this.promptTokens = promptTokens;
  }

  public EmbeddingUsageModel totalTokens(@Nullable Integer totalTokens) {
    this.totalTokens = totalTokens;
    return this;
  }

  /**
   * Get totalTokens
   * @return totalTokens
   */
  
  @Schema(name = "total_tokens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("total_tokens")
  public @Nullable Integer getTotalTokens() {
    return totalTokens;
  }

  public void setTotalTokens(@Nullable Integer totalTokens) {
    this.totalTokens = totalTokens;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    EmbeddingUsageModel embeddingUsage = (EmbeddingUsageModel) o;
    return Objects.equals(this.promptTokens, embeddingUsage.promptTokens) &&
        Objects.equals(this.totalTokens, embeddingUsage.totalTokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(promptTokens, totalTokens);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class EmbeddingUsageModel {\n");
    sb.append("    promptTokens: ").append(toIndentedString(promptTokens)).append("\n");
    sb.append("    totalTokens: ").append(toIndentedString(totalTokens)).append("\n");
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

