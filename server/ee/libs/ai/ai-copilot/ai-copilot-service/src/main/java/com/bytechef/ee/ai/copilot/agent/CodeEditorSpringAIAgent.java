/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ee.ai.copilot.util.CopilotToolContextUtils;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public class CodeEditorSpringAIAgent extends CopilotSpringAIAgent {

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - If state.workflowExecutionError is not empty, there is an error and you must instruct the user on how to fix it. The user can't modify the code, only the input parameters. If it's impossible to fix the error, instruct the user to raise an issue on our GitHub https://github.com/bytechefhq/bytechef/issues.
            """;

    protected CodeEditorSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder, builder.overrideChatClientResolver);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return CopilotToolContextUtils.toToolContext(input.state());
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Map<?, ?> parameters = (Map<?, ?>) state.get("parameters");

        String promptLanguage = switch ((String) parameters.get("language")) {
            case "javascript" -> "The language you have to assist with is Javascript.";
            case "python" -> "The language you have to assist with is Python.";
            case "ruby" -> "The language you have to assist with is Ruby.";
            case "r" -> "The language you have to assist with is R.";
            case "java" -> "The language you have to assist with is Java.";
            default -> throw new IllegalStateException("Unexpected value: " + parameters.get("language"));
        };

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, promptLanguage, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private @Nullable OverrideChatClientResolver overrideChatClientResolver;

        public Builder overrideChatClientResolver(@Nullable OverrideChatClientResolver overrideChatClientResolver) {
            this.overrideChatClientResolver = overrideChatClientResolver;

            return this;
        }

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

        public Builder tool(Object tool) {
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

            return new CodeEditorSpringAIAgent(this);
        }
    }
}
