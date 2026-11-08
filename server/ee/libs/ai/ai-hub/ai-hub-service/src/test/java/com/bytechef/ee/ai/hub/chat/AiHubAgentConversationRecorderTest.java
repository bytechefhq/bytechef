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
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.automation.configuration.domain.Project;
import com.bytechef.automation.configuration.service.ProjectService;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
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
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.transaction.PlatformTransactionManager;

/**
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
    @SuppressWarnings("unchecked")
    void setUp() {
        lenient().when(aiHubSessionMemoryProvider.getIfAvailable())
            .thenReturn(aiHubSessionMemory);
        lenient().when(aiHubSessionMemory.sessionService())
            .thenReturn(sessionService);
        lenient().when(aiHubSessionMemory.sessionRepository())
            .thenReturn(sessionRepository);
        lenient().when(sessionService.getEvents(anyString(), eq(EventFilter.all())))
            .thenReturn(List.of());

        lenient().when(chatRepository.updateLastPreviewUnlessDeleted(anyLong(), anyString(), anyInt(), any()))
            .thenReturn(1);
        lenient().when(chatRepository.updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any()))
            .thenReturn(1);

        chatService = new AiHubChatServiceImpl(
            chatRepository, new OwnerOnlyAccessPolicy(), mock(AiHubToolApprovalService.class),
            aiHubSessionMemoryProvider, mock(InFlightAiHubRunRegistry.class), mock(PlatformTransactionManager.class),
            mock(ObjectProvider.class), mock(ApplicationEventPublisher.class));

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
        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());

        assertThat(composerChat.getAiAgentId()).isNull();
        assertThat(composerChat.getMessageCount()).isZero();
    }

    @Test
    void testTranscriptReadsBackThroughTheExistingGetEventsPath() {
        givenWorkflowInWorkspace(WORKSPACE_ID);
        givenInsertStoresRow();

        SessionEvent userEvent = sessionEvent(MessageType.USER, "what is the status?");
        SessionEvent assistantEvent = sessionEvent(MessageType.ASSISTANT, "all clear");

        when(sessionService.getEvents(CONVERSATION_ID, EventFilter.all()))
            .thenReturn(List.of(userEvent, assistantEvent));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        List<AiHubChatMessage> messages = chatService.loadMessages(CHAT_ID, WORKSPACE_ID, CREATOR_USER_ID);

        assertThat(messages).extracting(AiHubChatMessage::content)
            .containsExactly("what is the status?", "all clear");

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        assertThat(chat.getMessageCount()).isEqualTo(2);
        assertThat(chat.getLastPreview()).isEqualTo("all clear");
    }

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

        when(sessionService.getEvents(CONVERSATION_ID, EventFilter.all())).thenReturn(sessionEvents);

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        assertThat(chat.getLastPreview()).isEqualTo("message " + (eventCount - 1));
        assertThat(chat.getMessageCount()).isEqualTo(eventCount);
    }

    @Test
    void testPreviewTruncationDoesNotSplitASurrogatePair() {
        givenWorkflowInWorkspace(WORKSPACE_ID);
        givenInsertStoresRow();

        String lastMessageContent = "a".repeat(254) + "😀" + "b".repeat(100);

        List<SessionEvent> sessionEvents = List.of(sessionEvent(MessageType.ASSISTANT, lastMessageContent));

        when(sessionService.getEvents(CONVERSATION_ID, EventFilter.all())).thenReturn(sessionEvents);

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        AiHubChat chat = chatRepository.findByThreadId(CONVERSATION_ID)
            .orElseThrow();

        String lastPreview = chat.getLastPreview();

        assertThat(lastPreview).isEqualTo("a".repeat(254));
        assertThat(Character.isHighSurrogate(lastPreview.charAt(lastPreview.length() - 1))).isFalse();
    }

    @Test
    void testComposerChatCarryingAnAgentIdIsStillNotAdopted() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat composerChat = chat(AiHubChatKind.AGENT_CHAT, CREATOR_USER_ID, WORKSPACE_ID, AI_AGENT_ID);

        composerChat.setWorkflowExecutionId("tenant:execution-1");

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(composerChat));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).insertAgentChatIfAbsent(
            anyLong(), anyString(), any(), anyInt(), anyInt(), anyInt(), anyLong(), anyLong(), any());
        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
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

        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void testDeletedChatIsNotResurrected() {
        givenWorkflowInWorkspace(WORKSPACE_ID);

        AiHubChat deletedChat = chat(AiHubChatKind.AGENT_CHAT, CREATOR_USER_ID, WORKSPACE_ID, AI_AGENT_ID);

        deletedChat.setStatus(AiHubChatStatus.DELETED);

        when(chatRepository.findByThreadId(CONVERSATION_ID)).thenReturn(Optional.of(deletedChat));

        recorder.recordTurn(agentConversation(WORKSPACE_ID, AI_AGENT_ID, CREATOR_USER_ID));

        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
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
                        .name("Support Bot")
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
