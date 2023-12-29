package com.bytechef.hermes.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.hermes.configuration.web.rest.model.OptionModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * The response object when dynamically resolving options.
 */

@Schema(name = "OptionsResponse", description = "The response object when dynamically resolving options.")
@JsonTypeName("OptionsResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2024-01-03T07:47:14.476436+01:00[Europe/Zagreb]")
public class OptionsResponseModel {

  private String errorMessage;

  @Valid
  private List<@Valid OptionModel> options;

  public OptionsResponseModel errorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
    return this;
  }

  /**
   * The error message.
   * @return errorMessage
  */
  
  @Schema(name = "errorMessage", description = "The error message.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("errorMessage")
  public String getErrorMessage() {
    return errorMessage;
  }

  public void setErrorMessage(String errorMessage) {
    this.errorMessage = errorMessage;
  }

  public OptionsResponseModel options(List<@Valid OptionModel> options) {
    this.options = options;
    return this;
  }

  public OptionsResponseModel addOptionsItem(OptionModel optionsItem) {
    if (this.options == null) {
      this.options = new ArrayList<>();
    }
    this.options.add(optionsItem);
    return this;
  }

  /**
   * Get options
   * @return options
  */
  @Valid 
  @Schema(name = "options", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("options")
  public List<@Valid OptionModel> getOptions() {
    return options;
  }

  public void setOptions(List<@Valid OptionModel> options) {
    this.options = options;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    OptionsResponseModel optionsResponse = (OptionsResponseModel) o;
    return Objects.equals(this.errorMessage, optionsResponse.errorMessage) &&
        Objects.equals(this.options, optionsResponse.options);
  }

  @Override
  public int hashCode() {
    return Objects.hash(errorMessage, options);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class OptionsResponseModel {\n");
    sb.append("    errorMessage: ").append(toIndentedString(errorMessage)).append("\n");
    sb.append("    options: ").append(toIndentedString(options)).append("\n");
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

