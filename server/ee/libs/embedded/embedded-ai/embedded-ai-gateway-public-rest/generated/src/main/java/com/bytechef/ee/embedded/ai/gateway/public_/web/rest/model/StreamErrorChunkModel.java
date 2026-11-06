package com.bytechef.ee.embedded.ai.gateway.public_.web.rest.model;

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
 * The data of an SSE frame whose event is \&quot;error\&quot;. Sent when a request fails after the stream has already opened, so the failure cannot be reported as an HTTP status. Identity and connected-user rejections happen before the stream opens and arrive as a real 403 instead.
 */

@Schema(name = "StreamErrorChunk", description = "The data of an SSE frame whose event is \"error\". Sent when a request fails after the stream has already opened, so the failure cannot be reported as an HTTP status. Identity and connected-user rejections happen before the stream opens and arrive as a real 403 instead.")
@JsonTypeName("StreamErrorChunk")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-07T00:41:20.907024+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class StreamErrorChunkModel {

  private @Nullable String type;

  private @Nullable String message;

  public StreamErrorChunkModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * \"budget_exceeded\" for a budget hard limit, otherwise the simple class name of the underlying exception.
   * @return type
   */
  
  @Schema(name = "type", description = "\"budget_exceeded\" for a budget hard limit, otherwise the simple class name of the underlying exception.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  @JsonProperty("type")
  public void setType(@Nullable String type) {
    this.type = type;
  }

  public StreamErrorChunkModel message(@Nullable String message) {
    this.message = message;
    return this;
  }

  /**
   * Get message
   * @return message
   */
  
  @Schema(name = "message", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("message")
  public @Nullable String getMessage() {
    return message;
  }

  @JsonProperty("message")
  public void setMessage(@Nullable String message) {
    this.message = message;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    StreamErrorChunkModel streamErrorChunk = (StreamErrorChunkModel) o;
    return Objects.equals(this.type, streamErrorChunk.type) &&
        Objects.equals(this.message, streamErrorChunk.message);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, message);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class StreamErrorChunkModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    message: ").append(toIndentedString(message)).append("\n");
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

