package com.bytechef.ee.embedded.unified.web.rest.accounting.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.embedded.unified.web.rest.accounting.model.ErrorsInnerModel;
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
 * InlineObjectModel
 */

@JsonTypeName("inline_object")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2025-08-26T09:58:14.997427+02:00[Europe/Zagreb]", comments = "Generator version: 7.14.0")
public class InlineObjectModel {

  @Valid
  private List<@Valid ErrorsInnerModel> errors = new ArrayList<>();

  public InlineObjectModel errors(List<@Valid ErrorsInnerModel> errors) {
    this.errors = errors;
    return this;
  }

  public InlineObjectModel addErrorsItem(ErrorsInnerModel errorsItem) {
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
    InlineObjectModel inlineObject = (InlineObjectModel) o;
    return Objects.equals(this.errors, inlineObject.errors);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errors);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class InlineObjectModel {\n");
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

