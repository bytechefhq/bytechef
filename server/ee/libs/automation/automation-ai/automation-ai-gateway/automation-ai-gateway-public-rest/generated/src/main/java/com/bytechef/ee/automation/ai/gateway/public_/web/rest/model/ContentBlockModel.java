package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ContentBlockImageUrlModel;
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
 * A content block for multimodal messages.
 */

@Schema(name = "ContentBlock", description = "A content block for multimodal messages.")
@JsonTypeName("ContentBlock")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ContentBlockModel {

  private @Nullable String type;

  private @Nullable String text;

  private @Nullable ContentBlockImageUrlModel imageUrl;

  public ContentBlockModel type(@Nullable String type) {
    this.type = type;
    return this;
  }

  /**
   * The type of content block.
   * @return type
   */
  
  @Schema(name = "type", description = "The type of content block.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("type")
  public @Nullable String getType() {
    return type;
  }

  public void setType(@Nullable String type) {
    this.type = type;
  }

  public ContentBlockModel text(@Nullable String text) {
    this.text = text;
    return this;
  }

  /**
   * The text content.
   * @return text
   */
  
  @Schema(name = "text", description = "The text content.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("text")
  public @Nullable String getText() {
    return text;
  }

  public void setText(@Nullable String text) {
    this.text = text;
  }

  public ContentBlockModel imageUrl(@Nullable ContentBlockImageUrlModel imageUrl) {
    this.imageUrl = imageUrl;
    return this;
  }

  /**
   * Get imageUrl
   * @return imageUrl
   */
  @Valid 
  @Schema(name = "image_url", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("image_url")
  public @Nullable ContentBlockImageUrlModel getImageUrl() {
    return imageUrl;
  }

  public void setImageUrl(@Nullable ContentBlockImageUrlModel imageUrl) {
    this.imageUrl = imageUrl;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ContentBlockModel contentBlock = (ContentBlockModel) o;
    return Objects.equals(this.type, contentBlock.type) &&
        Objects.equals(this.text, contentBlock.text) &&
        Objects.equals(this.imageUrl, contentBlock.imageUrl);
  }

  @Override
  public int hashCode() {
    return Objects.hash(type, text, imageUrl);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ContentBlockModel {\n");
    sb.append("    type: ").append(toIndentedString(type)).append("\n");
    sb.append("    text: ").append(toIndentedString(text)).append("\n");
    sb.append("    imageUrl: ").append(toIndentedString(imageUrl)).append("\n");
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

