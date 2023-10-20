package com.bytechef.atlas.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * A sublist of a list of objects. It allows gain information about the position of it in the containing entire list.
 */

@Schema(name = "Page", description = "A sublist of a list of objects. It allows gain information about the position of it in the containing entire list.")
@JsonTypeName("Page")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2023-01-18T08:53:27.088924+01:00[Europe/Zagreb]")
public class PageModel {

  @JsonProperty("number")
  private Integer number;

  @JsonProperty("size")
  private Integer size;

  @JsonProperty("numberOfElements")
  private Integer numberOfElements;

  @JsonProperty("totalPages")
  private Integer totalPages;

  @JsonProperty("totalElements")
  private Integer totalElements;

  @JsonProperty("content")
  @Valid
  private List<Object> content = null;

  public PageModel number(Integer number) {
    this.number = number;
    return this;
  }

  /**
   * The current page.
   * @return number
  */
  
  @Schema(name = "number", description = "The current page.", required = false)
  public Integer getNumber() {
    return number;
  }

  public void setNumber(Integer number) {
    this.number = number;
  }

  public PageModel size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * The size of the page.
   * @return size
  */
  
  @Schema(name = "size", description = "The size of the page.", required = false)
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public PageModel numberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
    return this;
  }

  /**
   * The number of elements.
   * @return numberOfElements
  */
  
  @Schema(name = "numberOfElements", description = "The number of elements.", required = false)
  public Integer getNumberOfElements() {
    return numberOfElements;
  }

  public void setNumberOfElements(Integer numberOfElements) {
    this.numberOfElements = numberOfElements;
  }

  public PageModel totalPages(Integer totalPages) {
    this.totalPages = totalPages;
    return this;
  }

  /**
   * The total number of pages.
   * @return totalPages
  */
  
  @Schema(name = "totalPages", description = "The total number of pages.", required = false)
  public Integer getTotalPages() {
    return totalPages;
  }

  public void setTotalPages(Integer totalPages) {
    this.totalPages = totalPages;
  }

  public PageModel totalElements(Integer totalElements) {
    this.totalElements = totalElements;
    return this;
  }

  /**
   * The total number of elements.
   * @return totalElements
  */
  
  @Schema(name = "totalElements", description = "The total number of elements.", required = false)
  public Integer getTotalElements() {
    return totalElements;
  }

  public void setTotalElements(Integer totalElements) {
    this.totalElements = totalElements;
  }

  public PageModel content(List<Object> content) {
    this.content = content;
    return this;
  }

  public PageModel addContentItem(Object contentItem) {
    if (this.content == null) {
      this.content = new ArrayList<>();
    }
    this.content.add(contentItem);
    return this;
  }

  /**
   * List of elements.
   * @return content
  */
  
  @Schema(name = "content", description = "List of elements.", required = false)
  public List<Object> getContent() {
    return content;
  }

  public void setContent(List<Object> content) {
    this.content = content;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    PageModel page = (PageModel) o;
    return Objects.equals(this.number, page.number) &&
        Objects.equals(this.size, page.size) &&
        Objects.equals(this.numberOfElements, page.numberOfElements) &&
        Objects.equals(this.totalPages, page.totalPages) &&
        Objects.equals(this.totalElements, page.totalElements) &&
        Objects.equals(this.content, page.content);
  }

  @Override
  public int hashCode() {
    return Objects.hash(number, size, numberOfElements, totalPages, totalElements, content);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class PageModel {\n");
    sb.append("    number: ").append(toIndentedString(number)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    numberOfElements: ").append(toIndentedString(numberOfElements)).append("\n");
    sb.append("    totalPages: ").append(toIndentedString(totalPages)).append("\n");
    sb.append("    totalElements: ").append(toIndentedString(totalElements)).append("\n");
    sb.append("    content: ").append(toIndentedString(content)).append("\n");
    sb.append("}");
    return sb.toString();
  }

  /**
   * Convert the given object to string with each line indented by 4 spaces
   * (except the first line).
   */
  private String toIndentedString(Object o) {
    if (o == null) {
      return "null";
    }
    return o.toString().replace("\n", "\n    ");
  }
}

