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
import com.bytechef.ai.mcp.tool.automation.ChatProjectTools;
import com.bytechef.ai.mcp.tool.automation.ChatProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.ProjectTools;
import com.bytechef.ai.mcp.tool.automation.ProjectWorkflowTools;
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

    private static final String ADDITIONAL_RULES =
        """
            ## Additional Rules

            - The assistant must not produce visual representations of any kind, including diagrams, charts, UI sketches, images, or pseudo-visuals.
            - When operating in CHAT mode, the assistant must not modify, propose modifications to, or generate new versions of the workflow definition. The assistant may only describe, clarify, or explain.
            - If a current selected node is available, the assistant must prioritize all answers using that node as the primary context.
            - If no node is selected, the assistant must use the broader workflow context as the primary basis for responses.
            """;

    private final WorkflowService workflowService;
    private final List<Object> tools;

    protected WorkflowEditorSpringAIAgent(final Builder builder, final WorkflowService workflowService)
        throws AGUIException {
        super(builder);
        this.tools = builder.tools;

        this.workflowService = workflowService;
    }

    public static Builder builder() {
        return new Builder();
    }

    @Override
    protected SystemMessage createSystemMessage(State state, List<Context> contexts) {
        Workflow workflow = workflowService.getWorkflow((String) state.get("workflowId"));
        String mode = (String) state.get("mode");

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

        if (mode.equals("CHAT")) {
            setChatTools();
        } else if (mode.equals("BUILD")) {
            setAllTools();
        }

        return systemMessage;
    }

    private void setChatTools() {
        tools.set(0, (ChatProjectTools) tools.get(0));
        tools.set(1, (ChatProjectWorkflowTools) tools.get(1));
    }

    private void setAllTools() {
        tools.set(0, (ProjectTools) tools.get(0));
        tools.set(1, (ProjectWorkflowTools) tools.get(1));
    }

    public static class Builder extends SpringAIAgent.Builder {

        private WorkflowService workflowService;
        private List<Object> tools;

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

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder tools(List<Object> tools) {
            super.tools(tools);
            this.tools = tools;

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
