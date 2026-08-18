/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.platform.ai.conversation.AgentConversationRecorder.AgentConversation;
import com.bytechef.platform.configuration.domain.Environment;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit tests for {@link AiHubAgentConversationRecorder} — the EE side of AI Hub visibility for agent channel runs
 * (ticket 732, {@code 2026-08-17-agent-run-hub-visibility}).
 *
 * <p>
 * The collaborating {@link AiHubChatServiceImpl} is the REAL implementation over the same mocked repository and session
 * store, not a mock, so the transcript readback assertions exercise the Hub's existing {@code getEvents(threadId)} path
 * rather than a stub of it.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubAgentConversationRecorderTest {

    private static final long AI_AGENT_ID = 20L;
    private static final long CHAT_ID = 500L;
    private static final String CONVERSATION_ID = "slack-C0123";
    private static final long CREATOR_USER_ID = 30L;
    private static final int ENVIRONMENT_ID = 2;
    private static final long OTHER_AI_AGENT_ID = 21L;
    private static final long OTHER_USER_ID = 31L;
    private static final long WORKSPACE_ID = 10L;
    private static final String WORKFLOW_ID = "workflow-1";

    @Mock
    private AiHubChatRepository chatRepository;

    @Mock
    private ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider;

    @Mock
    private AiHubSessionMemory aiHubSessionMemory;

    @Mock
    private ProjectService projectService;

    @Mock
    private SessionRepository sessionRepository;

    @Mock
    private SessionService sessionService;

    private AiHubChatServiceImpl chatService;
    private AiHubAgentConversationRecorder recorder;

    @BeforeEach
    void setUp() {
        lenient().when(aiHubSessionMemoryProvider.getIfAvailable())
            .thenReturn(aiHubSessionMemory);
        lenient().when(aiHubSessionMemory.sessionService())
            .thenReturn(sessionService);
        lenient().when(aiHubSessionMemory.sessionRepository())
            .thenReturn(sessionRepository);
        lenient().when(sessionService.getEvents(anyString()))
            .thenReturn(List.of());
        lenient().when(chatRepository.save(any(AiHubChat.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        chatService = new AiHubChatServiceImpl(
            chatRepository, mock(com.bytechef.atlas.execution.facade.JobFacade.class),
            mock(com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry.class),
            mock(com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry.class), null, aiHubSessionMemoryProvider,
            null);

        recorder = new AiHubAgentConversationRecorder(
            chatRepository, chatService, projectService, Clock.fixed(Instant.EPOCH, ZoneOffset.UTC));
    }

    @Test
    void testFirstTurnCreatesRowAndSecondTurnReusesIt() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AtomicReference<AiHubChat> storedChat = givenInsertStoresRow();

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        AiHubChat created = storedChat.get();

        assertThat(created).isNotNull();
        assertThat(created.getThreadId()).isEqualTo(CONVERSATION_ID);
        assertThat(created.getKind()).isEqualTo(AiHubChatKind.AGENT_CHAT);
        assertThat(created.getAiAgentId()).isEqualTo(AI_AGENT_ID);
        assertThat(created.getUserId()).isEqualTo(CREATOR_USER_ID);
        assertThat(created.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(created.getEnvironment()).isEqualTo(Environment.values()[ENVIRONMENT_ID]);

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        // The second turn found the row and did not attempt another insert — the opposite of
        // createAgentChatAiHubChat, which is deliberately always-new.
        verify(chatRepository, times(1)).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
    }

    @Test
    void testTwoAgentsOnOneConversationShareOneRowAttributedToTheFirst() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AtomicReference<AiHubChat> storedChat = givenInsertStoresRow();

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));
        recorder.recordTurn(agentConversation(WORKSPACE_ID, OTHER_AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, times(1)).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());

        AiHubChat chat = storedChat.get();

        // One conversationId means one session and one transcript, so one row — attributed to whichever agent
        // spoke first.
        assertThat(chat.getAiAgentId()).isEqualTo(AI_AGENT_ID);
    }

    @Test
    void testComposerCreatedAgentChatIsNeverMatchedOrMutated() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat composerChat = chat(AiHubChatKind.AGENT_CHAT, CREATOR_USER_ID, WORKSPACE_ID, null);

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(composerChat));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
        verify(chatRepository, never()).save(any(AiHubChat.class));

        assertThat(composerChat.getAiAgentId()).isNull();
        assertThat(composerChat.getMessageCount()).isZero();
    }

    @Test
    void testTranscriptReadsBackThroughTheExistingGetEventsPath() {
        givenWorkflowInWorkspace(WORKSPACE_ID);
        givenInsertStoresRow();

        // Built before the stubbing call: creating a mock inside an unfinished when(...) confuses Mockito.
        SessionEvent userEvent = sessionEvent(MessageType.USER, "what is the status?");
        SessionEvent assistantEvent = sessionEvent(MessageType.ASSISTANT, "all clear");

        when(sessionService.getEvents(CONVERSATION_ID)).thenReturn(List.of(userEvent, assistantEvent));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, CREATOR_USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("what is the status?", "all clear");

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        assertThat(chat.getMessageCount()).isEqualTo(2);
        assertThat(chat.getLastPreview()).isEqualTo("all clear");
    }

    /**
     * A long-lived channel thread must not freeze its sidebar preview. {@code loadMessages} caps at
     * {@code MESSAGE_LIMIT = 500} visible rows, so deriving the preview from it would pin the row to message #500
     * forever while {@code updated_at} kept re-sorting it to the top as freshly active.
     */
    @Test
    void testPreviewAndCountFollowTheTranscriptPastTheLoadMessagesCap() {
        givenWorkflowInWorkspace(WORKSPACE_ID);
        givenInsertStoresRow();

        int eventCount = 620;
        List<SessionEvent> sessionEvents = new ArrayList<>();

        for (int i = 0; i < eventCount; i++) {
            sessionEvents.add(
                sessionEvent(i % 2 == 0 ? MessageType.USER : MessageType.ASSISTANT, "message " + i));
        }

        when(sessionService.getEvents(CONVERSATION_ID)).thenReturn(sessionEvents);

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        assertThat(chat.getLastPreview()).isEqualTo("message " + (eventCount - 1));
        assertThat(chat.getMessageCount()).isEqualTo(eventCount);
    }

    /**
     * Channel text is emoji-heavy, and an emoji is a surrogate pair — two {@code char}s. Truncating the preview at a
     * fixed {@code char} offset can land between the halves and leave a lone high surrogate, which renders as a
     * replacement glyph in the sidebar. The preview must therefore end on a code point boundary.
     */
    @Test
    void testPreviewTruncationDoesNotSplitASurrogatePair() {
        givenWorkflowInWorkspace(WORKSPACE_ID);
        givenInsertStoresRow();

        // Places the emoji's high surrogate at index 254 and its low surrogate at index 255 — exactly the boundary a
        // plain substring(0, 255) would cut through.
        String lastMessageContent = "a".repeat(254) + "😀" + "b".repeat(100);

        // Built before the stubbing call: sessionEvent() itself stubs a mock, which Mockito rejects mid-when().
        List<SessionEvent> sessionEvents = List.of(sessionEvent(MessageType.ASSISTANT, lastMessageContent));

        when(sessionService.getEvents(CONVERSATION_ID)).thenReturn(sessionEvents);

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        String lastPreview = chat.getLastPreview();

        assertThat(lastPreview).isEqualTo("a".repeat(254));
        assertThat(Character.isHighSurrogate(lastPreview.charAt(lastPreview.length() - 1))).isFalse();
    }

    /**
     * The composer guard must not rest on a single unenforced invariant. {@code createWebhookBridgedChat} happens to
     * leave {@code ai_agent_id} null today, but a later task labelling composer rows by agent could set it; the row is
     * still a composer row because it binds a workflow execution, and must still be left alone.
     */
    @Test
    void testComposerChatCarryingAnAgentIdIsStillNotAdopted() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat composerChat = chat(AiHubChatKind.AGENT_CHAT, CREATOR_USER_ID, WORKSPACE_ID, AI_AGENT_ID);

        composerChat.setWorkflowExecutionId("tenant:execution-1");

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(composerChat));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
        verify(chatRepository, never()).save(any(AiHubChat.class));
    }

    @Test
    void testForgedWorkspaceStampCreatesNoRow() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        recorder.recordTurn(agentConversation(WORKSPACE_ID + 1, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
        verify(chatRepository, never()).findByThreadId(anyString());
    }

    @Test
    void testUnresolvableWorkflowCreatesNoRow() {
        when(projectService.fetchWorkflowProject(WORKFLOW_ID)).thenReturn(Optional.empty());

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).findByThreadId(anyString());
    }

    @Test
    void testMissingWorkflowIdCreatesNoRow() {
        recorder.recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, null,
                (long) ENVIRONMENT_ID));

        verify(chatRepository, never()).findByThreadId(anyString());
    }

    @Test
    void testMissingEnvironmentCreatesNoRow() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.empty());

        recorder.recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, CONVERSATION_ID, null, null, WORKFLOW_ID, null));

        verify(chatRepository, never()).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
    }

    @Test
    void testBlankConversationIdCreatesNoRow() {
        recorder.recordTurn(
            new AgentConversation(
                WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID, "", null, null, WORKFLOW_ID, (long) ENVIRONMENT_ID));

        verify(chatRepository, never()).findByThreadId(anyString());
    }

    @Test
    void testCrossOwnerCollisionIsSkippedAndNotThrown() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat otherUsersChat = chat(AiHubChatKind.AGENT_CHAT, OTHER_USER_ID, WORKSPACE_ID, OTHER_AI_AGENT_ID);

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(otherUsersChat));

        assertThatCode(() -> recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID)))
            .doesNotThrowAnyException();

        verify(chatRepository, never()).save(any(AiHubChat.class));
    }

    @Test
    void testDeletedChatIsNotResurrected() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat deletedChat = chat(AiHubChatKind.AGENT_CHAT, CREATOR_USER_ID, WORKSPACE_ID, AI_AGENT_ID);

        deletedChat.setStatus(AiHubChatStatus.DELETED);

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(deletedChat));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).save(any(AiHubChat.class));
    }

    private static AgentConversation agentConversation(long workspaceId, long aiAgentId, long creatorUserId) {
        return new AgentConversation(
            workspaceId, aiAgentId, creatorUserId, CONVERSATION_ID, null, null, WORKFLOW_ID, (long) ENVIRONMENT_ID);
    }

    private static AiHubChat chat(
        AiHubChatKind kind, long userId, long workspaceId, Long aiAgentId) {

        AiHubChat chat = new AiHubChat(userId);

        chat.setId(CHAT_ID);
        chat.setThreadId(CONVERSATION_ID);
        chat.setKind(kind);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setEnvironment(Environment.values()[ENVIRONMENT_ID]);
        chat.setWorkspaceId(workspaceId);
        chat.setAiAgentId(aiAgentId);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());

        return chat;
    }

    /**
     * Makes the repository behave like the real one for the find-or-create path: {@code findByThreadId} is empty until
     * {@code insertAgentChatIfAbsent} runs, and returns the inserted row afterwards.
     */
    private AtomicReference<AiHubChat> givenInsertStoresRow() {
        AtomicReference<AiHubChat> storedChat = new AtomicReference<>();

        when(chatRepository.findByThreadId(CONVERSATION_ID))
            .thenAnswer(invocation -> Optional.ofNullable(storedChat.get()));
        when(chatRepository.findById(CHAT_ID))
            .thenAnswer(invocation -> Optional.ofNullable(storedChat.get()));
        when(
            chatRepository.insertAgentChatIfAbsent(
                anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any()))
                    .thenAnswer(invocation -> {
                        AiHubChat chat = chat(
                            AiHubChatKind.values()[invocation.getArgument(5, Integer.class)],
                            invocation.getArgument(0, Long.class), invocation.getArgument(6, Long.class),
                            invocation.getArgument(7, Long.class));

                        chat.setThreadId(invocation.getArgument(1, String.class));
                        chat.setEnvironment(Environment.values()[invocation.getArgument(4, Integer.class)]);

                        storedChat.set(chat);

                        return 1;
                    });

        return storedChat;
    }

    private void givenWorkflowInWorkspace(long workspaceId) {
        when(projectService.fetchWorkflowProject(WORKFLOW_ID))
            .thenReturn(
                Optional.of(
                    Project.builder()
                        .id(1L)
                        .name("__AI_AGENT__test")
                        .workspaceId(workspaceId)
                        .build()));
    }

    private static SessionEvent sessionEvent(MessageType messageType, String text) {
        SessionEvent sessionEvent = mock(SessionEvent.class);

        Message message = messageType == MessageType.USER ? new UserMessage(text) : new AssistantMessage(text);

        lenient().when(sessionEvent.getMessageType())
            .thenReturn(messageType);
        lenient().when(sessionEvent.getMessage())
            .thenReturn(message);
        lenient().when(sessionEvent.getTimestamp())
            .thenReturn(Instant.EPOCH);

        return sessionEvent;
    }
}
