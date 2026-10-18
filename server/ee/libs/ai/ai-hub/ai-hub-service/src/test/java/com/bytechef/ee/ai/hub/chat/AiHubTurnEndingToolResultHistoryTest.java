/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.client.advisor.ToolCallingAdvisor;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.beans.factory.ObjectProvider;
import reactor.core.publisher.Flux;

/**
 * Runs hub turns through the real session memory and tool-calling advisors over an in-memory session store, so the
 * stored history and the prompt the model sees are both what production produces.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(ObjectMapperSetupExtension.class)
class AiHubTurnEndingToolResultHistoryTest {

    private static final long CHAT_ID = 1L;
    private static final String THREAD_ID = "thread-turn-ending";
    private static final long USER_ID = 10L;
    private static final long WORKSPACE_ID = 1L;

    private static final String SELECT_CONNECTION_RESULT =
        "{\"kind\":\"select-connection\",\"componentName\":\"slack\",\"awaitingAnswer\":true}";

    private static final String APPROVAL_REQUEST_RESULT =
        "{\"kind\":\"tool-approval-request\",\"approvalId\":3,\"toolName\":\"deleteWorkflow\","
            + "\"arguments\":{\"workflowId\":\"wf-1\"},\"awaitingApproval\":true}";

    private final ScriptedChatModel chatModel = new ScriptedChatModel();

    private AiHubChatServiceImpl chatService;
    private AiHubSessionMemory sessionMemory;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        sessionMemory = new AiHubSessionMemory(InMemorySessionRepository.builder()
            .build(), null);

        AiHubChatRepository chatRepository = mock(AiHubChatRepository.class);

        AiHubChat chat = new AiHubChat();

        chat.setId(CHAT_ID);
        chat.setUserId(USER_ID);
        chat.setThreadId(THREAD_ID);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        chat.setWorkspaceId(WORKSPACE_ID);

        when(chatRepository.findById(CHAT_ID)).thenReturn(Optional.of(chat));

        ObjectProvider<AiHubSessionMemory> sessionMemoryProvider = mock(ObjectProvider.class);

        when(sessionMemoryProvider.getIfAvailable()).thenReturn(sessionMemory);

        chatService = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), mock(AiHubToolApprovalService.class),
            sessionMemoryProvider, mock(InFlightAiHubRunRegistry.class));
    }

    @Test
    void testTurnEndingOnSelectConnectionKeepsItsResultInTheChatHistory() {
        chatModel.reply(toolCallResponse("Pick a Slack connection:", "call-select", "selectConnection"));

        runTurn("Bind a Slack connection", tool("selectConnection", SELECT_CONNECTION_RESULT));

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(2);
        assertThat(messages.get(1)
            .toolEventsJson()).contains("\"kind\":\"result\"")
                .contains("call-select")
                .contains("select-connection");
    }

    @Test
    void testTurnEndingOnAnApprovalRequestKeepsItsResultInTheChatHistory() {
        chatModel.reply(toolCallResponse("", "call-delete", "deleteWorkflow"));

        runTurn("Delete the workflow", tool("deleteWorkflow", APPROVAL_REQUEST_RESULT));

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()
            .toolEventsJson()).contains("\"kind\":\"result\"")
                .contains("call-delete")
                .contains("tool-approval-request");
    }

    @Test
    void testKeptResultStaysOutOfTheNextTurnsPrompt() {
        chatModel.reply(toolCallResponse("Pick a Slack connection:", "call-select", "selectConnection"));

        runTurn("Bind a Slack connection", tool("selectConnection", SELECT_CONNECTION_RESULT));

        chatModel.reply(new ChatResponse(List.of(new Generation(new AssistantMessage("Bound.")))));

        runTurn("User picked: Slack Engineering (ID: 1051)", tool("selectConnection", SELECT_CONNECTION_RESULT));

        List<Message> secondPrompt = chatModel.prompts.getLast()
            .getInstructions();

        assertThat(secondPrompt).noneMatch(ToolResponseMessage.class::isInstance);
        assertThat(secondPrompt).noneMatch(message -> message instanceof AssistantMessage assistantMessage
            && assistantMessage.hasToolCalls());
        assertThat(secondPrompt).extracting(Message::getText)
            .containsExactly("Bind a Slack connection", "User picked: Slack Engineering (ID: 1051)");
    }

    @Test
    void testKeptResultIsDroppedWithTheTruncatedTurn() {
        chatModel.reply(toolCallResponse("Pick a Slack connection:", "call-select", "selectConnection"));

        runTurn("Bind a Slack connection", tool("selectConnection", SELECT_CONNECTION_RESULT));

        chatService.truncateMessagesFrom(CHAT_ID, WORKSPACE_ID, USER_ID, 1);

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()
            .toolEventsJson()).isNull();
    }

    private void runTurn(String userText, ToolCallback toolCallback) {
        ToolCallingAdvisor toolCallingAdvisor = ToolCallingAdvisor.builder()
            .toolCallingManager(new TurnEndingToolCallingManager(
                ToolCallingManager.builder()
                    .build(),
                toolResponse -> toolResponse.responseData()
                    .contains("\"awaitingAnswer\":true")
                    || toolResponse.responseData()
                        .contains("\"awaitingApproval\":true")))
            .conversationHistoryEnabled(false)
            .build();

        ChatClient.builder(chatModel)
            .build()
            .prompt()
            .user(userText)
            .toolCallbacks(toolCallback)
            .advisors(
                sessionMemory.createSessionMemoryAdvisor(), sessionMemory.createTurnEndingToolResultAdvisor(),
                toolCallingAdvisor)
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, THREAD_ID))
            .stream()
            .chatResponse()
            .blockLast();
    }

    private static ChatResponse toolCallResponse(String text, String toolCallId, String toolName) {
        AssistantMessage assistantMessage = AssistantMessage.builder()
            .content(text)
            .toolCalls(List.of(new ToolCall(toolCallId, "function", toolName, "{}")))
            .build();

        return new ChatResponse(List.of(new Generation(assistantMessage)));
    }

    private static ToolCallback tool(String name, String result) {
        return new ToolCallback() {

            @Override
            public ToolDefinition getToolDefinition() {
                return ToolDefinition.builder()
                    .name(name)
                    .description(name)
                    .inputSchema("{\"type\":\"object\",\"properties\":{}}")
                    .build();
            }

            @Override
            public String call(String toolInput) {
                return result;
            }
        };
    }

    private static final class ScriptedChatModel implements ChatModel {

        private final List<Prompt> prompts = new ArrayList<>();
        private ChatResponse nextResponse;

        void reply(ChatResponse chatResponse) {
            nextResponse = chatResponse;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            return nextResponse;
        }

        @Override
        public ChatOptions getOptions() {
            return ToolCallingChatOptions.builder()
                .build();
        }

        @Override
        public Flux<ChatResponse> stream(Prompt prompt) {
            return Flux.just(call(prompt));
        }
    }
}
