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
 * Token usage information.
 */

@Schema(name = "Usage", description = "Token usage information.")
@JsonTypeName("Usage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class UsageModel {

  private @Nullable Long promptTokens;

  private @Nullable Long completionTokens;

  private @Nullable Long totalTokens;

  public UsageModel promptTokens(@Nullable Long promptTokens) {
    this.promptTokens = promptTokens;
    return this;
  }

  /**
   * Get promptTokens
   * @return promptTokens
   */
  
  @Schema(name = "prompt_tokens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("prompt_tokens")
  public @Nullable Long getPromptTokens() {
    return promptTokens;
  }

  public void setPromptTokens(@Nullable Long promptTokens) {
    this.promptTokens = promptTokens;
  }

  public UsageModel completionTokens(@Nullable Long completionTokens) {
    this.completionTokens = completionTokens;
    return this;
  }

  /**
   * Get completionTokens
   * @return completionTokens
   */
  
  @Schema(name = "completion_tokens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("completion_tokens")
  public @Nullable Long getCompletionTokens() {
    return completionTokens;
  }

  public void setCompletionTokens(@Nullable Long completionTokens) {
    this.completionTokens = completionTokens;
  }

  public UsageModel totalTokens(@Nullable Long totalTokens) {
    this.totalTokens = totalTokens;
    return this;
  }

  /**
   * Get totalTokens
   * @return totalTokens
   */
  
  @Schema(name = "total_tokens", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("total_tokens")
  public @Nullable Long getTotalTokens() {
    return totalTokens;
  }

  public void setTotalTokens(@Nullable Long totalTokens) {
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
    UsageModel usage = (UsageModel) o;
    return Objects.equals(this.promptTokens, usage.promptTokens) &&
        Objects.equals(this.completionTokens, usage.completionTokens) &&
        Objects.equals(this.totalTokens, usage.totalTokens);
  }

  @Override
  public int hashCode() {
    return Objects.hash(promptTokens, completionTokens, totalTokens);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class UsageModel {\n");
    sb.append("    promptTokens: ").append(toIndentedString(promptTokens)).append("\n");
    sb.append("    completionTokens: ").append(toIndentedString(completionTokens)).append("\n");
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

