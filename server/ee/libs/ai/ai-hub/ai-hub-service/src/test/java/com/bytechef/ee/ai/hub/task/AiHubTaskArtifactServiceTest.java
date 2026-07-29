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
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskArtifactRepository;
import com.bytechef.ee.ai.hub.task.repository.AiHubTaskRepository;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link AiHubTaskArtifactServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubTaskArtifactServiceTest {

    private static final long USER_ID = 42L;
    private static final long OTHER_USER_ID = 99L;
    private static final long TASK_ID = 100L;
    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final String THREAD_ID = "thread-abc-123";

    @Mock
    private AiHubTaskArtifactRepository taskArtifactRepository;

    @Mock
    private AiHubTaskRepository taskRepository;

    @Mock
    private com.bytechef.ee.ai.hub.task.repository.WorkspaceAiHubTaskRepository workspaceTaskRepository;

    @Mock
    private JsonMapper jsonMapper;

    @InjectMocks
    private AiHubTaskArtifactServiceImpl taskArtifactService;

    private ListAppender<ILoggingEvent> logAppender;
    @SuppressWarnings("PMD")
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(AiHubTaskArtifactServiceImpl.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    @Test
    void testRecordSavesArtifactWhenTaskFound() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID))
            .thenReturn(Optional.of(task));

        taskArtifactService.record(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_CREATED,
            "wf-001", "My Workflow", null);

        ArgumentCaptor<AiHubTaskArtifact> captor =
            ArgumentCaptor.forClass(AiHubTaskArtifact.class);

        verify(taskArtifactRepository).save(captor.capture());

        AiHubTaskArtifact saved = captor.getValue();

        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
        assertThat(saved.getKind()).isEqualTo(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactId()).isEqualTo("wf-001");
        assertThat(saved.getArtifactName()).isEqualTo("My Workflow");
        assertThat(saved.getMetadataJson()).isNull();
    }

    @Test
    void testRecordWarnsAndSkipsSaveWhenTaskNotFound() {
        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        taskArtifactService.record(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.KB_DOCUMENT_ADDED,
            "doc-1", "guide.md", null);

        verify(taskArtifactRepository, never()).save(any());

        // The orphan must be surfaced loudly so production logs catch this case. The diagnostic message must
        // include enough context — threadId, userId, kind, artifactId, artifactName — to reproduce the issue.
        List<ILoggingEvent> warnEvents = logAppender.list.stream()
            .filter(event -> event.getLevel() == Level.WARN)
            .toList();

        assertThat(warnEvents).hasSize(1);

        String formattedMessage = warnEvents.get(0)
            .getFormattedMessage();

        assertThat(formattedMessage).contains(THREAD_ID);
        assertThat(formattedMessage).contains(String.valueOf(USER_ID));
        assertThat(formattedMessage).contains(AiHubTaskArtifactKind.KB_DOCUMENT_ADDED.name());
        assertThat(formattedMessage).contains("doc-1");
        assertThat(formattedMessage).contains("guide.md");
    }

    @Test
    void testListByTaskReturnsArtifactsOrderedNewestFirst() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);
        AiHubTaskArtifact firstArtifact = buildArtifact(1L, "WORKFLOW_CREATED", "wf-001");
        AiHubTaskArtifact secondArtifact = buildArtifact(2L, "KB_DOCUMENT_ADDED", "doc-1");
        List<AiHubTaskArtifact> expectedArtifacts = List.of(secondArtifact, firstArtifact);

        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(WORKSPACE_ID, TASK_ID))
            .thenReturn(Optional.of(
                new WorkspaceAiHubTask(WORKSPACE_ID, TASK_ID)));
        when(
            taskArtifactRepository.findByTaskIdOrderByCreatedAtDesc(
                eq(TASK_ID), any(org.springframework.data.domain.Limit.class)))
                    .thenReturn(expectedArtifacts);

        List<AiHubTaskArtifact> result =
            taskArtifactService.listByTask(TASK_ID, WORKSPACE_ID, USER_ID);

        assertThat(result).isSameAs(expectedArtifacts);
    }

    @Test
    void testListByTaskThrowsWhenTaskNotFound() {
        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> taskArtifactService.listByTask(TASK_ID, WORKSPACE_ID, USER_ID))
                .isInstanceOf(com.bytechef.ee.ai.hub.exception.NotFoundException.class)
                .hasMessageContaining(String.valueOf(TASK_ID));
    }

    @Test
    void testListByTaskThrowsWhenOwnershipMismatch() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));

        assertThatThrownBy(
            () -> taskArtifactService.listByTask(TASK_ID, WORKSPACE_ID, OTHER_USER_ID))
                .isInstanceOf(com.bytechef.ee.ai.hub.exception.ForbiddenException.class)
                .hasMessageContaining(String.valueOf(OTHER_USER_ID))
                .hasMessageContaining(String.valueOf(TASK_ID));
    }

    @Test
    void testListByTaskThrowsWhenWorkspaceMismatch() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        // Membership lookup returns empty → workspace doesn't claim this task → ownership-check rejects.
        when(workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(OTHER_WORKSPACE_ID, TASK_ID))
            .thenReturn(Optional.empty());

        assertThatThrownBy(
            () -> taskArtifactService.listByTask(TASK_ID, OTHER_WORKSPACE_ID, USER_ID))
                .isInstanceOf(com.bytechef.ee.ai.hub.exception.ForbiddenException.class)
                .hasMessageContaining("is not in workspace")
                .hasMessageContaining(String.valueOf(TASK_ID));
    }

    @Test
    void testCountByTaskDelegatesToRepository() {
        when(taskArtifactRepository.countByTaskId(TASK_ID)).thenReturn(5L);

        long count = taskArtifactService.countByTask(TASK_ID);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void testRecordWorkflowArtifactInsertsWhenAbsent() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(eq(TASK_ID), eq("wf-1"), any()))
            .thenReturn(Optional.empty());
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        ArgumentCaptor<AiHubTaskArtifact> captor = ArgumentCaptor.forClass(AiHubTaskArtifact.class);

        verify(taskArtifactRepository).save(captor.capture());

        AiHubTaskArtifact saved = captor.getValue();

        assertThat(saved.getKind()).isEqualTo(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactId()).isEqualTo("wf-1");
        assertThat(saved.getArtifactName()).isEqualTo("My Flow");
        assertThat(saved.getTaskId()).isEqualTo(TASK_ID);
    }

    @Test
    void testRecordWorkflowArtifactDedupsAndKeepsExistingKind() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        AiHubTaskArtifact existing = new AiHubTaskArtifact();

        existing.setId(500L);
        existing.setTaskId(TASK_ID);
        existing.setKind(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        existing.setArtifactId("wf-1");
        existing.setArtifactName("Old Name");

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(task));
        when(taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(eq(TASK_ID), eq("wf-1"), any()))
            .thenReturn(Optional.of(existing));
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, "wf-1", 7L, 55L, "New Name");

        ArgumentCaptor<AiHubTaskArtifact> captor = ArgumentCaptor.forClass(AiHubTaskArtifact.class);

        verify(taskArtifactRepository).save(captor.capture());

        AiHubTaskArtifact saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(500L);
        assertThat(saved.getKind()).isEqualTo(AiHubTaskArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactName()).isEqualTo("New Name");
    }

    @Test
    void testRecordWorkflowArtifactDropsWhenTaskMissing() {
        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(taskArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordWorkflowArtifactSkipsWhenUserIdNull() {
        taskArtifactService.recordWorkflowArtifact(
            THREAD_ID, null, AiHubTaskArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(taskRepository, never()).findByThreadIdAndUserId(anyString(), anyLong());
        verify(taskArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceCollapsesWorkflowOntoExistingCreatedRow() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);

        AiHubTaskArtifact existing = buildArtifact(500L, "WORKFLOW_CREATED", "wf-1");

        when(taskRepository.findById(TASK_ID)).thenReturn(Optional.of(task));
        when(workspaceTaskRepository.findByWorkspaceIdAndAiHubTaskId(WORKSPACE_ID, TASK_ID))
            .thenReturn(Optional.of(new WorkspaceAiHubTask(WORKSPACE_ID, TASK_ID)));
        // Cross-kind workflow lookup finds the row already written server-side by createProjectWorkflow.
        when(taskArtifactRepository.findFirstByTaskIdAndArtifactIdAndKindIn(eq(TASK_ID), eq("wf-1"), any()))
            .thenReturn(Optional.of(existing));

        AiHubTaskArtifact result = taskArtifactService.recordReference(
            TASK_ID, WORKSPACE_ID, USER_ID, AiHubTaskArtifactKind.WORKFLOW_REFERENCED, "wf-1",
            "Gmail to Slack", null);

        // The client-driven WORKFLOW_REFERENCED record must return the existing server-side WORKFLOW_CREATED
        // row rather than inserting a second sidebar entry for the same workflow.
        assertThat(result).isSameAs(existing);

        verify(taskArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceByThreadDedupsOnThreadKindArtifactId() {
        AiHubTask task = buildTask(TASK_ID, USER_ID, THREAD_ID);
        AiHubTaskArtifact existing = buildArtifact(500L, "SKILL_REFERENCED", "5");

        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.of(task));
        when(
            taskArtifactRepository.findFirstByTaskIdAndKindAndArtifactId(
                TASK_ID, AiHubTaskArtifactKind.SKILL_REFERENCED.ordinal(), "5"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existing));
        when(taskArtifactRepository.save(any())).thenReturn(existing);

        AiHubTaskArtifact firstResult = taskArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");
        AiHubTaskArtifact secondResult = taskArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        // The second call must collapse onto the row saved by the first call rather than inserting a duplicate.
        assertThat(firstResult).isSameAs(existing);
        assertThat(secondResult).isSameAs(existing);
        verify(taskArtifactRepository, times(1)).save(any());
    }

    @Test
    void testRecordReferenceByThreadReturnsNullWhenUserIdNull() {
        AiHubTaskArtifact result = taskArtifactService.recordReferenceByThread(
            THREAD_ID, null, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        assertThat(result).isNull();

        verify(taskRepository, never()).findByThreadIdAndUserId(anyString(), anyLong());
        verify(taskArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceByThreadReturnsNullWhenTaskNotFound() {
        when(taskRepository.findByThreadIdAndUserId(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        AiHubTaskArtifact result = taskArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubTaskArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        assertThat(result).isNull();

        verify(taskArtifactRepository, never()).save(any());
    }

    private AiHubTask buildTask(long id, long userId, String threadId) {
        AiHubTask task = new AiHubTask();

        task.setId(id);
        task.setUserId(userId);
        task.setThreadId(threadId);
        task.setStatus(AiHubTaskStatus.ACTIVE);

        return task;
    }

    private AiHubTaskArtifact buildArtifact(long id, String kind, String artifactId) {
        AiHubTaskArtifact artifact = new AiHubTaskArtifact();

        artifact.setId(id);
        artifact.setTaskId(TASK_ID);
        artifact.setKind(AiHubTaskArtifactKind.valueOf(kind));
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName("Artifact " + artifactId);

        return artifact;
    }
}
