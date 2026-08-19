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
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.beans.factory.ObjectProvider;

/**
 * Unit tests for {@link AiHubChatServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubChatServiceTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long USER_ID = 10L;
    private static final long OTHER_USER_ID = 99L;
    private static final String THREAD_ID = "thread-abc";

    @Mock
    private AiHubChatRepository chatRepository;

    @Mock
    private ObjectProvider<com.bytechef.ee.ai.hub.memory.AiHubSessionMemory> aiHubSessionMemoryProvider;

    @Mock
    private com.bytechef.ee.ai.hub.memory.AiHubSessionMemory aiHubSessionMemory;

    @Mock
    private org.springframework.ai.session.SessionService sessionService;

    @Mock
    private org.springframework.ai.session.SessionRepository sessionRepository;

    @Mock
    private com.bytechef.atlas.execution.facade.JobFacade jobFacade;

    @Mock
    private com.bytechef.ee.ai.hub.agent.WorkflowChatJobRegistry jobRegistry;

    @Mock
    private com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry inFlightRunRegistry;

    private AiHubChatServiceImpl chatService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(aiHubSessionMemoryProvider.getIfAvailable())
            .thenReturn(aiHubSessionMemory);
        lenient().when(aiHubSessionMemory.sessionService())
            .thenReturn(sessionService);
        lenient().when(aiHubSessionMemory.sessionRepository())
            .thenReturn(sessionRepository);

        // The optional tool-search ObjectProvider is left null (the delete path's index clear guards on null) —
        // the same shape the previous @InjectMocks wiring produced.
        chatService = new AiHubChatServiceImpl(
            chatRepository, jobFacade, jobRegistry, inFlightRunRegistry, null, aiHubSessionMemoryProvider, null);
    }

    private static org.springframework.ai.session.SessionEvent sessionEvent(
        org.springframework.ai.chat.messages.MessageType type, String text, Instant timestamp) {

        org.springframework.ai.session.SessionEvent event = mock(org.springframework.ai.session.SessionEvent.class);

        org.springframework.ai.chat.messages.Message message =
            type == org.springframework.ai.chat.messages.MessageType.USER
                ? new org.springframework.ai.chat.messages.UserMessage(text)
                : new org.springframework.ai.chat.messages.AssistantMessage(text);

        lenient().when(event.getMessageType())
            .thenReturn(type);
        lenient().when(event.getMessage())
            .thenReturn(message);
        lenient().when(event.getTimestamp())
            .thenReturn(timestamp);

        return event;
    }

    @Test
    void testCreateInsertsNewChat() {
        when(chatRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        ArgumentCaptor<AiHubChat> captor = ArgumentCaptor.forClass(AiHubChat.class);

        when(chatRepository.save(captor.capture())).thenAnswer(invocation -> {
            AiHubChat captured = invocation.getArgument(0);

            captured.setId(42L);

            return captured;
        });

        AiHubChat result = chatService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getThreadId()).isEqualTo(THREAD_ID);
        assertThat(result.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);
        assertThat(result.getMessageCount()).isZero();

        verify(chatRepository).save(any(AiHubChat.class));
    }

    @Test
    void testCreateIsIdempotent() {
        AiHubChat existing =
            buildChat(7L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        AiHubChat result = chatService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID);

        assertThat(result.getId()).isEqualTo(7L);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void testListReturnsFilteredByStatus() {
        List<AiHubChat> activeList = List.of(
            buildChat(1L, USER_ID, "t-1", AiHubChatStatus.ACTIVE),
            buildChat(2L, USER_ID, "t-2", AiHubChatStatus.ACTIVE));

        when(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            eq(WORKSPACE_ID), eq(USER_ID), eq(0), eq(AiHubChatStatus.ACTIVE.ordinal()),
            anyInt()))
                .thenReturn(activeList);

        List<AiHubChat> result =
            chatService.list(WORKSPACE_ID, USER_ID, 0, AiHubChatStatus.ACTIVE);

        assertThat(result).hasSize(2);
        assertThat(result)
            .allSatisfy(
                chat -> assertThat(chat.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE));
    }

    @Test
    void testLoadMessagesThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns the same NotFoundException as a missing row, with no
        // hint that the chat actually exists in another user's namespace.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.loadMessages(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");
    }

    @Test
    void testLoadMessagesThrowsNotFoundOnWorkspaceMismatch() {
        // Probe-oracle defense: cross-workspace access returns the same NotFoundException as a missing row.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.loadMessages(1L, OTHER_WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadMessagesQueriesChatMemory() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        Instant now = Instant.parse("2026-04-23T10:00:00Z");

        List<org.springframework.ai.session.SessionEvent> events =
            List.of(sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "Hello", now));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);

        List<AiHubChatMessage> messages = chatService.loadMessages(1L, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)
            .role()).isEqualTo("USER");
        assertThat(messages.get(0)
            .content()).isEqualTo("Hello");
    }

    @Test
    void testPatchAppliesNonNullFieldsOnly() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        chat.setTitle("Old title");
        chat.setMessageCount(3);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChatPatch patch = new AiHubChatPatch("New title", null, null, null);

        AiHubChat result = chatService.patch(1L, WORKSPACE_ID, USER_ID, patch);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getMessageCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);
    }

    @Test
    void testPatchThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns NotFoundException with the same opaque message as a
        // missing row.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(
            () -> chatService.patch(
                1L, WORKSPACE_ID, OTHER_USER_ID, new AiHubChatPatch("New title", null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("AiHubChat not found");
    }

    @Test
    void testPatchThrowsNotFoundOnWorkspaceMismatch() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(
            () -> chatService.patch(
                1L, OTHER_WORKSPACE_ID, USER_ID, new AiHubChatPatch("New title", null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("AiHubChat not found");
    }

    @Test
    void testDeleteCascadesToChatMemoryAndRemovesRow() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(THREAD_ID);
        verify(chatRepository).delete(chat);
    }

    /**
     * A specialist subagent keeps its own session under {@code <threadId>:<agentType>}; deleting the chat must take
     * those with it rather than leaving one conversation's memory behind for the next one.
     */
    @Test
    void testDeletePurgesSpecialistSessionsAlongsideTheParentSession() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(THREAD_ID);
        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.DATA_ANALYST.key()));
        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.RESEARCH.key()));
        verify(chatRepository).delete(chat);
    }

    /**
     * One failing session delete must not abandon the rest — the purge is best-effort per session, not all-or-nothing.
     */
    @Test
    void testDeleteContinuesPurgingAfterASessionDeleteFails() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        doThrow(new IllegalStateException("session store unavailable")).when(sessionService)
            .delete(THREAD_ID);

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.DATA_ANALYST.key()));
        verify(chatRepository).delete(chat);
    }

    @Test
    void testDeleteThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns NotFoundException, not ForbiddenException.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.delete(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");

        verify(sessionService, never()).delete(any());
        verify(chatRepository, never()).delete(any());
    }

    @Test
    void testDeleteThrowsNotFoundOnWorkspaceMismatch() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.delete(1L, OTHER_WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");

        verify(sessionService, never()).delete(any());
        verify(chatRepository, never()).delete(any());
    }

    @Test
    void testCreateThrowsConflictWhenThreadIdBoundToDifferentWorkspace() {
        AiHubChat existing =
            buildChat(7L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        // The threadId is already claimed by a row living in another workspace, so the create must collide rather
        // than return the existing row idempotently.
        existing.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(chatRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> chatService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(THREAD_ID);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void testCreateThrowsConflictWhenThreadIdOwnedByDifferentUser() {
        long otherUserId = 999L;
        AiHubChat existing =
            buildChat(7L, otherUserId, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        // Without the cross-user check the call would fall through to save() and trigger a 500 from the
        // DataIntegrityViolationException — which also leaks a probe oracle that the threadId is taken.
        assertThatThrownBy(() -> chatService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(THREAD_ID);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void testCreateWorkflowChatPersistsNewRowWithUuidThreadId() {
        // Always-new semantics: service inserts a fresh chat on every call with kind=WORKFLOW_CHAT, the
        // supplied title, and a UUID-prefixed threadId so chat-memory rows are isolated per chat. The
        // partial unique index that previously enforced one-row-per-(workspace, user, environment, workflow)
        // was dropped in 20260505000001; this method no longer reads from the repository before saving.
        ArgumentCaptor<AiHubChat> captor = ArgumentCaptor.forClass(AiHubChat.class);

        when(chatRepository.save(captor.capture())).thenAnswer(invocation -> {
            AiHubChat captured = invocation.getArgument(0);

            captured.setId(77L);

            return captured;
        });

        AiHubChat result = chatService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getKind()).isEqualTo(AiHubChatKind.WORKFLOW_CHAT);
        assertThat(result.getTitle()).isEqualTo("Project — Reply Bot");
        assertThat(result.getWorkflowExecutionId()).isEqualTo("wf-exec-id");
        assertThat(result.getProjectDeploymentId()).isEqualTo(99L);
        // Pin the threadId shape (plain UUID) without locking the random value. A regression that returned
        // to deterministic threadIds keyed off (workflow_execution_id, user) would re-introduce the
        // session-store cross-talk the always-new design is meant to eliminate. The kind column is the
        // authoritative discriminator for routing.
        assertThat(result.getThreadId())
            .as("threadId must be a plain UUID so session-store events are isolated per chat")
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testCreateWorkflowChatProducesDistinctRowsOnEveryCall() {
        // Always-new invariant: two consecutive calls with the same (workspace, user, environment, workflow,
        // deployment) tuple must produce two distinct chat rows with two distinct threadIds. This is
        // the core behavior change from the prior find-or-create design — pin it explicitly so a regression
        // that re-introduces the lookup would fail here.
        when(chatRepository.save(any(AiHubChat.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChat first = chatService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");
        AiHubChat second = chatService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");

        assertThat(first.getThreadId())
            .as("two clicks on the same workflow row must produce distinct threadIds")
            .isNotEqualTo(second.getThreadId());
    }

    @Test
    void testCreateAgentChatStampsAgentChatKind() {
        // Only the kind separates an agent chat from a workflow chat; everything else — the always-new UUID threadId,
        // the bound execution, the deployment, the initial title — is the shared bridged-chat shape. The kind is what
        // survives an agent being undeployed, which is the whole reason it is persisted rather than re-derived.
        when(chatRepository.save(any(AiHubChat.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiHubChat result = chatService.createAgentChat(WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Agent1");

        assertThat(result.getKind()).isEqualTo(AiHubChatKind.AGENT_CHAT);
        assertThat(result.getTitle()).isEqualTo("Agent1");
        assertThat(result.getWorkflowExecutionId()).isEqualTo("wf-exec-id");
        assertThat(result.getProjectDeploymentId()).isEqualTo(99L);
        assertThat(result.getThreadId())
            .as("threadId must be a plain UUID so session-store events are isolated per chat")
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    private static AiHubChat buildChat(
        long id, long userId, String threadId, AiHubChatStatus status) {

        AiHubChat chat = new AiHubChat();

        chat.setId(id);
        chat.setUserId(userId);
        chat.setThreadId(threadId);
        chat.setStatus(status);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        // Fixtures live in WORKSPACE_ID; cross-workspace tests pass OTHER_WORKSPACE_ID as the REQUESTER, which is
        // what the ownership check compares the row's column against.
        chat.setWorkspaceId(WORKSPACE_ID);

        return chat;
    }

    @Test
    void testCancelAiHubRunDelegatesToInFlightRegistryWithChatThreadId() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        when(inFlightRunRegistry.cancel(THREAD_ID, null)).thenReturn(true);

        boolean cancelled = chatService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, null);

        assertThat(cancelled).isTrue();
        verify(inFlightRunRegistry).cancel(THREAD_ID, null);
    }

    @Test
    void testCancelAiHubRunReturnsFalseWhenNoRunIsInFlight() {
        // Idempotent: clicking Stop after the run completed must NOT throw — it returns false so the client
        // can render "nothing to cancel" without exception-handling gymnastics.
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        when(inFlightRunRegistry.cancel(THREAD_ID, null)).thenReturn(false);

        assertThat(chatService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, null)).isFalse();
    }

    @Test
    void testCancelAiHubRunThrowsOnCrossUserAccess() {
        // Ownership gate: another user can't cancel my run even within the same workspace. Mirrors the
        // probe-oracle defence used elsewhere — same NotFoundException as a missing row to avoid leaking
        // chat existence.
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        long otherUserId = USER_ID + 1;

        assertThatThrownBy(() -> chatService.cancelAiHubRun(1L, WORKSPACE_ID, otherUserId, null))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTruncateMessagesFromDeletesAtAndAfterIndex() {
        // Three messages in chat-memory at t=100, t=200, t=300. Truncating from index 1 should delete t=200
        // and t=300, leaving t=100 intact. The DELETE uses timestamp >= cutoff so any messages at the exact
        // cutoff time are also dropped — which is the right semantic: messages indistinguishable in ordering
        // belong to the same truncation window.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        List<org.springframework.ai.session.SessionEvent> events = List.of(
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "one", Instant.ofEpochMilli(100)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.ASSISTANT, "two", Instant.ofEpochMilli(200)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "three", Instant.ofEpochMilli(300)));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);
        when(sessionRepository.compactEvents(eq(THREAD_ID), eq(List.of()), any(), anyLong())).thenReturn(true);

        when(chatRepository.save(any(AiHubChat.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        int deleted = chatService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, 1);

        assertThat(deleted).isEqualTo(2);

        // Truncating from visible message index 1 keeps only the first event; pin the boundary so an off-by-one in
        // the index-to-event mapping would fail the test.
        ArgumentCaptor<List<org.springframework.ai.session.SessionEvent>> keptCaptor =
            ArgumentCaptor.forClass(List.class);

        verify(sessionRepository).compactEvents(eq(THREAD_ID), eq(List.of()), keptCaptor.capture(), eq(0L));

        assertThat(keptCaptor.getValue()).hasSize(1);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTruncateMessagesFromIsNoOpForIndexPastEnd() {
        // Index past the end of the history is a no-op rather than an error. This lets the client send "truncate
        // from N" without racing the server on history length — the worst case is no rows deleted, which is
        // recoverable, vs. a 400 the user can't act on.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        List<org.springframework.ai.session.SessionEvent> events = List.of(
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "one", Instant.ofEpochMilli(100)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.ASSISTANT, "two", Instant.ofEpochMilli(200)));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);

        int deleted = chatService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, 5);

        assertThat(deleted).isZero();

        // Nothing replaced and no save() either — the chat row's updatedAt is preserved when nothing
        // changed, so the sidebar doesn't re-sort for an effectively-no-op call.
        verify(sessionRepository, never()).compactEvents(any(), any(), any(), anyLong());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testTruncateMessagesFromRefusesNegativeIndex() {
        // Negative index would delete every row if mapped through the cutoff path. The service-layer guard
        // makes the contract explicit at the boundary even though the GraphQL surface should reject this
        // earlier — defense in depth catches a future caller that bypasses the GraphQL layer.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void testTruncateMessagesFromRefusesCrossUserAccess() {
        // Probe-oracle defense: cross-user truncation returns the same NotFoundException as a missing row.
        // Without this, the uniform "not found" probe could be circumvented by sending a truncate and
        // observing whether the call short-circuits or proceeds.
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.truncateMessagesFrom(1L, WORKSPACE_ID, OTHER_USER_ID, 0))
            .isInstanceOf(NotFoundException.class);
    }
}
