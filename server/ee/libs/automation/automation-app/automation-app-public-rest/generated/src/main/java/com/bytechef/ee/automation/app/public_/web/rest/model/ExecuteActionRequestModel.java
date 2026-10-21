package com.bytechef.ee.automation.app.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.HashMap;
import java.util.Map;
import org.springframework.lang.Nullable;
import org.openapitools.jackson.nullable.JsonNullable;
import java.time.OffsetDateTime;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import io.swagger.v3.oas.annotations.media.Schema;


import java.util.*;
import jakarta.annotation.Generated;

/**
 * ExecuteActionRequestModel
 */

@JsonTypeName("executeAction_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-10-21T02:28:31.849354+02:00[Europe/Zagreb]", comments = "Generator version: 7.26.0")
public class ExecuteActionRequestModel {

  private @Nullable Integer componentVersion;

  private Map<String, Object> input = new HashMap<>();

  public ExecuteActionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ExecuteActionRequestModel(Map<String, Object> input) {
    this.input = input;
  }

  public ExecuteActionRequestModel componentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The component version; the latest when omitted.
   * @return componentVersion
   */
  
  @Schema(name = "componentVersion", description = "The component version; the latest when omitted.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("componentVersion")
  public @Nullable Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(@Nullable Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public ExecuteActionRequestModel input(Map<String, Object> input) {
    this.input = input;
    return this;
  }

  public ExecuteActionRequestModel putInputItem(String key, Object inputItem) {
    if (this.input == null) {
      this.input = new HashMap<>();
    }
    this.input.put(key, inputItem);
    return this;
  }

  /**
   * The action input.
   * @return input
   */
  @NotNull 
  @Schema(name = "input", description = "The action input.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("input")
  public Map<String, Object> getInput() {
    return input;
  }

  @JsonProperty("input")
  public void setInput(Map<String, Object> input) {
    this.input = input;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ExecuteActionRequestModel executeActionRequest = (ExecuteActionRequestModel) o;
    return Objects.equals(this.componentVersion, executeActionRequest.componentVersion) &&
        Objects.equals(this.input, executeActionRequest.input);
  }

  @Override
  public int hashCode() {
    return Objects.hash(componentVersion, input);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ExecuteActionRequestModel {\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    input: ").append(toIndentedString(input)).append("\n");
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

