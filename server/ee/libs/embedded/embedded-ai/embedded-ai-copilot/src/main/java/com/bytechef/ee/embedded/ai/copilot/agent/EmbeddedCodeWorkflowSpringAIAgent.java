/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.copilot.agent;

import com.agui.core.agent.RunAgentInput;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.BaseMessage;
import com.agui.core.state.State;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.copilot.agent.CopilotSpringAIAgent;
import com.bytechef.ai.copilot.util.CopilotToolContextUtils;
import java.util.List;
import java.util.Map;
import java.util.function.Function;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.tool.ToolCallback;

/**
 * The embedded code-workflow Copilot agent. Extends the base {@link SpringAIAgent} (no automation-specific behavior is
 * needed) but overrides {@link #toolContext(RunAgentInput)} to carry the run's authenticated-user / tenant /
 * skip-automation-authorization signals from the AG-UI {@link State} into the Spring AI {@code ToolContext}, exactly
 * the way {@code WorkflowEditorSpringAIAgent} does.
 *
 * <p>
 * This is required because the code-workflow tools call {@code IntegrationCodeWorkflowFacade} methods, all of which are
 * {@code @PreAuthorize}-gated. Spring AI executes tool calls on a {@code boundedElastic} worker thread whose
 * {@code SecurityContextHolder} is empty; without this context (re-armed on the worker by
 * {@code RehydrateContextToolCallback}), every tool call would fail with {@code AccessDeniedException}.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class EmbeddedCodeWorkflowSpringAIAgent extends CopilotSpringAIAgent {

    protected EmbeddedCodeWorkflowSpringAIAgent(final Builder builder) throws AGUIException {
        super(builder, null);
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected Map<String, Object> toolContext(RunAgentInput input) {
        return CopilotToolContextUtils.toToolContext(input.state());
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

        public EmbeddedCodeWorkflowSpringAIAgent build() throws AGUIException {

            return new EmbeddedCodeWorkflowSpringAIAgent(this);
        }
    }
}
