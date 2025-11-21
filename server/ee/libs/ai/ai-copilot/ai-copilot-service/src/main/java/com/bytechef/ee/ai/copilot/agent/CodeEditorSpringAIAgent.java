/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeEditorSpringAIAgent extends SpringAIAgent {

    protected CodeEditorSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Map<?, ?> parameters = (Map<?, ?>) state.get("parameters");

        String systemPrompt = switch ((String) parameters.get("language")) {
            case "javascript" -> "You are a javascript code generator, answer only with code.";
            case "python" -> "You are a python code generator, answer only with code.";
            case "ruby" -> "You are a ruby code generator, answer only with code.";
            default -> throw new IllegalStateException("Unexpected value: " + parameters.get("language"));
        };

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String message = "%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            systemPrompt, state, String.join("%n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    public static class Builder extends SpringAIAgent.Builder {

        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);

            return this;
        }

        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            return this;
        }

        public Builder advisor(Advisor advisor) {
            super.advisor(advisor);

            return this;
        }

        public Builder tools(List<Object> tools) {
            super.tools(tools);

            return this;
        }

        public SpringAIAgent.Builder tool(Object tool) {
            super.tool(tool);

            return this;
        }

        public Builder agentId(String agentId) {
            super.agentId(agentId);

            return this;
        }

        public Builder state(State state) {
            super.state(state);

            return this;
        }

        public Builder toolCallbacks(List<ToolCallback> toolCallbacks) {
            super.toolCallbacks(toolCallbacks);

            return this;
        }

        public Builder toolCallback(ToolCallback toolCallback) {
            super.toolCallback(toolCallback);

            return this;
        }

        public Builder systemMessage(String systemMessage) {
            super.systemMessage(systemMessage);

            return this;
        }

        public Builder systemMessageProvider(Function<LocalAgent, String> systemMessageProvider) {
            super.systemMessageProvider(systemMessageProvider);

            return this;
        }

        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            return this;
        }

        public Builder messages(List<BaseMessage> messages) {
            super.messages(messages);
            return this;
        }

        public CodeEditorSpringAIAgent build() throws AGUIException {
            this.systemMessage("");

            return new CodeEditorSpringAIAgent(this);
        }
    }
}
