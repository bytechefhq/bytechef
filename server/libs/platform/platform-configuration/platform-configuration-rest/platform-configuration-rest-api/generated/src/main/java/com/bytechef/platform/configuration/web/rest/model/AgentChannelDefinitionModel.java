package com.bytechef.platform.configuration.web.rest.model;

import java.net.URI;
import java.util.Objects;
import com.fasterxml.jackson.annotation.JsonInclude;
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
 * A trigger/reply-action pair through which an AI Agent can be reached.
 */

@Schema(name = "AgentChannelDefinition", description = "A trigger/reply-action pair through which an AI Agent can be reached.")
@JsonTypeName("AgentChannelDefinition")
@Generated(value = "org.openapitools.codegen.languages.SpringCodegen", date = "2026-08-18T08:42:35.953453+02:00[Europe/Zagreb]", comments = "Generator version: 7.24.0")
public class AgentChannelDefinitionModel {

  private @Nullable String approvalChannelName;

  private @Nullable String attachmentsPath;

  private String componentName;

  private Integer componentVersion;

  private String conversationIdPath;

  private @Nullable String description;

  private String messagePath;

  private String name;

  private @Nullable String replyActionName;

  private @Nullable String replyAttachmentsProperty;

  private Map<String, String> replyChannelParameters = new HashMap<>();

  private @Nullable String replyConversationIdProperty;

  private Map<String, Object> replyFixedParameters = new HashMap<>();

  private @Nullable String replyMessageProperty;

  private @Nullable String title;

  private String triggerName;

  private Map<String, Object> triggerParameters = new HashMap<>();

  public AgentChannelDefinitionModel() {
    super();
  }

  /**
   * Constructor with only required parameters
   */
  public AgentChannelDefinitionModel(String componentName, Integer componentVersion, String conversationIdPath, String messagePath, String name, String triggerName) {
    this.componentName = componentName;
    this.componentVersion = componentVersion;
    this.conversationIdPath = conversationIdPath;
    this.messagePath = messagePath;
    this.name = name;
    this.triggerName = triggerName;
  }

  public AgentChannelDefinitionModel approvalChannelName(@Nullable String approvalChannelName) {
    this.approvalChannelName = approvalChannelName;
    return this;
  }

  /**
   * The name of the approval cluster element used to gate replies on this channel.
   * @return approvalChannelName
   */
  
  @Schema(name = "approvalChannelName", description = "The name of the approval cluster element used to gate replies on this channel.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("approvalChannelName")
  public @Nullable String getApprovalChannelName() {
    return approvalChannelName;
  }

  @JsonProperty("approvalChannelName")
  public void setApprovalChannelName(@Nullable String approvalChannelName) {
    this.approvalChannelName = approvalChannelName;
  }

  public AgentChannelDefinitionModel attachmentsPath(@Nullable String attachmentsPath) {
    this.attachmentsPath = attachmentsPath;
    return this;
  }

  /**
   * The path, within the trigger's payload, to the incoming message's attachments.
   * @return attachmentsPath
   */
  
  @Schema(name = "attachmentsPath", description = "The path, within the trigger's payload, to the incoming message's attachments.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("attachmentsPath")
  public @Nullable String getAttachmentsPath() {
    return attachmentsPath;
  }

  @JsonProperty("attachmentsPath")
  public void setAttachmentsPath(@Nullable String attachmentsPath) {
    this.attachmentsPath = attachmentsPath;
  }

  public AgentChannelDefinitionModel componentName(String componentName) {
    this.componentName = componentName;
    return this;
  }

  /**
   * The name of the component that declares this channel.
   * @return componentName
   */
  @NotNull 
  @Schema(name = "componentName", description = "The name of the component that declares this channel.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentName")
  public String getComponentName() {
    return componentName;
  }

  @JsonProperty("componentName")
  public void setComponentName(String componentName) {
    this.componentName = componentName;
  }

  public AgentChannelDefinitionModel componentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
    return this;
  }

  /**
   * The version of the component that declares this channel.
   * @return componentVersion
   */
  @NotNull 
  @Schema(name = "componentVersion", description = "The version of the component that declares this channel.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("componentVersion")
  public Integer getComponentVersion() {
    return componentVersion;
  }

  @JsonProperty("componentVersion")
  public void setComponentVersion(Integer componentVersion) {
    this.componentVersion = componentVersion;
  }

  public AgentChannelDefinitionModel conversationIdPath(String conversationIdPath) {
    this.conversationIdPath = conversationIdPath;
    return this;
  }

  /**
   * The path, within the trigger's payload, to the conversation id.
   * @return conversationIdPath
   */
  @NotNull 
  @Schema(name = "conversationIdPath", description = "The path, within the trigger's payload, to the conversation id.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("conversationIdPath")
  public String getConversationIdPath() {
    return conversationIdPath;
  }

  @JsonProperty("conversationIdPath")
  public void setConversationIdPath(String conversationIdPath) {
    this.conversationIdPath = conversationIdPath;
  }

  public AgentChannelDefinitionModel description(@Nullable String description) {
    this.description = description;
    return this;
  }

  /**
   * The description.
   * @return description
   */
  
  @Schema(name = "description", description = "The description.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("description")
  public @Nullable String getDescription() {
    return description;
  }

  @JsonProperty("description")
  public void setDescription(@Nullable String description) {
    this.description = description;
  }

  public AgentChannelDefinitionModel messagePath(String messagePath) {
    this.messagePath = messagePath;
    return this;
  }

  /**
   * The path, within the trigger's payload, to the incoming message text.
   * @return messagePath
   */
  @NotNull 
  @Schema(name = "messagePath", description = "The path, within the trigger's payload, to the incoming message text.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("messagePath")
  public String getMessagePath() {
    return messagePath;
  }

  @JsonProperty("messagePath")
  public void setMessagePath(String messagePath) {
    this.messagePath = messagePath;
  }

  public AgentChannelDefinitionModel name(String name) {
    this.name = name;
    return this;
  }

  /**
   * The channel's stored key (matches `ai_agent_channel.channel_type`).
   * @return name
   */
  @NotNull 
  @Schema(name = "name", description = "The channel's stored key (matches `ai_agent_channel.channel_type`).", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("name")
  public String getName() {
    return name;
  }

  @JsonProperty("name")
  public void setName(String name) {
    this.name = name;
  }

  public AgentChannelDefinitionModel replyActionName(@Nullable String replyActionName) {
    this.replyActionName = replyActionName;
    return this;
  }

  /**
   * The name of the reply action, if this channel supports replying.
   * @return replyActionName
   */
  
  @Schema(name = "replyActionName", description = "The name of the reply action, if this channel supports replying.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyActionName")
  public @Nullable String getReplyActionName() {
    return replyActionName;
  }

  @JsonProperty("replyActionName")
  public void setReplyActionName(@Nullable String replyActionName) {
    this.replyActionName = replyActionName;
  }

  public AgentChannelDefinitionModel replyAttachmentsProperty(@Nullable String replyAttachmentsProperty) {
    this.replyAttachmentsProperty = replyAttachmentsProperty;
    return this;
  }

  /**
   * The reply action property that carries outgoing attachments.
   * @return replyAttachmentsProperty
   */
  
  @Schema(name = "replyAttachmentsProperty", description = "The reply action property that carries outgoing attachments.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyAttachmentsProperty")
  public @Nullable String getReplyAttachmentsProperty() {
    return replyAttachmentsProperty;
  }

  @JsonProperty("replyAttachmentsProperty")
  public void setReplyAttachmentsProperty(@Nullable String replyAttachmentsProperty) {
    this.replyAttachmentsProperty = replyAttachmentsProperty;
  }

  public AgentChannelDefinitionModel replyChannelParameters(Map<String, String> replyChannelParameters) {
    this.replyChannelParameters = replyChannelParameters;
    return this;
  }

  public AgentChannelDefinitionModel putReplyChannelParametersItem(String key, String replyChannelParametersItem) {
    if (this.replyChannelParameters == null) {
      this.replyChannelParameters = new HashMap<>();
    }
    this.replyChannelParameters.put(key, replyChannelParametersItem);
    return this;
  }

  /**
   * Reply action parameters mapped from trigger property row keys.
   * @return replyChannelParameters
   */
  
  @Schema(name = "replyChannelParameters", description = "Reply action parameters mapped from trigger property row keys.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyChannelParameters")
  public Map<String, String> getReplyChannelParameters() {
    return replyChannelParameters;
  }

  @JsonProperty("replyChannelParameters")
  public void setReplyChannelParameters(Map<String, String> replyChannelParameters) {
    this.replyChannelParameters = replyChannelParameters;
  }

  public AgentChannelDefinitionModel replyConversationIdProperty(@Nullable String replyConversationIdProperty) {
    this.replyConversationIdProperty = replyConversationIdProperty;
    return this;
  }

  /**
   * The reply action property that carries the conversation id.
   * @return replyConversationIdProperty
   */
  
  @Schema(name = "replyConversationIdProperty", description = "The reply action property that carries the conversation id.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyConversationIdProperty")
  public @Nullable String getReplyConversationIdProperty() {
    return replyConversationIdProperty;
  }

  @JsonProperty("replyConversationIdProperty")
  public void setReplyConversationIdProperty(@Nullable String replyConversationIdProperty) {
    this.replyConversationIdProperty = replyConversationIdProperty;
  }

  public AgentChannelDefinitionModel replyFixedParameters(Map<String, Object> replyFixedParameters) {
    this.replyFixedParameters = replyFixedParameters;
    return this;
  }

  public AgentChannelDefinitionModel putReplyFixedParametersItem(String key, Object replyFixedParametersItem) {
    if (this.replyFixedParameters == null) {
      this.replyFixedParameters = new HashMap<>();
    }
    this.replyFixedParameters.put(key, replyFixedParametersItem);
    return this;
  }

  /**
   * Reply action parameters fixed to constant values.
   * @return replyFixedParameters
   */
  
  @Schema(name = "replyFixedParameters", description = "Reply action parameters fixed to constant values.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyFixedParameters")
  public Map<String, Object> getReplyFixedParameters() {
    return replyFixedParameters;
  }

  @JsonProperty("replyFixedParameters")
  public void setReplyFixedParameters(Map<String, Object> replyFixedParameters) {
    this.replyFixedParameters = replyFixedParameters;
  }

  public AgentChannelDefinitionModel replyMessageProperty(@Nullable String replyMessageProperty) {
    this.replyMessageProperty = replyMessageProperty;
    return this;
  }

  /**
   * The reply action property that carries the outgoing message text.
   * @return replyMessageProperty
   */
  
  @Schema(name = "replyMessageProperty", description = "The reply action property that carries the outgoing message text.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("replyMessageProperty")
  public @Nullable String getReplyMessageProperty() {
    return replyMessageProperty;
  }

  @JsonProperty("replyMessageProperty")
  public void setReplyMessageProperty(@Nullable String replyMessageProperty) {
    this.replyMessageProperty = replyMessageProperty;
  }

  public AgentChannelDefinitionModel title(@Nullable String title) {
    this.title = title;
    return this;
  }

  /**
   * The title
   * @return title
   */
  
  @Schema(name = "title", description = "The title", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("title")
  public @Nullable String getTitle() {
    return title;
  }

  @JsonProperty("title")
  public void setTitle(@Nullable String title) {
    this.title = title;
  }

  public AgentChannelDefinitionModel triggerName(String triggerName) {
    this.triggerName = triggerName;
    return this;
  }

  /**
   * The name of the trigger through which incoming messages arrive.
   * @return triggerName
   */
  @NotNull 
  @Schema(name = "triggerName", description = "The name of the trigger through which incoming messages arrive.", requiredMode = Schema.RequiredMode.REQUIRED)
  @JsonProperty("triggerName")
  public String getTriggerName() {
    return triggerName;
  }

  @JsonProperty("triggerName")
  public void setTriggerName(String triggerName) {
    this.triggerName = triggerName;
  }

  public AgentChannelDefinitionModel triggerParameters(Map<String, Object> triggerParameters) {
    this.triggerParameters = triggerParameters;
    return this;
  }

  public AgentChannelDefinitionModel putTriggerParametersItem(String key, Object triggerParametersItem) {
    if (this.triggerParameters == null) {
      this.triggerParameters = new HashMap<>();
    }
    this.triggerParameters.put(key, triggerParametersItem);
    return this;
  }

  /**
   * Fixed parameters applied to the trigger when it is used as an agent channel.
   * @return triggerParameters
   */
  
  @Schema(name = "triggerParameters", description = "Fixed parameters applied to the trigger when it is used as an agent channel.", requiredMode = Schema.RequiredMode.NOT_REQUIRED)
  @JsonProperty("triggerParameters")
  public Map<String, Object> getTriggerParameters() {
    return triggerParameters;
  }

  @JsonProperty("triggerParameters")
  public void setTriggerParameters(Map<String, Object> triggerParameters) {
    this.triggerParameters = triggerParameters;
  }

  @Override
  public boolean equals(Object o) {
    if (this == o) {
      return true;
    }
    if (o == null || getClass() != o.getClass()) {
      return false;
    }
    AgentChannelDefinitionModel agentChannelDefinition = (AgentChannelDefinitionModel) o;
    return Objects.equals(this.approvalChannelName, agentChannelDefinition.approvalChannelName) &&
        Objects.equals(this.attachmentsPath, agentChannelDefinition.attachmentsPath) &&
        Objects.equals(this.componentName, agentChannelDefinition.componentName) &&
        Objects.equals(this.componentVersion, agentChannelDefinition.componentVersion) &&
        Objects.equals(this.conversationIdPath, agentChannelDefinition.conversationIdPath) &&
        Objects.equals(this.description, agentChannelDefinition.description) &&
        Objects.equals(this.messagePath, agentChannelDefinition.messagePath) &&
        Objects.equals(this.name, agentChannelDefinition.name) &&
        Objects.equals(this.replyActionName, agentChannelDefinition.replyActionName) &&
        Objects.equals(this.replyAttachmentsProperty, agentChannelDefinition.replyAttachmentsProperty) &&
        Objects.equals(this.replyChannelParameters, agentChannelDefinition.replyChannelParameters) &&
        Objects.equals(this.replyConversationIdProperty, agentChannelDefinition.replyConversationIdProperty) &&
        Objects.equals(this.replyFixedParameters, agentChannelDefinition.replyFixedParameters) &&
        Objects.equals(this.replyMessageProperty, agentChannelDefinition.replyMessageProperty) &&
        Objects.equals(this.title, agentChannelDefinition.title) &&
        Objects.equals(this.triggerName, agentChannelDefinition.triggerName) &&
        Objects.equals(this.triggerParameters, agentChannelDefinition.triggerParameters);
  }

  @Override
  public int hashCode() {
    return Objects.hash(approvalChannelName, attachmentsPath, componentName, componentVersion, conversationIdPath, description, messagePath, name, replyActionName, replyAttachmentsProperty, replyChannelParameters, replyConversationIdProperty, replyFixedParameters, replyMessageProperty, title, triggerName, triggerParameters);
  }

  @Override
  public String toString() {
    StringBuilder sb = new StringBuilder();
    sb.append("class AgentChannelDefinitionModel {\n");
    sb.append("    approvalChannelName: ").append(toIndentedString(approvalChannelName)).append("\n");
    sb.append("    attachmentsPath: ").append(toIndentedString(attachmentsPath)).append("\n");
    sb.append("    componentName: ").append(toIndentedString(componentName)).append("\n");
    sb.append("    componentVersion: ").append(toIndentedString(componentVersion)).append("\n");
    sb.append("    conversationIdPath: ").append(toIndentedString(conversationIdPath)).append("\n");
    sb.append("    description: ").append(toIndentedString(description)).append("\n");
    sb.append("    messagePath: ").append(toIndentedString(messagePath)).append("\n");
    sb.append("    name: ").append(toIndentedString(name)).append("\n");
    sb.append("    replyActionName: ").append(toIndentedString(replyActionName)).append("\n");
    sb.append("    replyAttachmentsProperty: ").append(toIndentedString(replyAttachmentsProperty)).append("\n");
    sb.append("    replyChannelParameters: ").append(toIndentedString(replyChannelParameters)).append("\n");
    sb.append("    replyConversationIdProperty: ").append(toIndentedString(replyConversationIdProperty)).append("\n");
    sb.append("    replyFixedParameters: ").append(toIndentedString(replyFixedParameters)).append("\n");
    sb.append("    replyMessageProperty: ").append(toIndentedString(replyMessageProperty)).append("\n");
    sb.append("    title: ").append(toIndentedString(title)).append("\n");
    sb.append("    triggerName: ").append(toIndentedString(triggerName)).append("\n");
    sb.append("    triggerParameters: ").append(toIndentedString(triggerParameters)).append("\n");
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

