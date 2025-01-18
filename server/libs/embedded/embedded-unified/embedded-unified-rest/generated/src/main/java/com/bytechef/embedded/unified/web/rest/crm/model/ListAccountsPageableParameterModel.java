package com.bytechef.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ListAccountsPageableParameterModel
 */

@JsonTypeName("listAccounts_pageable_parameter")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-01-09T18:54:19.403746+01:00[Europe/Zagreb]", comments = "Generator version: 7.10.0")
public class ListAccountsPageableParameterModel {

  private String direction;

  private String sort;

  private Integer size;

  private String continuationToken;

  public ListAccountsPageableParameterModel direction(String direction) {
    this.direction = direction;
    return this;
  }

  /**
   * The direction parameter.
   * @return direction
   */
  
  @Schema(name = "direction", description = "The direction parameter.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("direction")
  public String getDirection() {
    return direction;
  }

  public void setDirection(String direction) {
    this.direction = direction;
  }

  public ListAccountsPageableParameterModel sort(String sort) {
    this.sort = sort;
    return this;
  }

  /**
   * The sorting parameter.
   * @return sort
   */
  
  @Schema(name = "sort", description = "The sorting parameter.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("sort")
  public String getSort() {
    return sort;
  }

  public void setSort(String sort) {
    this.sort = sort;
  }

  public ListAccountsPageableParameterModel size(Integer size) {
    this.size = size;
    return this;
  }

  /**
   * The number of items to be returned.
   * @return size
   */
  
  @Schema(name = "size", description = "The number of items to be returned.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("size")
  public Integer getSize() {
    return size;
  }

  public void setSize(Integer size) {
    this.size = size;
  }

  public ListAccountsPageableParameterModel continuationToken(String continuationToken) {
    this.continuationToken = continuationToken;
    return this;
  }

  /**
   * The the continuationToken parameter.
   * @return continuationToken
   */
  
  @Schema(name = "continuationToken", description = "The the continuationToken parameter.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("continuationToken")
  public String getContinuationToken() {
    return continuationToken;
  }

  public void setContinuationToken(String continuationToken) {
    this.continuationToken = continuationToken;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListAccountsPageableParameterModel listAccountsPageableParameter = (ListAccountsPageableParameterModel) o;
    return Objects.equals(this.direction, listAccountsPageableParameter.direction) &&
        Objects.equals(this.sort, listAccountsPageableParameter.sort) &&
        Objects.equals(this.size, listAccountsPageableParameter.size) &&
        Objects.equals(this.continuationToken, listAccountsPageableParameter.continuationToken);
  }

  @Override
  public int hashCode() {
    return Objects.hash(direction, sort, size, continuationToken);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListAccountsPageableParameterModel {\n");
    sb.append("    direction: ").append(toIndentedString(direction)).append("\n");
    sb.append("    sort: ").append(toIndentedString(sort)).append("\n");
    sb.append("    size: ").append(toIndentedString(size)).append("\n");
    sb.append("    continuationToken: ").append(toIndentedString(continuationToken)).append("\n");
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

