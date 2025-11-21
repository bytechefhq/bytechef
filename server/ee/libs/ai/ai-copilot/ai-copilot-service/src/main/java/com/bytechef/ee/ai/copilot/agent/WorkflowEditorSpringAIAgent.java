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
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Objects;
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
public class WorkflowEditorSpringAIAgent extends SpringAIAgent {

    private static final String ADDITIONAL_RULES = """
        ## Additional Rules:

        - Don't send Visual Representations. as part of the response
        - If you are in CHAT mode don't try to modify the workflow definition
        """;

    private final WorkflowService workflowService;

    protected WorkflowEditorSpringAIAgent(final Builder builder, final WorkflowService workflowService)
        throws AGUIException {
        super(builder);

        this.workflowService = workflowService;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Workflow workflow = workflowService.getWorkflow((String) state.get("workflowId"));

        contexts.add(new Context("Current Workflow Definition", workflow.getDefinition()));

        List<String> contextStrings = contexts.stream()
            .map(Context::toString)
            .toList();

        String message = "%s%n%s%n%nState:%n%s%n%nContext:%n%s%n".formatted(
            Objects.nonNull(this.systemMessageProvider)
                ? this.systemMessageProvider.apply(this) : this.systemMessage,
            ADDITIONAL_RULES, state, String.join("%n", contextStrings));

        SystemMessage systemMessage = new SystemMessage();

        systemMessage.setId(String.valueOf(UUID.randomUUID()));
        systemMessage.setContent(message);

        return systemMessage;
    }

    public static class Builder extends SpringAIAgent.Builder {

        private WorkflowService workflowService;

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

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder workflowService(final WorkflowService workflowService) {
            this.workflowService = workflowService;

            return this;
        }

        public WorkflowEditorSpringAIAgent build() throws AGUIException {
            return new WorkflowEditorSpringAIAgent(this, workflowService);
        }
    }
}
