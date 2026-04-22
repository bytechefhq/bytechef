/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.Role;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ee.ai.copilot.util.Source;
import com.bytechef.ee.automation.workspacefile.ai.tool.AgUiToolContextWorkspaceContextProvider;
import com.bytechef.ee.automation.workspacefile.ai.tool.WorkspaceInvocationContext;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * Copilot agent that authors workspace files on behalf of the user. Relies on the workspace-file tool callbacks
 * (createWorkspaceFile, listWorkspaceFiles, getWorkspaceFileContent) being injected via
 * {@link com.bytechef.ee.ai.copilot.config.CopilotConfiguration}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class FilesSpringAIAgent extends SpringAIAgent {

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - When the user asks for a file (spec, runbook, CSV, JSON, markdown note, code file), produce the content and save it by calling createWorkspaceFile. Keep files concise and useful.
            - Before referring to existing files, call listWorkspaceFiles to discover what is available.
            - When editing an existing file, call getWorkspaceFileContent first, then call createWorkspaceFile with the updated content and a new or existing filename.
            """;

    protected FilesSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        WorkspaceInvocationContext context = buildInvocationContext(input);

        AgUiToolContextWorkspaceContextProvider.runWithContext(context, () -> super.run(input, subscriber));
    }

    WorkspaceInvocationContext buildInvocationContext(RunAgentInput input) {
        State state = input.state();

        Long workspaceId = state == null ? null : asLong(state.get("workspaceId"));
        Short sourceOrdinal = (short) Source.FILES.ordinal();
        String lastUserPrompt = lastUserPrompt(input.messages());

        return new WorkspaceInvocationContext(workspaceId, sourceOrdinal, lastUserPrompt);
    }

    private static Long asLong(Object value) {
        if (value == null) {
            return null;
        }

        if (value instanceof Long longValue) {
            return longValue;
        }

        if (value instanceof Number numberValue) {
            return numberValue.longValue();
        }

        if (value instanceof String stringValue && !stringValue.isBlank()) {
            try {
                return Long.parseLong(stringValue);
            } catch (NumberFormatException exception) {
                return null;
            }
        }

        return null;
    }

    private static String lastUserPrompt(List<BaseMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return null;
        }

        for (int i = messages.size() - 1; i >= 0; i--) {
            BaseMessage message = messages.get(i);

            if (message instanceof UserMessage userMessage && Role.user.equals(userMessage.getRole())) {
                return userMessage.getContent();
            }
        }

        return null;
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String resolvedMessage = Objects.nonNull(this.systemMessageProvider)
            ? this.systemMessageProvider.apply(this) : this.systemMessage;

        String message = "%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            resolvedMessage, ADDITIONAL_RULES, state, String.join("\n", contextStrings));

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

        public FilesSpringAIAgent build() throws AGUIException {

            return new FilesSpringAIAgent(this);
        }
    }
}
