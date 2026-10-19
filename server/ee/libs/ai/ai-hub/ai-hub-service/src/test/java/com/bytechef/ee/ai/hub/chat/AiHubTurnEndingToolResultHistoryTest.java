/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.spring.ai.TurnEndingToolCallingManager;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.memory.AiHubTurnEndingToolResultAdvisor;
import com.bytechef.test.extension.ObjectMapperSetupExtension;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
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
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.ai.model.tool.ToolCallingChatOptions;
import org.springframework.ai.model.tool.ToolCallingManager;
import org.springframework.ai.session.CreateSessionRequest;
import org.springframework.ai.session.InMemorySessionRepository;
import org.springframework.ai.session.SessionEvent;
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

    private static final String FIRST_USER_MESSAGE_ID = "1b9d6bcd-bbfd-4b2d-9b5d-ab8dfbbd4bed";
    private static final String SECOND_USER_MESSAGE_ID = "6ec0bd7f-11c0-43da-975e-2a8ad9ebae0b";
    private static final String EDITED_USER_MESSAGE_ID = "9f8b2c4e-5d6a-4b3c-8e1f-0a2b3c4d5e6f";

    private static final String SELECT_CONNECTION_RESULT =
        "{\"kind\":\"select-connection\",\"componentName\":\"slack\",\"awaitingAnswer\":true}";

    private static final String APPROVAL_REQUEST_RESULT =
        "{\"kind\":\"tool-approval-request\",\"approvalId\":3,\"toolName\":\"deleteWorkflow\","
            + "\"arguments\":{\"workflowId\":\"wf-1\"},\"awaitingApproval\":true}";

    private final AiHubToolApprovalService approvalService = mock(AiHubToolApprovalService.class);
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
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, sessionMemoryProvider,
            mock(InFlightAiHubRunRegistry.class));
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

        String pickerRowId = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)
            .get(1)
            .id();

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, pickerRowId, false);

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.getFirst()
            .toolEventsJson()).isNull();
    }

    @Test
    void testTruncatingAwayAnApprovalRequestSupersedesItsPendingApproval() {
        chatModel.reply(toolCallResponse("", "call-delete", "deleteWorkflow"));

        runTurn("Delete the workflow", FIRST_USER_MESSAGE_ID, tool("deleteWorkflow", APPROVAL_REQUEST_RESULT));

        AiHubToolApproval approval = pendingApproval("deleteWorkflow", "{}");

        assertThat(chatService.containsActiveToolCall(THREAD_ID, "deleteWorkflow", "{}")).isTrue();

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true);

        assertThat(chatService.containsActiveToolCall(THREAD_ID, "deleteWorkflow", "{}")).isFalse();
        assertThat(approval.getStatus()).isEqualTo(AiHubToolApproval.Status.SUPERSEDED);

        verify(approvalService).save(approval);
    }

    @Test
    void testTruncatingAfterAnApprovalRequestKeepsItsPendingApproval() {
        chatModel.reply(toolCallResponse("", "call-delete", "deleteWorkflow"));

        runTurn("Delete the workflow", FIRST_USER_MESSAGE_ID, tool("deleteWorkflow", APPROVAL_REQUEST_RESULT));

        chatModel.reply(textResponse("Waiting for your approval."));

        runTurn("Is it done?", SECOND_USER_MESSAGE_ID);

        AiHubToolApproval approval = pendingApproval("deleteWorkflow", "{}");

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, SECOND_USER_MESSAGE_ID, false);

        assertThat(approval.getStatus()).isEqualTo(AiHubToolApproval.Status.PENDING);

        verify(approvalService, never()).save(any());
    }

    @Test
    void testUserRowIsStoredUnderItsAgUiMessageId() {
        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).extracting(AiHubChatMessage::id)
            .first()
            .isEqualTo(FIRST_USER_MESSAGE_ID);
    }

    @Test
    void testReloadKeepsTheUserRowAndDoesNotStoreItAgain() {
        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true);

        chatModel.reply(textResponse("Renamed to Leads."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).extracting(AiHubChatMessage::content)
            .containsExactly("Now rename it", "Renamed to Leads.");
        assertThat(chatModel.prompts.getLast()
            .getInstructions()).extracting(Message::getText)
                .containsExactly("Now rename it");
    }

    @Test
    void testReloadAfterAFailedReRunKeepsTheUserRow() {
        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true);

        chatModel.fail(new IllegalStateException("The selected model is unavailable"));

        assertThatThrownBy(() -> runTurn("Now rename it", FIRST_USER_MESSAGE_ID))
            .hasStackTraceContaining("The selected model is unavailable");

        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).extracting(AiHubChatMessage::content)
            .containsExactly("Now rename it");

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true);

        chatModel.reply(textResponse("Renamed to Leads."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).extracting(AiHubChatMessage::content)
            .containsExactly("Now rename it", "Renamed to Leads.");
    }

    @Test
    void testEditDropsTheEditedRowAndStoresTheNewTextUnderItsNewId() {
        chatModel.reply(textResponse("Bound."));

        runTurn("Bind a Slack connection", FIRST_USER_MESSAGE_ID);

        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", SECOND_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, SECOND_USER_MESSAGE_ID, false);

        chatModel.reply(textResponse("Renamed to Leads."));

        runTurn("Now rename it to Leads", EDITED_USER_MESSAGE_ID);

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("Bind a Slack connection", "Bound.", "Now rename it to Leads", "Renamed to Leads.");
        assertThat(messages.get(2)
            .id()).isEqualTo(EDITED_USER_MESSAGE_ID);
    }

    @Test
    void testReloadOfALaterMessageShowsTheModelEachMessageOnce() {
        chatModel.reply(textResponse("Bound."));

        runTurn("Bind a Slack connection", FIRST_USER_MESSAGE_ID);

        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", SECOND_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, SECOND_USER_MESSAGE_ID, true);

        chatModel.reply(textResponse("Renamed to Leads."));

        runTurn("Now rename it", SECOND_USER_MESSAGE_ID);

        assertThat(chatModel.prompts.getLast()
            .getInstructions()).extracting(Message::getText)
                .containsExactly("Bind a Slack connection", "Bound.", "Now rename it");
        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).extracting(AiHubChatMessage::content)
            .containsExactly("Bind a Slack connection", "Bound.", "Now rename it", "Renamed to Leads.");
    }

    @Test
    void testARepeatUnderANewIdIsStoredAndShownToTheModel() {
        chatModel.reply(textResponse("Bound."));

        runTurn("Bind a Slack connection", FIRST_USER_MESSAGE_ID);

        appendEvent(SECOND_USER_MESSAGE_ID, new UserMessage("Now rename it"));

        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", EDITED_USER_MESSAGE_ID);

        assertThat(chatModel.prompts.getLast()
            .getInstructions()).extracting(Message::getText)
                .containsExactly("Bind a Slack connection", "Bound.", "Now rename it", "Now rename it");

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("Bind a Slack connection", "Bound.", "Now rename it", "Now rename it", "Renamed.");
        assertThat(messages.get(3)
            .id()).isEqualTo(EDITED_USER_MESSAGE_ID);
    }

    @Test
    void testAStaleTabReSendingATruncatedMessageStoresItAgainAheadOfItsReply() {
        chatModel.reply(textResponse("Bound."));

        runTurn("Bind a Slack connection", FIRST_USER_MESSAGE_ID);

        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", SECOND_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, SECOND_USER_MESSAGE_ID, false);

        chatModel.reply(textResponse("Renamed again."));

        runTurn("Now rename it", SECOND_USER_MESSAGE_ID);

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("Bind a Slack connection", "Bound.", "Now rename it", "Renamed again.");
        assertThat(messages.get(2)
            .id()).isNotEqualTo(SECOND_USER_MESSAGE_ID);
    }

    @Test
    void testTruncatingAtATruncatedMessageIsAConflict() {
        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, false);

        assertThatThrownBy(
            () -> chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testAKeptResultBehindTheCutNeverResurfacesWhenTheReRunReusesItsToolCallId() {
        chatModel.reply(toolCallResponse("Pick a Slack connection:", "call-select", "selectConnection"));

        runTurn("Bind a connection", FIRST_USER_MESSAGE_ID, tool("selectConnection", SELECT_CONNECTION_RESULT));

        chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, FIRST_USER_MESSAGE_ID, true);

        chatModel.reply(toolCallResponse("Pick a Gmail connection:", "call-select", "selectConnection"));

        runTurn(
            "Bind a connection", FIRST_USER_MESSAGE_ID,
            tool("selectConnection", SELECT_CONNECTION_RESULT.replace("slack", "gmail")));

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("Bind a connection", "Pick a Gmail connection:");
        assertThat(messages.get(1)
            .toolEventsJson()).contains("gmail")
                .doesNotContain("slack");
    }

    @Test
    void testTruncatingAtAMessageTheChatDoesNotHoldDropsNothing() {
        chatModel.reply(textResponse("Renamed."));

        runTurn("Now rename it", FIRST_USER_MESSAGE_ID);

        assertThat(chatService.truncateMessages(CHAT_ID, WORKSPACE_ID, USER_ID, SECOND_USER_MESSAGE_ID, true))
            .isEmpty();
        assertThat(chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID)).hasSize(2);
    }

    @Test
    void testKeptResultsPairWithTheirOwnCallsWhenToolCallIdsRepeat() {
        appendEvent(new UserMessage("First pick"));
        appendEvent(toolCallMessage("Pick one:", "repeated-id"));
        appendDisplayOnlyResult("repeated-id", "{\"kind\":\"first-result\"}");
        appendEvent(new UserMessage("Second pick"));
        appendEvent(toolCallMessage("Pick another:", "repeated-id"));
        appendDisplayOnlyResult("repeated-id", "{\"kind\":\"second-result\"}");

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(4);
        assertThat(messages.get(1)
            .toolEventsJson()).contains("first-result")
                .doesNotContain("second-result");
        assertThat(messages.get(3)
            .toolEventsJson()).contains("second-result")
                .doesNotContain("first-result");
    }

    @Test
    void testKeptResultWithABlankToolCallIdIsNotShown() {
        appendEvent(new UserMessage("Pick"));
        appendEvent(toolCallMessage("Pick one:", ""));
        appendDisplayOnlyResult("", "{\"kind\":\"blank-result\"}");

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(messages.get(1)
            .toolEventsJson()).doesNotContain("blank-result");
    }

    private AiHubToolApproval pendingApproval(String toolName, String arguments) {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setChatId(CHAT_ID);
        approval.setThreadId(THREAD_ID);
        approval.setToolName(toolName);
        approval.setArguments(arguments);
        approval.setStatus(AiHubToolApproval.Status.PENDING);

        when(approvalService.list(CHAT_ID)).thenReturn(List.of(approval));

        return approval;
    }

    private void appendEvent(Message message) {
        appendEvent(UUID.randomUUID()
            .toString(), message);
    }

    private void appendEvent(String eventId, Message message) {
        if (sessionMemory.sessionService()
            .findById(THREAD_ID) == null) {

            sessionMemory.sessionService()
                .create(CreateSessionRequest.builder()
                    .id(THREAD_ID)
                    .userId(AiHubSessionMemory.SESSION_USER_ID)
                    .build());
        }

        sessionMemory.sessionService()
            .appendEvent(SessionEvent.builder()
                .id(eventId)
                .sessionId(THREAD_ID)
                .message(message)
                .build());
    }

    private void appendDisplayOnlyResult(String toolCallId, String responseData) {
        sessionMemory.sessionService()
            .appendEvent(SessionEvent.builder()
                .sessionId(THREAD_ID)
                .message(ToolResponseMessage.builder()
                    .responses(List.of(new ToolResponseMessage.ToolResponse(
                        toolCallId, "selectConnection", responseData)))
                    .build())
                .metadata(AiHubTurnEndingToolResultAdvisor.DISPLAY_ONLY_METADATA_KEY, true)
                .archived(true)
                .build());
    }

    private static AssistantMessage toolCallMessage(String text, String toolCallId) {
        return AssistantMessage.builder()
            .content(text)
            .toolCalls(List.of(new ToolCall(toolCallId, "function", "selectConnection", "{}")))
            .build();
    }

    private void runTurn(String userText, String userMessageId) {
        runTurn(userText, userMessageId, tool("listConnections", "[]"));
    }

    private void runTurn(String userText, ToolCallback toolCallback) {
        runTurn(userText, UUID.randomUUID()
            .toString(), toolCallback);
    }

    private void runTurn(String userText, String userMessageId, ToolCallback toolCallback) {
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
                toolCallingAdvisor, sessionMemory.createResentUserMessageAdvisor())
            .advisors(advisorSpec -> advisorSpec.param(ChatMemory.CONVERSATION_ID, THREAD_ID)
                .param(AiHubSessionMemory.USER_MESSAGE_ID_PARAM, userMessageId))
            .stream()
            .chatResponse()
            .blockLast();
    }

    private static ChatResponse textResponse(String text) {
        return new ChatResponse(List.of(new Generation(new AssistantMessage(text))));
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
        private @Nullable RuntimeException nextFailure;
        private @Nullable ChatResponse nextResponse;

        void fail(RuntimeException failure) {
            nextFailure = failure;
        }

        void reply(ChatResponse chatResponse) {
            nextFailure = null;
            nextResponse = chatResponse;
        }

        @Override
        public ChatResponse call(Prompt prompt) {
            prompts.add(prompt);

            if (nextFailure != null) {
                throw nextFailure;
            }

            return Objects.requireNonNull(nextResponse);
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
