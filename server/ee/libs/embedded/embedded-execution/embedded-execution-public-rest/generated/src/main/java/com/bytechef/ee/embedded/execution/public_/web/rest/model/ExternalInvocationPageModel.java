package com.bytechef.ee.embedded.execution.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.execution.public_.web.rest.model.ExternalInvocationModel;
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
 * A page of a connected user&#39;s external invocations.
 */

@Schema(name = "ExternalInvocationPage", description = "A page of a connected user's external invocations.")
@JsonTypeName("ExternalInvocationPage")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-11-06T10:03:58.085959+01:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ExternalInvocationPageModel {

  private List<@Valid ExternalInvocationModel> content = new ArrayList<>();

  private @Nullable Integer number;

  private @Nullable Integer numberOfElements;

  private @Nullable Integer size;

  private @Nullable Integer totalElements;

  private @Nullable Integer totalPages;

  public ExternalInvocationPageModel content(List<@Valid ExternalInvocationModel> content) {
    this.content = content;
    return this;
  }

  public ExternalInvocationPageModel addContentItem(ExternalInvocationModel contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

  /**
   * The external invocations on this page.
   * @return content
   */
  
  @Schema(name = "content", description = "The external invocations on this page.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("content")
  public List<@Valid ExternalInvocationModel> getContent() {
    return content;
  }

  @JsonProperty("content")
  public void setContent(List<@Valid ExternalInvocationModel> content) {
    this.content = content;
  }

  public ExternalInvocationPageModel number(@Nullable Integer number) {
    this.number = number;
    return this;
  }

  /**
   * The current page.
   * @return number
   */
  
  @Schema(name = "number", description = "The current page.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("number")
  public @Nullable Integer getNumber() {
    return number;
  }

  @JsonProperty("number")
  public void setNumber(@Nullable Integer number) {
    this.number = number;
  }

  public ExternalInvocationPageModel numberOfElements(@Nullable Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
    return this;
  }

  /**
   * The number of elements.
   * @return numberOfElements
   */
  
  @Schema(name = "numberOfElements", description = "The number of elements.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("numberOfElements")
  public @Nullable Integer getNumberOfElements() {
    return numberOfElements;
  }

  @JsonProperty("numberOfElements")
  public void setNumberOfElements(@Nullable Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }

  public ExternalInvocationPageModel size(@Nullable Integer size) {
    this.size = size;
    return this;
  }

  /**
   * The size of the page.
   * @return size
   */
  
  @Schema(name = "size", description = "The size of the page.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("size")
  public @Nullable Integer getSize() {
    return size;
  }

  @JsonProperty("size")
  public void setSize(@Nullable Integer size) {
    this.size = size;
  }

  public ExternalInvocationPageModel totalElements(@Nullable Integer totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * The total number of elements.
   * @return totalElements
   */
  
  @Schema(name = "totalElements", description = "The total number of elements.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalElements")
  public @Nullable Integer getTotalElements() {
    return totalElements;
  }

  @JsonProperty("totalElements")
  public void setTotalElements(@Nullable Integer totalElements) {
    this.totalElements = totalElements;
  }

  public ExternalInvocationPageModel totalPages(@Nullable Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * The total number of pages.
   * @return totalPages
   */
  
  @Schema(name = "totalPages", description = "The total number of pages.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("totalPages")
  public @Nullable Integer getTotalPages() {
    return totalPages;
  }

  @JsonProperty("totalPages")
  public void setTotalPages(@Nullable Integer totalPages) {
    this.totalPages = totalPages;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExternalInvocationPageModel externalInvocationPage = (ExternalInvocationPageModel) o;
    return Objects.equals(this.content, externalInvocationPage.content) &&
        Objects.equals(this.number, externalInvocationPage.number) &&
        Objects.equals(this.numberOfElements, externalInvocationPage.numberOfElements) &&
        Objects.equals(this.size, externalInvocationPage.size) &&
        Objects.equals(this.totalElements, externalInvocationPage.totalElements) &&
        Objects.equals(this.totalPages, externalInvocationPage.totalPages);
  }

  @Override
  public int hashCode() {
    return Objects.hash(content, number, numberOfElements, size, totalElements, totalPages);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExternalInvocationPageModel {\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    numberOfElements: ").append(toIndentedString(numberOfElements)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
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

