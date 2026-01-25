package com.bytechef.ee.embedded.unified.web.rest.crm.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.unified.web.rest.crm.model.ErrorsInnerModel;
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
 * ListAccounts400ResponseModel
 */

@JsonTypeName("listAccounts_400_response")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-01-23T12:37:00.707409+01:00[Europe/Zagreb]", comments = "Generator version: 7.18.0")
public class ListAccounts400ResponseModel {

  @Valid
  private List<@Valid ErrorsInnerModel> errors = new ArrayList<>();

  public ListAccounts400ResponseModel errors(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
    return this;
  }

  public ListAccounts400ResponseModel addErrorsItem(ErrorsInnerModel errorsItem) {
    if (this.errors == null) {
      this.errors = new ArrayList<>();
    }
    this.errors.add(errorsItem);
    return this;
  }

  /**
   * Get errors
   * @return errors
   */
  @Valid 
  @Schema(name = "errors", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errors")
  public List<@Valid ErrorsInnerModel> getErrors() {
    return errors;
  }

  public void setErrors(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ListAccounts400ResponseModel listAccounts400Response = (ListAccounts400ResponseModel) o;
    return Objects.equals(this.errors, listAccounts400Response.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ListAccounts400ResponseModel {\n");
    sb.append("    errors: ").append(toIndentedString(errors)).append("\n");
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

