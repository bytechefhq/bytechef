package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChatMessageModel;
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
 * A chat completion choice.
 */

@Schema(name = "Choice", description = "A chat completion choice.")
@JsonTypeName("Choice")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ChoiceModel {

  private @Nullable Integer index;

  private @Nullable ChatMessageModel message;

  private @Nullable String finishReason;

  public ChoiceModel index(@Nullable Integer index) {
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

  public ChoiceModel message(@Nullable ChatMessageModel message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  @Valid 
  @Schema(name = "message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable ChatMessageModel getMessage() {
    return message;
  }

  public void setMessage(@Nullable ChatMessageModel message) {
    this.message = message;
  }

  public ChoiceModel finishReason(@Nullable String finishReason) {
    this.finishReason = finishReason;
    return this;
  }

  /**
   * Get finishReason
   * @return finishReason
   */
  
  @Schema(name = "finish_reason", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("finish_reason")
  public @Nullable String getFinishReason() {
    return finishReason;
  }

  public void setFinishReason(@Nullable String finishReason) {
    this.finishReason = finishReason;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChoiceModel choice = (ChoiceModel) o;
    return Objects.equals(this.index, choice.index) &&
        Objects.equals(this.message, choice.message) &&
        Objects.equals(this.finishReason, choice.finishReason);
  }

  @Override
  public int hashCode() {
    return Objects.hash(index, message, finishReason);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChoiceModel {\n");
    sb.append("    index: ").append(toIndentedString(index)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
    sb.append("    finishReason: ").append(toIndentedString(finishReason)).append("\n");
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

