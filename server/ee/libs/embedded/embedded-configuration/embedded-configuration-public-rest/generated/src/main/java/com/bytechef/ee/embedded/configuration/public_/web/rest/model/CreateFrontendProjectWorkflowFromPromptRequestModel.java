package com.bytechef.ee.embedded.configuration.public_.web.rest.model;

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
 * CreateFrontendProjectWorkflowFromPromptRequestModel
 */

@JsonTypeName("createFrontendProjectWorkflowFromPrompt_request")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-06-06T14:44:54.600950+02:00[Europe/Zagreb]", comments = "Generator version: 7.21.0")
public class CreateFrontendProjectWorkflowFromPromptRequestModel {

  private String prompt;

  public CreateFrontendProjectWorkflowFromPromptRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public CreateFrontendProjectWorkflowFromPromptRequestModel(String prompt) {
    this.prompt = prompt;
  }

  public CreateFrontendProjectWorkflowFromPromptRequestModel prompt(String prompt) {
    this.prompt = prompt;
    return this;
  }

  /**
   * Natural language description of the workflow to build.
   * @return prompt
   */
  @NotNull 
  @Schema(name = "prompt", description = "Natural language description of the workflow to build.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("prompt")
  public String getPrompt() {
    return prompt;
  }

  @JsonProperty("prompt")
  public void setPrompt(String prompt) {
    this.prompt = prompt;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    CreateFrontendProjectWorkflowFromPromptRequestModel createFrontendProjectWorkflowFromPromptRequest = (CreateFrontendProjectWorkflowFromPromptRequestModel) o;
    return Objects.equals(this.prompt, createFrontendProjectWorkflowFromPromptRequest.prompt);
  }

  @Override
  public int hashCode() {
    return Objects.hash(prompt);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class CreateFrontendProjectWorkflowFromPromptRequestModel {\n");
    sb.append("    prompt: ").append(toIndentedString(prompt)).append("\n");
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

