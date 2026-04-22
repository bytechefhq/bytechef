package com.bytechef.ee.automation.ai.gateway.public_.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ChatMessageModel;
import com.bytechef.ee.automation.ai.gateway.public_.web.rest.model.ToolModel;
import com.fasterxml.jackson.annotation.JsonProperty;
import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonTypeName;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.List;
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
 * A chat completion request.
 */

@Schema(name = "ChatCompletionRequest", description = "A chat completion request.")
@JsonTypeName("ChatCompletionRequest")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-04-07T21:40:02.582901+02:00[Europe/Zagreb]", comments = "Generator version: 7.20.0")
public class ChatCompletionRequestModel {

  private String model;

  @Valid
  private List<@Valid ChatMessageModel> messages = new ArrayList<>();

  private @Nullable Double temperature;

  private @Nullable Integer maxTokens;

  private @Nullable Double topP;

  private Boolean stream = false;

  private @Nullable String routingPolicy;

  private @Nullable Boolean cache;

  private @Nullable Object toolChoice;

  @Valid
  private List<@Valid ToolModel> tools = new ArrayList<>();

  @Valid
  private Map<String, String> tags = new HashMap<>();

  public ChatCompletionRequestModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public ChatCompletionRequestModel(String model, List<@Valid ChatMessageModel> messages) {
    this.model = model;
    this.messages = messages;
  }

  public ChatCompletionRequestModel model(String model) {
    this.model = model;
    return this;
  }

  /**
   * The model to use in provider/model format.
   * @return model
   */
  @NotNull 
  @Schema(name = "model", description = "The model to use in provider/model format.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("model")
  public String getModel() {
    return model;
  }

  public void setModel(String model) {
    this.model = model;
  }

  public ChatCompletionRequestModel messages(List<@Valid ChatMessageModel> messages) {
    this.messages = messages;
    return this;
  }

  public ChatCompletionRequestModel addMessagesItem(ChatMessageModel messagesItem) {
    if (this.messages == null) {
      this.messages = new ArrayList<>();
    }
    this.messages.add(messagesItem);
    return this;
  }

  /**
   * The messages to generate chat completions for.
   * @return messages
   */
  @NotNull @Valid 
  @Schema(name = "messages", description = "The messages to generate chat completions for.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("messages")
  public List<@Valid ChatMessageModel> getMessages() {
    return messages;
  }

  public void setMessages(List<@Valid ChatMessageModel> messages) {
    this.messages = messages;
  }

  public ChatCompletionRequestModel temperature(@Nullable Double temperature) {
    this.temperature = temperature;
    return this;
  }

  /**
   * Sampling temperature between 0 and 2.
   * @return temperature
   */
  
  @Schema(name = "temperature", description = "Sampling temperature between 0 and 2.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("temperature")
  public @Nullable Double getTemperature() {
    return temperature;
  }

  public void setTemperature(@Nullable Double temperature) {
    this.temperature = temperature;
  }

  public ChatCompletionRequestModel maxTokens(@Nullable Integer maxTokens) {
    this.maxTokens = maxTokens;
    return this;
  }

  /**
   * Maximum number of tokens to generate.
   * @return maxTokens
   */
  
  @Schema(name = "max_tokens", description = "Maximum number of tokens to generate.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("max_tokens")
  public @Nullable Integer getMaxTokens() {
    return maxTokens;
  }

  public void setMaxTokens(@Nullable Integer maxTokens) {
    this.maxTokens = maxTokens;
  }

  public ChatCompletionRequestModel topP(@Nullable Double topP) {
    this.topP = topP;
    return this;
  }

  /**
   * Nucleus sampling parameter.
   * @return topP
   */
  
  @Schema(name = "top_p", description = "Nucleus sampling parameter.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("top_p")
  public @Nullable Double getTopP() {
    return topP;
  }

  public void setTopP(@Nullable Double topP) {
    this.topP = topP;
  }

  public ChatCompletionRequestModel stream(Boolean stream) {
    this.stream = stream;
    return this;
  }

  /**
   * Whether to stream the response.
   * @return stream
   */
  
  @Schema(name = "stream", description = "Whether to stream the response.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("stream")
  public Boolean getStream() {
    return stream;
  }

  public void setStream(Boolean stream) {
    this.stream = stream;
  }

  public ChatCompletionRequestModel routingPolicy(@Nullable String routingPolicy) {
    this.routingPolicy = routingPolicy;
    return this;
  }

  /**
   * The name of the routing policy to use.
   * @return routingPolicy
   */
  
  @Schema(name = "routing_policy", description = "The name of the routing policy to use.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("routing_policy")
  public @Nullable String getRoutingPolicy() {
    return routingPolicy;
  }

  public void setRoutingPolicy(@Nullable String routingPolicy) {
    this.routingPolicy = routingPolicy;
  }

  public ChatCompletionRequestModel cache(@Nullable Boolean cache) {
    this.cache = cache;
    return this;
  }

  /**
   * Whether to enable response caching.
   * @return cache
   */
  
  @Schema(name = "cache", description = "Whether to enable response caching.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("cache")
  public @Nullable Boolean getCache() {
    return cache;
  }

  public void setCache(@Nullable Boolean cache) {
    this.cache = cache;
  }

  public ChatCompletionRequestModel toolChoice(@Nullable Object toolChoice) {
    this.toolChoice = toolChoice;
    return this;
  }

  /**
   * Controls which tool is called by the model.
   * @return toolChoice
   */
  
  @Schema(name = "tool_choice", description = "Controls which tool is called by the model.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tool_choice")
  public @Nullable Object getToolChoice() {
    return toolChoice;
  }

  public void setToolChoice(@Nullable Object toolChoice) {
    this.toolChoice = toolChoice;
  }

  public ChatCompletionRequestModel tools(List<@Valid ToolModel> tools) {
    this.tools = tools;
    return this;
  }

  public ChatCompletionRequestModel addToolsItem(ToolModel toolsItem) {
    if (this.tools == null) {
      this.tools = new ArrayList<>();
    }
    this.tools.add(toolsItem);
    return this;
  }

  /**
   * A list of tools the model may call.
   * @return tools
   */
  @Valid 
  @Schema(name = "tools", description = "A list of tools the model may call.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tools")
  public List<@Valid ToolModel> getTools() {
    return tools;
  }

  public void setTools(List<@Valid ToolModel> tools) {
    this.tools = tools;
  }

  public ChatCompletionRequestModel tags(Map<String, String> tags) {
    this.tags = tags;
    return this;
  }

  public ChatCompletionRequestModel putTagsItem(String key, String tagsItem) {
    if (this.tags == null) {
      this.tags = new HashMap<>();
    }
    this.tags.put(key, tagsItem);
    return this;
  }

  /**
   * Custom tags for the request.
   * @return tags
   */
  
  @Schema(name = "tags", description = "Custom tags for the request.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("tags")
  public Map<String, String> getTags() {
    return tags;
  }

  public void setTags(Map<String, String> tags) {
    this.tags = tags;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    ChatCompletionRequestModel chatCompletionRequest = (ChatCompletionRequestModel) o;
    return Objects.equals(this.model, chatCompletionRequest.model) &&
        Objects.equals(this.messages, chatCompletionRequest.messages) &&
        Objects.equals(this.temperature, chatCompletionRequest.temperature) &&
        Objects.equals(this.maxTokens, chatCompletionRequest.maxTokens) &&
        Objects.equals(this.topP, chatCompletionRequest.topP) &&
        Objects.equals(this.stream, chatCompletionRequest.stream) &&
        Objects.equals(this.routingPolicy, chatCompletionRequest.routingPolicy) &&
        Objects.equals(this.cache, chatCompletionRequest.cache) &&
        Objects.equals(this.toolChoice, chatCompletionRequest.toolChoice) &&
        Objects.equals(this.tools, chatCompletionRequest.tools) &&
        Objects.equals(this.tags, chatCompletionRequest.tags);
  }

  @Override
  public int hashCode() {
    return Objects.hash(model, messages, temperature, maxTokens, topP, stream, routingPolicy, cache, toolChoice, tools, tags);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class ChatCompletionRequestModel {\n");
    sb.append("    model: ").append(toIndentedString(model)).append("\n");
    sb.append("    messages: ").append(toIndentedString(messages)).append("\n");
    sb.append("    temperature: ").append(toIndentedString(temperature)).append("\n");
    sb.append("    maxTokens: ").append(toIndentedString(maxTokens)).append("\n");
    sb.append("    topP: ").append(toIndentedString(topP)).append("\n");
    sb.append("    stream: ").append(toIndentedString(stream)).append("\n");
    sb.append("    routingPolicy: ").append(toIndentedString(routingPolicy)).append("\n");
    sb.append("    cache: ").append(toIndentedString(cache)).append("\n");
    sb.append("    toolChoice: ").append(toIndentedString(toolChoice)).append("\n");
    sb.append("    tools: ").append(toIndentedString(tools)).append("\n");
    sb.append("    tags: ").append(toIndentedString(tags)).append("\n");
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

