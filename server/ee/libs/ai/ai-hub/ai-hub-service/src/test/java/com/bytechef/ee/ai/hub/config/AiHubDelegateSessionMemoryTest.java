/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.config;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ai.agent.tool.AgentType;
import com.bytechef.ai.copilot.tool.CopilotAgentType;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolDefinition;
import com.bytechef.ai.copilot.tool.catalog.IntelligentToolVariant;
import com.bytechef.ai.copilot.tool.context.AgentToolInvocationContext;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalPolicy;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.BiFunction;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubDelegateSessionMemoryTest {

    private static final String THREAD_ID = "thread-1";
    private static final long WORKSPACE_ID = 7L;

    private final PromptCapturingChatModel chatModel = new PromptCapturingChatModel();

    @Test
    void testHubDelegateReplaysItsEarlierCallsOnTheSameThread() throws Exception {
        AiHubAgentFixtures fixtures = fixtures();

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            captureChatClientDecorator(fixtures);

        ChatClient delegateChatClient = decorate(chatClientDecorator, CopilotAgentType.BUILD_WORKFLOW);

        callWith(delegateChatClient, "add a Slack step", THREAD_ID);
        callWith(delegateChatClient, "make the message shorter", THREAD_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly(
            "add a Slack step", "reply to: add a Slack step", "make the message shorter");

        List<SessionEvent> events = fixtures.sessionMemory.sessionService()
            .getEvents(
                SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, CopilotAgentType.BUILD_WORKFLOW.key()),
                EventFilter.all());

        assertThat(events).hasSize(4);
        assertThat(fixtures.sessionMemory.sessionService()
            .getEvents(THREAD_ID, EventFilter.all())).isEmpty();
    }

    @Test
    void testHubDelegatesOfDifferentAgentTypesKeepSeparateMemories() throws Exception {
        AiHubAgentFixtures fixtures = fixtures();

        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator =
            captureChatClientDecorator(fixtures);

        ChatClient buildWorkflowChatClient = decorate(chatClientDecorator, CopilotAgentType.BUILD_WORKFLOW);
        ChatClient importWorkflowChatClient = decorate(chatClientDecorator, CopilotAgentType.IMPORT_WORKFLOW);

        callWith(buildWorkflowChatClient, "add a Slack step", THREAD_ID);
        callWith(importWorkflowChatClient, "import this n8n workflow", THREAD_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly("import this n8n workflow");

        callWith(buildWorkflowChatClient, "make the message shorter", THREAD_ID);

        assertThat(chatModel.lastConversationTexts()).containsExactly(
            "add a Slack step", "reply to: add a Slack step", "make the message shorter");
    }

    private static AiHubAgentFixtures fixtures() {
        AiHubAgentFixtures fixtures = AiHubAgentFixtures.withBuildWorkflow();

        when(fixtures.policy.decide(WORKSPACE_ID))
            .thenReturn(new AiHubToolApprovalPolicy.Decision(Set.of(), Set.of()));

        return fixtures;
    }

    @SuppressWarnings({
        "rawtypes", "unchecked"
    })
    private static BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> captureChatClientDecorator(
        AiHubAgentFixtures fixtures) throws Exception {

        fixtures.buildAgent();

        ArgumentCaptor<BiFunction> chatClientDecoratorCaptor = ArgumentCaptor.forClass(BiFunction.class);

        verify(fixtures.intelligentToolCatalog)
            .getByNames(anySet(), eq(IntelligentToolVariant.BUILD), chatClientDecoratorCaptor.capture(), any());

        return chatClientDecoratorCaptor.getValue();
    }

    private ChatClient decorate(
        BiFunction<ChatClient, IntelligentToolDefinition, ChatClient> chatClientDecorator, AgentType agentType) {

        IntelligentToolDefinition definition = mock(IntelligentToolDefinition.class);

        when(definition.agentType()).thenReturn(agentType);

        return chatClientDecorator.apply(
            ChatClient.builder(chatModel)
                .build(),
            definition);
    }

    private static void callWith(ChatClient chatClient, String request, String threadId) {
        Map<String, Object> toolContext = AgentToolInvocationContext.builder()
            .workspaceId(WORKSPACE_ID)
            .conversationId(threadId)
            .build()
            .toToolContext();

        chatClient.prompt(request)
            .toolContext(toolContext)
            .call()
            .content();
    }

    private static final class PromptCapturingChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            UserMessage userMessage = prompt.getUserMessage();

            return new ChatResponse(
                List.of(new Generation(new AssistantMessage("reply to: " + userMessage.getText()))));
        }

        List<String> lastConversationTexts() {
            Prompt prompt = prompts.getLast();

            return prompt.getInstructions()
                .stream()
                .filter(message -> message.getMessageType() != MessageType.SYSTEM)
                .map(Message::getText)
                .toList();
        }
    }
}
