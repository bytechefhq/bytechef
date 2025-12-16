/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.agent;

import com.agui.core.agent.AgentSubscriber;
import com.agui.core.agent.AgentSubscriberParams;
import com.agui.core.agent.RunAgentInput;
import com.agui.core.context.Context;
import com.agui.core.event.BaseEvent;
import com.agui.core.exception.AGUIException;
import com.agui.core.message.AssistantMessage;
import com.agui.core.message.BaseMessage;
import com.agui.core.message.SystemMessage;
import com.agui.core.message.UserMessage;
import com.agui.core.state.State;
import com.agui.server.EventFactory;
import com.agui.server.LocalAgent;
import com.agui.spring.ai.SpringAIAgent;
import com.bytechef.ai.mcp.tool.automation.api.ChatProjectTools;
import com.bytechef.ai.mcp.tool.automation.api.ChatProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.api.ProjectTools;
import com.bytechef.ai.mcp.tool.automation.api.ProjectWorkflowTools;
import com.bytechef.ai.mcp.tool.automation.impl.ChatProjectToolsImpl;
import com.bytechef.ai.mcp.tool.automation.impl.ChatProjectWorkflowToolsImpl;
import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.UUID;
import java.util.function.Function;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.PromptChatMemoryAdvisor;
import org.springframework.ai.chat.client.advisor.api.Advisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.prompt.Prompt;
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
    private final ProjectTools projectTools;
    private final ProjectWorkflowTools projectWorkflowTools;
    private final ChatProjectTools chatProjectTools;
    private final ChatProjectWorkflowTools chatProjectWorkflowTools;

    private final ChatClient chatClient;
    private final ChatMemory chatMemory;
    private final List<Advisor> advisors;

    protected WorkflowEditorSpringAIAgent(final Builder builder, final WorkflowService workflowService)
        throws AGUIException {

        super(builder);

        this.tools = builder.tools;

        this.advisors = builder.advisors;
        this.chatMemory = builder.chatMemory;
        this.projectTools = (ProjectTools) tools.get(0);
        this.projectWorkflowTools = (ProjectWorkflowTools) tools.get(1);

        this.chatProjectTools = new ChatProjectToolsImpl(projectTools);
        this.chatProjectWorkflowTools = new ChatProjectWorkflowToolsImpl(projectWorkflowTools);

        this.workflowService = workflowService;
        this.chatClient = builder.chatClient;
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

    @Override
    protected void run(RunAgentInput input, AgentSubscriber subscriber) {
        try {
            State state = input.state();

            String mode = (String) state.get("mode");

            List<Object> selectedTools = new ArrayList<>(tools);

            if (mode.equals("CHAT")) {
                selectedTools.set(0, chatProjectTools);
                selectedTools.set(1, chatProjectWorkflowTools);
            } else if (mode.equals("BUILD")) {
                selectedTools.set(0, projectTools);
                selectedTools.set(1, projectWorkflowTools);
            }

            SystemMessage systemMessage = createSystemMessage(state, input.context());

            UserMessage userMessage = getLatestUserMessage(input.messages());
            String userContent = userMessage.getContent();

            AssistantMessage assistantMessage = new AssistantMessage();
            String messageId = String.valueOf(UUID.randomUUID());

            assistantMessage.setId(messageId);

            emitEvent(EventFactory.runStartedEvent(input.runId(), input.threadId()), subscriber);
            emitEvent(EventFactory.textMessageStartEvent(messageId, "assistant"), subscriber);

            ChatClient.ChatClientRequestSpec chatRequest = chatClient
                .prompt(Prompt.builder()
                    .content(userContent)
                    .build())
                .system(systemMessage.getContent())
                .tools(selectedTools.toArray(new Object[0]));

            if (advisors != null && !advisors.isEmpty()) {
                chatRequest = chatRequest.advisors(spec -> spec.param("chat_memory_conversation_id", input.threadId()));
            }

            if (Objects.nonNull(this.chatMemory)) {
                try {
                    chatRequest.advisors(
                        PromptChatMemoryAdvisor.builder(this.chatMemory)
                            .build());
                    chatRequest.advisors((a) -> a.param("chat_memory_conversation_id", input.threadId()));
                } catch (RuntimeException e) {
                    throw new AGUIException("Could not add chat memory", e);
                }
            }

            List<BaseEvent> deferredEvents = new ArrayList<>();

            chatRequest.stream()
                .chatResponse()
                .subscribe(
                    chatResponse -> handleChatResponse(chatResponse, assistantMessage, messageId, subscriber),
                    error -> handleError(error, subscriber),
                    () -> handleCompletion(input, assistantMessage, messageId, deferredEvents, subscriber));

        } catch (AGUIException e) {
            emitEvent(EventFactory.runErrorEvent(e.getMessage()), subscriber);
        }
    }

    private void handleChatResponse(
        ChatResponse chatResponse, AssistantMessage assistantMessage, String messageId, AgentSubscriber subscriber) {

        String content = chatResponse.getResult()
            .getOutput()
            .getText();

        if (content != null && !content.isEmpty()) {
            assistantMessage.setContent(
                (assistantMessage.getContent() != null ? assistantMessage.getContent() : "") + content);
            emitEvent(EventFactory.textMessageContentEvent(messageId, content), subscriber);
        }
    }

    private void handleCompletion(
        RunAgentInput input, AssistantMessage assistantMessage, String messageId, List<BaseEvent> deferredEvents,
        AgentSubscriber subscriber) {

        emitEvent(EventFactory.textMessageEndEvent(messageId), subscriber);
        deferredEvents.forEach(event -> emitEvent(event, subscriber));
        subscriber.onNewMessage(assistantMessage);
        emitEvent(EventFactory.runFinishedEvent(input.threadId(), input.runId()), subscriber);
        subscriber.onRunFinalized(new AgentSubscriberParams(input.messages(), this.state, this, input));
    }

    private void handleError(Throwable error, AgentSubscriber subscriber) {
        emitEvent(EventFactory.runErrorEvent(error.getMessage()), subscriber);
    }

    public static class Builder extends SpringAIAgent.Builder {

        private WorkflowService workflowService;
        private List<Object> tools;
        private ChatClient chatClient;
        private ChatModel chatModel;
        private ChatMemory chatMemory;
        private List<Advisor> advisors;

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder chatModel(ChatModel chatModel) {
            super.chatModel(chatModel);
            this.chatModel = chatModel;

            return this;
        }

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder advisors(List<Advisor> advisors) {
            super.advisors(advisors);

            this.advisors = advisors;

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

        @SuppressFBWarnings("EI_EXPOSE_REP2")
        public Builder chatMemory(ChatMemory chatMemory) {
            super.chatMemory(chatMemory);

            this.chatMemory = chatMemory;

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
            this.chatClient = ChatClient.builder(chatModel)
                .build();

            return new WorkflowEditorSpringAIAgent(this, workflowService);
        }
    }
}
