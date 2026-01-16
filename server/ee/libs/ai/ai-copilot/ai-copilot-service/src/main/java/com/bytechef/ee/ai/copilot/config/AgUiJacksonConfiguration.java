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

package com.bytechef.ee.ai.copilot.config;

import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import tools.jackson.databind.JacksonModule;
import tools.jackson.databind.module.SimpleModule;

/**
 * Jackson configuration for ag-ui message types to support polymorphic deserialization.
 *
 * @author Ivica Cardic
 */
@Configuration
@ConditionalOnProperty(prefix = "bytechef.ai.copilot", name = "enabled", havingValue = "true")
public class AgUiJacksonConfiguration {

    @Bean
    JacksonModule agUiJacksonModule() {
        SimpleModule module = new SimpleModule("AgUiModule");

        module.setMixInAnnotation(com.agui.core.message.BaseMessage.class, BaseMessageMixin.class);
        module.setMixInAnnotation(com.agui.core.event.BaseEvent.class, BaseEventMixin.class);
        module.setMixInAnnotation(com.agui.core.state.State.class, StateMixin.class);

        return module;
    }

    /**
     * Mixin for BaseMessage to enable polymorphic JSON deserialization. Maps the "role" field to concrete message
     * implementation classes.
     */
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "role")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = com.agui.core.message.AssistantMessage.class, name = "assistant"),
        @JsonSubTypes.Type(value = com.agui.core.message.DeveloperMessage.class, name = "developer"),
        @JsonSubTypes.Type(value = com.agui.core.message.UserMessage.class, name = "user"),
        @JsonSubTypes.Type(value = com.agui.core.message.SystemMessage.class, name = "system"),
        @JsonSubTypes.Type(value = com.agui.core.message.ToolMessage.class, name = "tool")
    })
    interface BaseMessageMixin {
    }

    /**
     * Mixin for BaseEvent to enable polymorphic JSON deserialization. Maps the "type" field to concrete event
     * implementation classes.
     */
    @JsonTypeInfo(
        use = JsonTypeInfo.Id.NAME,
        include = JsonTypeInfo.As.PROPERTY,
        property = "type")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = com.agui.core.event.CustomEvent.class, name = "CUSTOM"),
        @JsonSubTypes.Type(value = com.agui.core.event.MessagesSnapshotEvent.class, name = "MESSAGES_SNAPSHOT"),
        @JsonSubTypes.Type(value = com.agui.core.event.RawEvent.class, name = "RAW"),
        @JsonSubTypes.Type(value = com.agui.core.event.RunErrorEvent.class, name = "RUN_ERROR"),
        @JsonSubTypes.Type(value = com.agui.core.event.RunFinishedEvent.class, name = "RUN_FINISHED"),
        @JsonSubTypes.Type(value = com.agui.core.event.RunStartedEvent.class, name = "RUN_STARTED"),
        @JsonSubTypes.Type(value = com.agui.core.event.StateDeltaEvent.class, name = "STATE_DELTA"),
        @JsonSubTypes.Type(value = com.agui.core.event.StateSnapshotEvent.class, name = "STATE_SNAPSHOT"),
        @JsonSubTypes.Type(value = com.agui.core.event.StepFinishedEvent.class, name = "STEP_FINISHED"),
        @JsonSubTypes.Type(value = com.agui.core.event.StepStartedEvent.class, name = "STEP_STARTED"),
        @JsonSubTypes.Type(value = com.agui.core.event.TextMessageChunkEvent.class, name = "TEXT_MESSAGE_CHUNK"),
        @JsonSubTypes.Type(value = com.agui.core.event.TextMessageContentEvent.class, name = "TEXT_MESSAGE_CONTENT"),
        @JsonSubTypes.Type(value = com.agui.core.event.TextMessageEndEvent.class, name = "TEXT_MESSAGE_END"),
        @JsonSubTypes.Type(value = com.agui.core.event.TextMessageStartEvent.class, name = "TEXT_MESSAGE_START"),
        @JsonSubTypes.Type(value = com.agui.core.event.ThinkingEndEvent.class, name = "THINKING_END"),
        @JsonSubTypes.Type(value = com.agui.core.event.ThinkingStartEvent.class, name = "THINKING_START"),
        @JsonSubTypes.Type(
            value = com.agui.core.event.ThinkingTextMessageContentEvent.class, name = "THINKING_TEXT_MESSAGE_CONTENT"),
        @JsonSubTypes.Type(
            value = com.agui.core.event.ThinkingTextMessageEndEvent.class, name = "THINKING_TEXT_MESSAGE_END"),
        @JsonSubTypes.Type(
            value = com.agui.core.event.ThinkingTextMessageStartEvent.class, name = "THINKING_TEXT_MESSAGE_START"),
        @JsonSubTypes.Type(value = com.agui.core.event.ToolCallArgsEvent.class, name = "TOOL_CALL_ARGS"),
        @JsonSubTypes.Type(value = com.agui.core.event.ToolCallChunkEvent.class, name = "TOOL_CALL_CHUNK"),
        @JsonSubTypes.Type(value = com.agui.core.event.ToolCallEndEvent.class, name = "TOOL_CALL_END"),
        @JsonSubTypes.Type(value = com.agui.core.event.ToolCallResultEvent.class, name = "TOOL_CALL_RESULT"),
        @JsonSubTypes.Type(value = com.agui.core.event.ToolCallStartEvent.class, name = "TOOL_CALL_START")
    })
    interface BaseEventMixin {
    }

    /**
     * Mixin for State to enable dynamic JSON property handling. Uses @JsonAnyGetter and @JsonAnySetter for flexible
     * state management.
     */
    interface StateMixin {
        @com.fasterxml.jackson.annotation.JsonAnyGetter
        java.util.Map<String, Object> getState();

        @com.fasterxml.jackson.annotation.JsonAnySetter
        void set(String key, Object value);
    }
}
