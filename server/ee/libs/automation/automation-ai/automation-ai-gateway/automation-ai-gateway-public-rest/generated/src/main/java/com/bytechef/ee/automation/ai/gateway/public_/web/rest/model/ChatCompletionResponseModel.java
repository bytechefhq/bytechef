package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChoiceModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.UsageModel;
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
 * A chat completion response.
 */

@Schema(name = "ChatCompletionResponse", description = "A chat completion response.")
@JsonTypeName("ChatCompletionResponse")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ChatCompletionResponseModel {

  private @Nullable String id;

  private @Nullable String _object;

  private @Nullable Long created;

  private @Nullable String model;

  @Valid
  private List<@Valid ChoiceModel> choices = new ArrayList<>();

  private @Nullable UsageModel usage;

  public ChatCompletionResponseModel id(@Nullable String id) {
    this.id = id;
    return this;
  }

  /**
   * Get id
   * @return id
   */
  
  @Schema(name = "id", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("id")
  public @Nullable String getId() {
    return id;
  }

  public void setId(@Nullable String id) {
    this.id = id;
  }

  public ChatCompletionResponseModel _object(@Nullable String _object) {
    this._object = _object;
    return this;
  }

  /**
   * Get _object
   * @return _object
   */
  
  @Schema(name = "object", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("object")
  public @Nullable String getObject() {
    return _object;
  }

  public void setObject(@Nullable String _object) {
    this._object = _object;
  }

  public ChatCompletionResponseModel created(@Nullable Long created) {
    this.created = created;
    return this;
  }

  /**
   * Get created
   * @return created
   */
  
  @Schema(name = "created", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("created")
  public @Nullable Long getCreated() {
    return created;
  }

  public void setCreated(@Nullable Long created) {
    this.created = created;
  }

  public ChatCompletionResponseModel model(@Nullable String model) {
    this.model = model;
    return this;
  }

  /**
   * Get model
   * @return model
   */
  
  @Schema(name = "model", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("model")
  public @Nullable String getModel() {
    return model;
  }

  public void setModel(@Nullable String model) {
    this.model = model;
  }

  public ChatCompletionResponseModel choices(List<@Valid ChoiceModel> choices) {
    this.choices = choices;
    return this;
  }

  public ChatCompletionResponseModel addChoicesItem(ChoiceModel choicesItem) {
    if (this.choices == null) {
      this.choices = new ArrayList<>();
    }
    this.choices.add(choicesItem);
    return this;
  }

  /**
   * Get choices
   * @return choices
   */
  @Valid 
  @Schema(name = "choices", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("choices")
  public List<@Valid ChoiceModel> getChoices() {
    return choices;
  }

  public void setChoices(List<@Valid ChoiceModel> choices) {
    this.choices = choices;
  }

  public ChatCompletionResponseModel usage(@Nullable UsageModel usage) {
    this.usage = usage;
    return this;
  }

  /**
   * Get usage
   * @return usage
   */
  @Valid 
  @Schema(name = "usage", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("usage")
  public @Nullable UsageModel getUsage() {
    return usage;
  }

  public void setUsage(@Nullable UsageModel usage) {
    this.usage = usage;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatCompletionResponseModel chatCompletionResponse = (ChatCompletionResponseModel) o;
    return Objects.equals(this.id, chatCompletionResponse.id) &&
        Objects.equals(this._object, chatCompletionResponse._object) &&
        Objects.equals(this.created, chatCompletionResponse.created) &&
        Objects.equals(this.model, chatCompletionResponse.model) &&
        Objects.equals(this.choices, chatCompletionResponse.choices) &&
        Objects.equals(this.usage, chatCompletionResponse.usage);
  }

  @Override
  public int hashCode() {
    return Objects.hash(id, _object, created, model, choices, usage);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChatCompletionResponseModel {\n");
    sb.append("    id: ").append(toIndentedString(id)).append("\n");
    sb.append("    _object: ").append(toIndentedString(_object)).append("\n");
    sb.append("    created: ").append(toIndentedString(created)).append("\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    choices: ").append(toIndentedString(choices)).append("\n");
    sb.append("    usage: ").append(toIndentedString(usage)).append("\n");
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

