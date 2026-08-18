/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.platform.component.domain;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import org.jspecify.annotations.Nullable;

/**
 * Platform mirror of the SDK {@link com.bytechef.component.definition.AgentChannelDefinition}: functions and definition
 * objects collapse to names, exactly as {@link ActionDefinition}/{@link TriggerDefinition} do.
 * <p>
 * The trigger/reply binding is flattened rather than nested: {@code conversationIdPath}, {@code messagePath} and
 * {@code attachmentsPath} are copied from the trigger's {@code AgentRequestDefinition}, and {@code
 * replyMessageProperty}, {@code replyConversationIdProperty}, {@code replyAttachmentsProperty}, {@code
 * replyChannelParameters} and {@code replyFixedParameters} are copied from the reply action's {@code
 * AgentReplyDefinition}. The reply-half fields are null/empty as a block when the channel has no reply action.
 *
 * @author Ivica Cardic
 */
public class AgentChannelDefinition {

    private @Nullable String approvalChannelName;
    private @Nullable String attachmentsPath;
    private String componentName;
    private int componentVersion;
    private String conversationIdPath;
    private @Nullable String description;
    private String messagePath;
    private String name;
    private @Nullable String replyActionName;
    private @Nullable String replyAttachmentsProperty;
    private @Nullable String replyConversationIdProperty;
    private @Nullable String replyMessageProperty;
    private Map<String, String> replyChannelParameters;
    private Map<String, Object> replyFixedParameters;
    private @Nullable String title;
    private String triggerName;
    private Map<String, Object> triggerParameters;

    private AgentChannelDefinition() {
    }

    public AgentChannelDefinition(
        com.bytechef.component.definition.AgentChannelDefinition agentChannelDefinition, String componentName,
        int componentVersion) {

        this.approvalChannelName = agentChannelDefinition.getApprovalChannelName()
            .orElse(null);
        this.componentName = componentName;
        this.componentVersion = componentVersion;
        this.description = agentChannelDefinition.getDescription()
            .orElse(null);
        this.name = agentChannelDefinition.getName();
        this.replyActionName = agentChannelDefinition.getReplyActionName();
        this.title = agentChannelDefinition.getTitle()
            .orElse(null);
        this.triggerName = agentChannelDefinition.getTriggerName();
        // Insertion-ordered rather than Map.copyOf, for the reason the SDK's own setter documents: the generated agent
        // workflow carries these verbatim, and Map.copyOf's iteration order is salted per JVM run.
        this.triggerParameters = Collections.unmodifiableMap(
            new LinkedHashMap<>(agentChannelDefinition.getTriggerParameters()));

        // The binding, flattened. The request descriptor is always present (the SDK validates it at construction
        // time); the reply half is null/empty as a block when there is no reply action.
        com.bytechef.component.definition.AgentRequestDefinition agentRequestDefinition = agentChannelDefinition
            .getTrigger()
            .getAgentRequestDefinition()
            .orElseThrow();

        this.attachmentsPath = agentRequestDefinition.getAttachmentsPath()
            .orElse(null);
        this.conversationIdPath = agentRequestDefinition.getConversationIdPath();
        this.messagePath = agentRequestDefinition.getMessagePath();

        Optional<com.bytechef.component.definition.AgentReplyDefinition> agentReplyDefinition =
            agentChannelDefinition.getReplyAction()
                .flatMap(com.bytechef.component.definition.ActionDefinition::getAgentReplyDefinition);

        this.replyAttachmentsProperty = agentReplyDefinition
            .flatMap(com.bytechef.component.definition.AgentReplyDefinition::getAttachmentsProperty)
            .orElse(null);
        this.replyConversationIdProperty = agentReplyDefinition
            .flatMap(com.bytechef.component.definition.AgentReplyDefinition::getConversationIdProperty)
            .orElse(null);
        this.replyMessageProperty = agentReplyDefinition
            .map(com.bytechef.component.definition.AgentReplyDefinition::getMessageProperty)
            .orElse(null);
        Map<String, String> channelParameters = agentReplyDefinition
            .map(com.bytechef.component.definition.AgentReplyDefinition::getChannelParameters)
            .orElseGet(Map::of);
        Map<String, Object> fixedParameters = agentReplyDefinition
            .map(com.bytechef.component.definition.AgentReplyDefinition::getFixedParameters)
            .orElseGet(Map::of);

        // Copied insertion-ordered and wrapped unmodifiable for the reason triggerParameters is above: the generated
        // agent workflow carries these verbatim, and the SDK definition they are read from stays mutable.
        this.replyChannelParameters = Collections.unmodifiableMap(new LinkedHashMap<>(channelParameters));
        this.replyFixedParameters = Collections.unmodifiableMap(new LinkedHashMap<>(fixedParameters));
    }

    @Nullable
    public String getApprovalChannelName() {
        return approvalChannelName;
    }

    @Nullable
    public String getAttachmentsPath() {
        return attachmentsPath;
    }

    public String getComponentName() {
        return componentName;
    }

    public int getComponentVersion() {
        return componentVersion;
    }

    public String getConversationIdPath() {
        return conversationIdPath;
    }

    @Nullable
    public String getDescription() {
        return description;
    }

    public String getMessagePath() {
        return messagePath;
    }

    public String getName() {
        return name;
    }

    @Nullable
    public String getReplyActionName() {
        return replyActionName;
    }

    @Nullable
    public String getReplyAttachmentsProperty() {
        return replyAttachmentsProperty;
    }

    @Nullable
    public String getReplyConversationIdProperty() {
        return replyConversationIdProperty;
    }

    @Nullable
    public String getReplyMessageProperty() {
        return replyMessageProperty;
    }

    public Map<String, String> getReplyChannelParameters() {
        return replyChannelParameters;
    }

    public Map<String, Object> getReplyFixedParameters() {
        return replyFixedParameters;
    }

    @Nullable
    public String getTitle() {
        return title;
    }

    public String getTriggerName() {
        return triggerName;
    }

    public Map<String, Object> getTriggerParameters() {
        return triggerParameters;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }

        if (!(o instanceof AgentChannelDefinition that)) {
            return false;
        }

        return componentVersion == that.componentVersion
            && Objects.equals(approvalChannelName, that.approvalChannelName) &&
            Objects.equals(attachmentsPath, that.attachmentsPath) && Objects.equals(componentName, that.componentName)
            &&
            Objects.equals(conversationIdPath, that.conversationIdPath) &&
            Objects.equals(description, that.description) && Objects.equals(messagePath, that.messagePath) &&
            Objects.equals(name, that.name) && Objects.equals(replyActionName, that.replyActionName) &&
            Objects.equals(replyAttachmentsProperty, that.replyAttachmentsProperty) &&
            Objects.equals(replyConversationIdProperty, that.replyConversationIdProperty) &&
            Objects.equals(replyMessageProperty, that.replyMessageProperty) &&
            Objects.equals(replyChannelParameters, that.replyChannelParameters) &&
            Objects.equals(replyFixedParameters, that.replyFixedParameters) && Objects.equals(title, that.title) &&
            Objects.equals(triggerName, that.triggerName) && Objects.equals(triggerParameters, that.triggerParameters);
    }

    @Override
    public int hashCode() {
        return Objects.hash(
            approvalChannelName, attachmentsPath, componentName, componentVersion, conversationIdPath, description,
            messagePath, name, replyActionName, replyAttachmentsProperty, replyConversationIdProperty,
            replyMessageProperty, replyChannelParameters, replyFixedParameters, title, triggerName, triggerParameters);
    }

    @Override
    public String toString() {
        return "AgentChannelDefinition{" +
            "name='" + name + '\'' +
            ", title='" + title + '\'' +
            ", description='" + description + '\'' +
            ", componentName='" + componentName + '\'' +
            ", componentVersion=" + componentVersion +
            ", triggerName='" + triggerName + '\'' +
            ", replyActionName='" + replyActionName + '\'' +
            ", approvalChannelName='" + approvalChannelName + '\'' +
            ", conversationIdPath='" + conversationIdPath + '\'' +
            ", messagePath='" + messagePath + '\'' +
            ", attachmentsPath='" + attachmentsPath + '\'' +
            ", replyMessageProperty='" + replyMessageProperty + '\'' +
            ", replyConversationIdProperty='" + replyConversationIdProperty + '\'' +
            ", replyAttachmentsProperty='" + replyAttachmentsProperty + '\'' +
            ", replyChannelParameters=" + replyChannelParameters +
            ", replyFixedParameters=" + replyFixedParameters +
            ", triggerParameters=" + triggerParameters +
            '}';
    }
}
