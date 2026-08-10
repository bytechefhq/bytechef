/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

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

import com.bytechef.ee.ai.hub.audit.AiHubAuditPublisher;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResource;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentResourceKind;
import com.bytechef.ee.ai.hub.personalagent.AiHubPersonalAgentService;
import com.bytechef.ee.ai.hub.subagent.SubAgentSessionMemoryContributor;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskMessage;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskPatch;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskRepository;
import com.bytechef.ee.ai.hub.tool.AiHubAgentType;
import com.bytechef.ee.ai.hub.toolsearch.ToolSearchCatalogFeeder;
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
 * Unit tests for {@link AiHubTaskServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubTaskServiceTest {

    private static final long WORKSPACE_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long USER_ID = 10L;
    private static final long OTHER_USER_ID = 99L;
    private static final String THREAD_ID = "thread-abc";

    @Mock
    private AiHubTaskRepository taskRepository;

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

    private AiHubTaskServiceImpl taskService;

    @org.junit.jupiter.api.BeforeEach
    void setUp() {
        lenient().when(aiHubSessionMemoryProvider.getIfAvailable())
            .thenReturn(aiHubSessionMemory);
        lenient().when(aiHubSessionMemory.sessionService())
            .thenReturn(sessionService);
        lenient().when(aiHubSessionMemory.sessionRepository())
            .thenReturn(sessionRepository);

        // The optional ObjectProvider dependencies are left null (copyAgentToolTemplate and the tool-search /
        // artifact paths guard on null) — the same shape the previous @InjectMocks wiring produced.
        taskService = new AiHubTaskServiceImpl(
            taskRepository, jobFacade, jobRegistry, inFlightRunRegistry, null, null, null, null,
            aiHubSessionMemoryProvider, null);
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
    void testCreateInsertsNewTask() {
        when(taskRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.empty());

        ArgumentCaptor<AiHubTask> captor = ArgumentCaptor.forClass(AiHubTask.class);

        when(taskRepository.save(captor.capture())).thenAnswer(invocation -> {
            AiHubTask captured = invocation.getArgument(0);

            captured.setId(42L);

            return captured;
        });

        AiHubTask result = taskService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID);

        assertThat(result.getId()).isEqualTo(42L);
        assertThat(result.getUserId()).isEqualTo(USER_ID);
        assertThat(result.getWorkspaceId()).isEqualTo(WORKSPACE_ID);
        assertThat(result.getThreadId()).isEqualTo(THREAD_ID);
        assertThat(result.getStatus()).isEqualTo(AiHubTaskStatus.ACTIVE);
        assertThat(result.getMessageCount()).isZero();

        verify(taskRepository).save(any(AiHubTask.class));
    }

    @Test
    void testCreateIsIdempotent() {
        AiHubTask existing =
            buildTask(7L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        AiHubTask result = taskService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID);

        assertThat(result.getId()).isEqualTo(7L);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void testListReturnsFilteredByStatus() {
        List<AiHubTask> activeList = List.of(
            buildTask(1L, USER_ID, "t-1", AiHubTaskStatus.ACTIVE),
            buildTask(2L, USER_ID, "t-2", AiHubTaskStatus.ACTIVE));

        when(taskRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            eq(WORKSPACE_ID), eq(USER_ID), eq(0), eq(AiHubTaskStatus.ACTIVE.ordinal()),
            anyInt()))
                .thenReturn(activeList);

        List<AiHubTask> result =
            taskService.list(WORKSPACE_ID, USER_ID, 0, AiHubTaskStatus.ACTIVE);

        assertThat(result).hasSize(2);
        assertThat(result)
            .allSatisfy(
                task -> assertThat(task.getStatus()).isEqualTo(AiHubTaskStatus.ACTIVE));
    }

    @Test
    void testLoadMessagesThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns the same NotFoundException as a missing row, with no
        // hint that the task actually exists in another user's namespace.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.loadMessages(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubTask not found");
    }

    @Test
    void testLoadMessagesThrowsNotFoundOnWorkspaceMismatch() {
        // Probe-oracle defense: cross-workspace access returns the same NotFoundException as a missing row.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.loadMessages(1L, OTHER_WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubTask not found");
    }

    @Test
    @SuppressWarnings("unchecked")
    void testLoadMessagesQueriesChatMemory() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        Instant now = Instant.parse("2026-04-23T10:00:00Z");

        List<org.springframework.ai.session.SessionEvent> events =
            List.of(sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "Hello", now));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);

        List<AiHubTaskMessage> messages = taskService.loadMessages(1L, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)
            .role()).isEqualTo("USER");
        assertThat(messages.get(0)
            .content()).isEqualTo("Hello");
    }

    @Test
    void testPatchAppliesNonNullFieldsOnly() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        task.setTitle("Old title");
        task.setMessageCount(3);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));
        when(taskRepository.save(any())).thenAnswer(invocation -> invocation.getArgument(0));

        AiHubTaskPatch patch = new AiHubTaskPatch("New title", null, null, null);

        AiHubTask result = taskService.patch(1L, WORKSPACE_ID, USER_ID, patch);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getMessageCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(AiHubTaskStatus.ACTIVE);
    }

    @Test
    void testPatchThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns NotFoundException with the same opaque message as a
        // missing row.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
            () -> taskService.patch(
                1L, WORKSPACE_ID, OTHER_USER_ID, new AiHubTaskPatch("New title", null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("AiHubTask not found");
    }

    @Test
    void testPatchThrowsNotFoundOnWorkspaceMismatch() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(
            () -> taskService.patch(
                1L, OTHER_WORKSPACE_ID, USER_ID, new AiHubTaskPatch("New title", null, null, null)))
                    .isInstanceOf(NotFoundException.class)
                    .hasMessageContaining("AiHubTask not found");
    }

    @Test
    void testDeleteCascadesToChatMemoryAndRemovesRow() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(THREAD_ID);
        verify(taskRepository).delete(task);
    }

    /**
     * A specialist subagent keeps its own session under {@code <threadId>:<agentType>}; deleting the task must take
     * those with it rather than leaving one conversation's memory behind for the next one.
     */
    @Test
    void testDeletePurgesSpecialistSessionsAlongsideTheParentSession() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        taskService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(THREAD_ID);
        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.PERSONAL_AGENT_MANAGER.key()));
        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.RESEARCH.key()));
        verify(taskRepository).delete(task);
    }

    /**
     * One failing session delete must not abandon the rest — the purge is best-effort per session, not all-or-nothing.
     */
    @Test
    void testDeleteContinuesPurgingAfterASessionDeleteFails() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        doThrow(new IllegalStateException("session store unavailable")).when(sessionService)
            .delete(THREAD_ID);

        taskService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(sessionService).delete(
            SubAgentSessionMemoryContributor.sessionKey(THREAD_ID, AiHubAgentType.PERSONAL_AGENT_MANAGER.key()));
        verify(taskRepository).delete(task);
    }

    @Test
    void testDeleteThrowsNotFoundOnOwnershipMismatch() {
        // Probe-oracle defense: cross-user access returns NotFoundException, not ForbiddenException.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubTask not found");

        verify(sessionService, never()).delete(any());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void testDeleteThrowsNotFoundOnWorkspaceMismatch() {
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.delete(1L, OTHER_WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubTask not found");

        verify(sessionService, never()).delete(any());
        verify(taskRepository, never()).delete(any());
    }

    @Test
    void testCreateThrowsConflictWhenThreadIdBoundToDifferentWorkspace() {
        AiHubTask existing =
            buildTask(7L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        // The threadId is already claimed by a row living in another workspace, so the create must collide rather
        // than return the existing row idempotently.
        existing.setWorkspaceId(OTHER_WORKSPACE_ID);

        when(taskRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        assertThatThrownBy(() -> taskService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(THREAD_ID);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCreateThrowsConflictWhenThreadIdOwnedByDifferentUser() {
        long otherUserId = 999L;
        AiHubTask existing =
            buildTask(7L, otherUserId, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(existing));

        // Without the cross-user check the call would fall through to save() and trigger a 500 from the
        // DataIntegrityViolationException — which also leaks a probe oracle that the threadId is taken.
        assertThatThrownBy(() -> taskService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(THREAD_ID);

        verify(taskRepository, never()).save(any());
    }

    @Test
    void testCreateWorkflowChatPersistsNewRowWithUuidThreadId() {
        // Always-new semantics: service inserts a fresh task on every call with kind=WORKFLOW_CHAT, the
        // supplied title, and a UUID-prefixed threadId so chat-memory rows are isolated per task. The
        // partial unique index that previously enforced one-row-per-(workspace, user, environment, workflow)
        // was dropped in 20260505000001; this method no longer reads from the repository before saving.
        ArgumentCaptor<AiHubTask> captor = ArgumentCaptor.forClass(AiHubTask.class);

        when(taskRepository.save(captor.capture())).thenAnswer(invocation -> {
            AiHubTask captured = invocation.getArgument(0);

            captured.setId(77L);

            return captured;
        });

        AiHubTask result = taskService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");

        assertThat(result.getId()).isEqualTo(77L);
        assertThat(result.getKind()).isEqualTo(AiHubTaskKind.WORKFLOW_CHAT);
        assertThat(result.getTitle()).isEqualTo("Project — Reply Bot");
        assertThat(result.getWorkflowExecutionId()).isEqualTo("wf-exec-id");
        assertThat(result.getProjectDeploymentId()).isEqualTo(99L);
        // Pin the threadId shape (plain UUID) without locking the random value. A regression that returned
        // to deterministic threadIds keyed off (workflow_execution_id, user) would re-introduce the
        // session-store cross-talk the always-new design is meant to eliminate. The kind column is the
        // authoritative discriminator for routing.
        assertThat(result.getThreadId())
            .as("threadId must be a plain UUID so session-store events are isolated per task")
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
    }

    @Test
    void testCreateWorkflowChatProducesDistinctRowsOnEveryCall() {
        // Always-new invariant: two consecutive calls with the same (workspace, user, environment, workflow,
        // deployment) tuple must produce two distinct task rows with two distinct threadIds. This is
        // the core behavior change from the prior find-or-create design — pin it explicitly so a regression
        // that re-introduces the lookup would fail here.
        when(taskRepository.save(any(AiHubTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiHubTask first = taskService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");
        AiHubTask second = taskService.createWorkflowChat(
            WORKSPACE_ID, USER_ID, 0, "wf-exec-id", 99L, "Project — Reply Bot");

        assertThat(first.getThreadId())
            .as("two clicks on the same workflow row must produce distinct threadIds")
            .isNotEqualTo(second.getThreadId());
    }

    private static AiHubTask buildTask(
        long id, long userId, String threadId, AiHubTaskStatus status) {

        AiHubTask task = new AiHubTask();

        task.setId(id);
        task.setUserId(userId);
        task.setThreadId(threadId);
        task.setStatus(status);
        task.setMessageCount(0);
        task.setCreatedAt(LocalDateTime.now());
        task.setUpdatedAt(LocalDateTime.now());
        // Fixtures live in WORKSPACE_ID; cross-workspace tests pass OTHER_WORKSPACE_ID as the REQUESTER, which is
        // what the ownership check compares the row's column against.
        task.setWorkspaceId(WORKSPACE_ID);

        return task;
    }

    @Test
    void testCancelAiHubRunDelegatesToInFlightRegistryWithTaskThreadId() {
        AiHubTask task = buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        when(inFlightRunRegistry.cancel(THREAD_ID, null)).thenReturn(true);

        boolean cancelled = taskService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, null);

        assertThat(cancelled).isTrue();
        verify(inFlightRunRegistry).cancel(THREAD_ID, null);
    }

    @Test
    void testCancelAiHubRunReturnsFalseWhenNoRunIsInFlight() {
        // Idempotent: clicking Stop after the run completed must NOT throw — it returns false so the client
        // can render "nothing to cancel" without exception-handling gymnastics.
        AiHubTask task = buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        when(inFlightRunRegistry.cancel(THREAD_ID, null)).thenReturn(false);

        assertThat(taskService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, null)).isFalse();
    }

    @Test
    void testCancelAiHubRunThrowsOnCrossUserAccess() {
        // Ownership gate: another user can't cancel my run even within the same workspace. Mirrors the
        // probe-oracle defence used elsewhere — same NotFoundException as a missing row to avoid leaking
        // task existence.
        AiHubTask task = buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        long otherUserId = USER_ID + 1;

        assertThatThrownBy(() -> taskService.cancelAiHubRun(1L, WORKSPACE_ID, otherUserId, null))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testTruncateMessagesFromDeletesAtAndAfterIndex() {
        // Three messages in chat-memory at t=100, t=200, t=300. Truncating from index 1 should delete t=200
        // and t=300, leaving t=100 intact. The DELETE uses timestamp >= cutoff so any messages at the exact
        // cutoff time are also dropped — which is the right semantic: messages indistinguishable in ordering
        // belong to the same truncation window.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        List<org.springframework.ai.session.SessionEvent> events = List.of(
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "one", Instant.ofEpochMilli(100)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.ASSISTANT, "two", Instant.ofEpochMilli(200)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "three", Instant.ofEpochMilli(300)));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);
        when(sessionRepository.compactEvents(eq(THREAD_ID), eq(List.of()), any(), anyLong())).thenReturn(true);

        when(taskRepository.save(any(AiHubTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        int deleted = taskService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, 1);

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
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        List<org.springframework.ai.session.SessionEvent> events = List.of(
            sessionEvent(org.springframework.ai.chat.messages.MessageType.USER, "one", Instant.ofEpochMilli(100)),
            sessionEvent(org.springframework.ai.chat.messages.MessageType.ASSISTANT, "two", Instant.ofEpochMilli(200)));

        when(sessionService.getEvents(THREAD_ID)).thenReturn(events);

        int deleted = taskService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, 5);

        assertThat(deleted).isZero();

        // Nothing replaced and no save() either — the task row's updatedAt is preserved when nothing
        // changed, so the sidebar doesn't re-sort for an effectively-no-op call.
        verify(sessionRepository, never()).compactEvents(any(), any(), any(), anyLong());
        verify(taskRepository, never()).save(any());
    }

    @Test
    void testTruncateMessagesFromRefusesNegativeIndex() {
        // Negative index would delete every row if mapped through the cutoff path. The service-layer guard
        // makes the contract explicit at the boundary even though the GraphQL surface should reject this
        // earlier — defense in depth catches a future caller that bypasses the GraphQL layer.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.truncateMessagesFrom(1L, WORKSPACE_ID, USER_ID, -1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void testTruncateMessagesFromRefusesCrossUserAccess() {
        // Probe-oracle defense: cross-user truncation returns the same NotFoundException as a missing row.
        // Without this, the uniform "not found" probe could be circumvented by sending a truncate and
        // observing whether the call short-circuits or proceeds.
        AiHubTask task =
            buildTask(1L, USER_ID, THREAD_ID, AiHubTaskStatus.ACTIVE);

        when(taskRepository.findById(1L)).thenReturn(Optional.of(task));

        assertThatThrownBy(() -> taskService.truncateMessagesFrom(1L, WORKSPACE_ID, OTHER_USER_ID, 0))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCreateAiHubPersonalAgentChatPersistsNewRowWithUuidThreadId() {
        // Always-new semantics: service inserts a fresh task on every call with kind=PERSONAL_AGENT, the
        // supplied title, and a UUID-prefixed threadId so chat-memory rows are isolated per task. The
        // partial unique index that previously enforced one-row-per-(workspace, user, environment, agent) was
        // dropped in 20260505000001; this method no longer reads from the repository before saving.
        when(taskRepository.save(any(AiHubTask.class))).thenAnswer(invocation -> {
            AiHubTask captured = invocation.getArgument(0);

            captured.setId(123L);

            return captured;
        });

        AiHubTask result = taskService.createAiHubPersonalAgentChat(
            WORKSPACE_ID, USER_ID, 0, 42L, "Research Assistant");

        assertThat(result.getId()).isEqualTo(123L);
        assertThat(result.getKind()).isEqualTo(AiHubTaskKind.PERSONAL_AGENT);
        assertThat(result.getTitle()).isEqualTo("Research Assistant");
        assertThat(result.getAiHubPersonalAgentId()).isEqualTo(42L);
        // Pin the threadId shape (plain UUID) without locking the random value. The kind column
        // distinguishes personal-agent rows from workflow-chat rows.
        assertThat(result.getThreadId())
            .as("threadId must be a plain UUID so session-store events are isolated per task")
            .matches("[0-9a-f]{8}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{4}-[0-9a-f]{12}");
        // autoTitled defaults to true on new rows so the LLM-driven generateAiHubTaskTitle loop runs
        // until a title is authoritatively set (LLM regen, or user rename). A regression that flips the
        // default to false would silently disable title regeneration for every new PA task,
        // leaving them stuck at the initial creation-time placeholder forever.
        assertThat(result.isAutoTitled())
            .as("new PA tasks must be eligible for LLM title regeneration")
            .isTrue();
    }

    @Test
    void testCreateAiHubPersonalAgentChatProducesDistinctRowsOnEveryCall() {
        // Always-new invariant: two consecutive calls with the same (workspace, user, environment, agent)
        // tuple must produce two distinct task rows with two distinct threadIds. Mirrors the
        // workflow-chat invariant and pins the always-new design against a regression that re-introduces
        // find-or-create.
        when(taskRepository.save(any(AiHubTask.class)))
            .thenAnswer(invocation -> invocation.getArgument(0));

        AiHubTask first = taskService.createAiHubPersonalAgentChat(
            WORKSPACE_ID, USER_ID, 0, 42L, "Research Assistant");
        AiHubTask second = taskService.createAiHubPersonalAgentChat(
            WORKSPACE_ID, USER_ID, 0, 42L, "Research Assistant");

        assertThat(first.getThreadId())
            .as("two clicks on the same personal agent row must produce distinct threadIds")
            .isNotEqualTo(second.getThreadId());
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateAiHubPersonalAgentChatCopiesAgentResourcesAsArtifacts() {
        // Verifies that copyAgentResourceTemplate calls AiHubTaskArtifactService.recordReference once per
        // resource and maps the kind correctly: WORKFLOW → WORKFLOW_REFERENCED, FILE → FILE_REFERENCED.
        AiHubPersonalAgentService personalAgentService = mock(AiHubPersonalAgentService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);

        AiHubTaskServiceImpl service = newServiceWithProviders(personalAgentService, artifactService);

        stubSaveReturnsId(55L);

        AiHubPersonalAgentResource workflowResource =
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");
        AiHubPersonalAgentResource fileResource =
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.FILE, "file-9", "notes.md");

        when(personalAgentService.listResources(42L)).thenReturn(List.of(workflowResource, fileResource));

        AiHubTask result = service.createAiHubPersonalAgentChat(WORKSPACE_ID, USER_ID, 0, 42L, "Research Assistant");

        assertThat(result.getId()).isEqualTo(55L);

        verify(artifactService).recordReference(
            55L, WORKSPACE_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, "wf-1", "Daily standup", null);
        verify(artifactService).recordReference(
            55L, WORKSPACE_ID, USER_ID, AiHubTaskArtifactKind.FILE_REFERENCED, "file-9", "notes.md", null);
    }

    @Test
    @SuppressWarnings("unchecked")
    void testCreateAiHubPersonalAgentChatSkipsFailingResourceWithoutAbortingTaskCreation() {
        // Per-resource failure must be swallowed (loop continues) and must NOT abort task creation.
        // Verify: task still returned non-null AND second resource still recorded despite first throwing.
        AiHubPersonalAgentService personalAgentService = mock(AiHubPersonalAgentService.class);
        AiHubTaskArtifactService artifactService = mock(AiHubTaskArtifactService.class);

        AiHubTaskServiceImpl service = newServiceWithProviders(personalAgentService, artifactService);

        stubSaveReturnsId(66L);

        AiHubPersonalAgentResource firstResource =
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.WORKFLOW, "wf-1", "Daily standup");
        AiHubPersonalAgentResource secondResource =
            new AiHubPersonalAgentResource(42L, AiHubPersonalAgentResourceKind.FILE, "file-9", "notes.md");

        when(personalAgentService.listResources(42L)).thenReturn(List.of(firstResource, secondResource));

        doThrow(new RuntimeException("simulated failure"))
            .when(artifactService)
            .recordReference(
                eq(66L), eq(WORKSPACE_ID), eq(USER_ID), eq(AiHubTaskArtifactKind.WORKFLOW_REFERENCED),
                eq("wf-1"), eq("Daily standup"), eq(null));

        AiHubTask result = service.createAiHubPersonalAgentChat(WORKSPACE_ID, USER_ID, 0, 42L, "Research Assistant");

        assertThat(result).isNotNull();
        assertThat(result.getId()).isEqualTo(66L);

        // Second resource must still be recorded despite the first failing.
        verify(artifactService).recordReference(
            66L, WORKSPACE_ID, USER_ID, AiHubTaskArtifactKind.FILE_REFERENCED, "file-9", "notes.md", null);
    }

    /**
     * Constructs an {@link AiHubTaskServiceImpl} manually with the shared {@code @Mock} infrastructure fields plus
     * explicit {@link ObjectProvider} mocks for {@link AiHubPersonalAgentService} and {@link AiHubTaskArtifactService}.
     * The tool-related providers return {@code null} from {@code getIfAvailable()} so {@code copyAgentToolTemplate}
     * stays a no-op and the tests isolate the resource copy path. Mirrors
     * {@code AiHubPersonalAgentServiceTest#newService()}.
     */
    @SuppressWarnings("unchecked")
    private AiHubTaskServiceImpl newServiceWithProviders(
        AiHubPersonalAgentService personalAgentService,
        AiHubTaskArtifactService artifactService) {

        ObjectProvider<AiHubPersonalAgentService> personalAgentServiceProvider =
            mock(ObjectProvider.class);

        lenient().when(personalAgentServiceProvider.getIfAvailable())
            .thenReturn(personalAgentService);

        ObjectProvider<AiHubTaskToolFacade> taskToolFacadeProvider = mock(ObjectProvider.class);

        lenient().when(taskToolFacadeProvider.getIfAvailable())
            .thenReturn(null);

        ObjectProvider<ToolSearchCatalogFeeder> toolSearchCatalogFeederProvider = mock(ObjectProvider.class);

        lenient().when(toolSearchCatalogFeederProvider.getIfAvailable())
            .thenReturn(null);

        ObjectProvider<AiHubTaskArtifactService> taskArtifactServiceProvider = mock(ObjectProvider.class);

        lenient().when(taskArtifactServiceProvider.getIfAvailable())
            .thenReturn(artifactService);

        return new AiHubTaskServiceImpl(
            taskRepository, jobFacade, jobRegistry, inFlightRunRegistry,
            personalAgentServiceProvider, taskToolFacadeProvider, toolSearchCatalogFeederProvider,
            taskArtifactServiceProvider, aiHubSessionMemoryProvider, mock(AiHubAuditPublisher.class));
    }

    /**
     * Stubs {@code taskRepository.save} so the returned task has a non-null {@code id}. Required for the copy-path
     * tests because {@code copyAgentResourceTemplate} guards on {@code saved.getId() != null}.
     */
    private void stubSaveReturnsId(long taskId) {
        when(taskRepository.save(any(AiHubTask.class))).thenAnswer(invocation -> {
            AiHubTask captured = invocation.getArgument(0);

            captured.setId(taskId);

            return captured;
        });
    }
}
