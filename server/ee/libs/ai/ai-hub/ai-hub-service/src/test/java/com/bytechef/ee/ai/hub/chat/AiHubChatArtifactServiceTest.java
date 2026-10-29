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
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatArtifactRepository;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
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
import org.springframework.beans.factory.ObjectProvider;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link AiHubChatArtifactServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubChatArtifactServiceTest {

    private static final long USER_ID = 42L;
    private static final long OTHER_USER_ID = 99L;
    private static final long CHAT_ID = 100L;
    private static final long WORKSPACE_ID = 7L;
    private static final long OTHER_WORKSPACE_ID = 8L;
    private static final String THREAD_ID = "thread-abc-123";

    @Mock
    private AiHubChatArtifactRepository chatArtifactRepository;

    @Mock
    private AiHubChatService chatService;

    @Mock
    private JsonMapper jsonMapper;

    @Mock
    private ObjectProvider<ProjectWorkflowService> projectWorkflowServiceProvider;

    @Mock
    private ProjectWorkflowService projectWorkflowService;

    @InjectMocks
    private AiHubChatArtifactServiceImpl chatArtifactService;

    private ListAppender<ILoggingEvent> logAppender;
    @SuppressWarnings("PMD")
    private Logger serviceLogger;

    @BeforeEach
    void setUp() {
        serviceLogger = (Logger) LoggerFactory.getLogger(AiHubChatArtifactServiceImpl.class);
        logAppender = new ListAppender<>();

        logAppender.start();
        serviceLogger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        serviceLogger.detachAppender(logAppender);
    }

    @Test
    void testRecordSavesArtifactWhenChatFound() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);

        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID))
            .thenReturn(Optional.of(chat));

        chatArtifactService.record(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_CREATED,
            "wf-001", "My Workflow", null);

        ArgumentCaptor<AiHubChatArtifact> captor =
            ArgumentCaptor.forClass(AiHubChatArtifact.class);

        verify(chatArtifactRepository).save(captor.capture());

        AiHubChatArtifact saved = captor.getValue();

        assertThat(saved.getChatId()).isEqualTo(CHAT_ID);
        assertThat(saved.getKind()).isEqualTo(AiHubChatArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactId()).isEqualTo("wf-001");
        assertThat(saved.getArtifactName()).isEqualTo("My Workflow");
        assertThat(saved.getMetadataJson()).isNull();
    }

    @Test
    void testRecordWarnsAndSkipsSaveWhenChatNotFound() {
        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        chatArtifactService.record(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.KB_DOCUMENT_ADDED,
            "doc-1", "guide.md", null);

        verify(chatArtifactRepository, never()).save(any());

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
        assertThat(formattedMessage).contains(AiHubChatArtifactKind.KB_DOCUMENT_ADDED.name());
        assertThat(formattedMessage).contains("doc-1");
        assertThat(formattedMessage).contains("guide.md");
    }

    @Test
    void testListByChatReturnsArtifactsOrderedNewestFirst() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);
        AiHubChatArtifact firstArtifact = buildArtifact(1L, "WORKFLOW_CREATED", "wf-001");
        AiHubChatArtifact secondArtifact = buildArtifact(2L, "KB_DOCUMENT_ADDED", "doc-1");
        List<AiHubChatArtifact> expectedArtifacts = List.of(secondArtifact, firstArtifact);

        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(chat);
        when(
            chatArtifactRepository.findByChatIdOrderByCreatedAtDesc(
                eq(CHAT_ID), any(org.springframework.data.domain.Limit.class)))
                    .thenReturn(expectedArtifacts);

        List<AiHubChatArtifact> result =
            chatArtifactService.listByChat(CHAT_ID, WORKSPACE_ID, USER_ID);

        assertThat(result).isSameAs(expectedArtifacts);
    }

    @Test
    void testListByChatDropsWorkflowArtifactsWhoseWorkflowWasDeleted() {
        AiHubChatArtifact deletedWorkflowArtifact = buildArtifact(1L, "WORKFLOW_CREATED", "wf-deleted");
        AiHubChatArtifact updatedWorkflowArtifact = buildArtifact(2L, "WORKFLOW_UPDATED", "wf-kept");
        AiHubChatArtifact referencedDeletedWorkflowArtifact = buildArtifact(3L, "WORKFLOW_REFERENCED", "wf-deleted");
        AiHubChatArtifact documentArtifact = buildArtifact(4L, "KB_DOCUMENT_ADDED", "doc-1");

        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(
            buildChat(CHAT_ID, USER_ID, THREAD_ID));
        when(
            chatArtifactRepository.findByChatIdOrderByCreatedAtDesc(
                eq(CHAT_ID), any(org.springframework.data.domain.Limit.class)))
                    .thenReturn(List.of(
                        documentArtifact, referencedDeletedWorkflowArtifact, updatedWorkflowArtifact,
                        deletedWorkflowArtifact));
        when(projectWorkflowServiceProvider.getIfAvailable()).thenReturn(projectWorkflowService);
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("wf-deleted", "wf-kept")))
            .thenReturn(List.of(new ProjectWorkflow(1053L, 1, "wf-kept")));

        assertThat(chatArtifactService.listByChat(CHAT_ID, WORKSPACE_ID, USER_ID))
            .containsExactly(documentArtifact, updatedWorkflowArtifact);
    }

    @Test
    void testListByChatListsEveryArtifactWhenTheWorkflowLookupIsRemote() {
        AiHubChatArtifact workflowArtifact = buildArtifact(1L, "WORKFLOW_CREATED", "wf-001");

        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(
            buildChat(CHAT_ID, USER_ID, THREAD_ID));
        when(
            chatArtifactRepository.findByChatIdOrderByCreatedAtDesc(
                eq(CHAT_ID), any(org.springframework.data.domain.Limit.class)))
                    .thenReturn(List.of(workflowArtifact));
        when(projectWorkflowServiceProvider.getIfAvailable()).thenReturn(projectWorkflowService);
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("wf-001")))
            .thenThrow(new UnsupportedOperationException());

        assertThat(chatArtifactService.listByChat(CHAT_ID, WORKSPACE_ID, USER_ID)).containsExactly(workflowArtifact);
    }

    @Test
    void testListByChatPropagatesAnyOtherWorkflowLookupFailure() {
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(
            buildChat(CHAT_ID, USER_ID, THREAD_ID));
        when(
            chatArtifactRepository.findByChatIdOrderByCreatedAtDesc(
                eq(CHAT_ID), any(org.springframework.data.domain.Limit.class)))
                    .thenReturn(List.of(buildArtifact(1L, "WORKFLOW_CREATED", "wf-001")));
        when(projectWorkflowServiceProvider.getIfAvailable()).thenReturn(projectWorkflowService);
        when(projectWorkflowService.getWorkflowProjectWorkflows(List.of("wf-001")))
            .thenThrow(new IllegalStateException("database down"));

        assertThatThrownBy(() -> chatArtifactService.listByChat(CHAT_ID, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalStateException.class)
            .hasMessage("database down");
    }

    @Test
    void testListByChatThrowsWhenChatNotViewable() {
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, OTHER_USER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));

        assertThatThrownBy(() -> chatArtifactService.listByChat(CHAT_ID, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class);

        verify(chatArtifactRepository, never()).findByChatIdOrderByCreatedAtDesc(anyLong(), any());
    }

    @Test
    void testListByChatThrowsWhenWorkspaceMismatch() {
        when(chatService.getViewable(CHAT_ID, OTHER_WORKSPACE_ID, USER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));

        assertThatThrownBy(() -> chatArtifactService.listByChat(CHAT_ID, OTHER_WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testCountByChatDelegatesToRepository() {
        when(chatArtifactRepository.countByChatId(CHAT_ID)).thenReturn(5L);

        long count = chatArtifactService.countByChat(CHAT_ID);

        assertThat(count).isEqualTo(5L);
    }

    @Test
    void testRecordWorkflowArtifactInsertsWhenAbsent() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);

        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.of(chat));
        when(chatArtifactRepository.findFirstByChatIdAndArtifactIdAndKindIn(eq(CHAT_ID), eq("wf-1"), any()))
            .thenReturn(Optional.empty());
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        chatArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        ArgumentCaptor<AiHubChatArtifact> captor = ArgumentCaptor.forClass(AiHubChatArtifact.class);

        verify(chatArtifactRepository).save(captor.capture());

        AiHubChatArtifact saved = captor.getValue();

        assertThat(saved.getKind()).isEqualTo(AiHubChatArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactId()).isEqualTo("wf-1");
        assertThat(saved.getArtifactName()).isEqualTo("My Flow");
        assertThat(saved.getChatId()).isEqualTo(CHAT_ID);
    }

    @Test
    void testRecordWorkflowArtifactDedupsAndKeepsExistingKind() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);

        AiHubChatArtifact existing = new AiHubChatArtifact();

        existing.setId(500L);
        existing.setChatId(CHAT_ID);
        existing.setKind(AiHubChatArtifactKind.WORKFLOW_CREATED);
        existing.setArtifactId("wf-1");
        existing.setArtifactName("Old Name");

        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.of(chat));
        when(chatArtifactRepository.findFirstByChatIdAndArtifactIdAndKindIn(eq(CHAT_ID), eq("wf-1"), any()))
            .thenReturn(Optional.of(existing));
        when(jsonMapper.writeValueAsString(any())).thenReturn("{}");

        chatArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_REFERENCED, "wf-1", 7L, 55L, "New Name");

        ArgumentCaptor<AiHubChatArtifact> captor = ArgumentCaptor.forClass(AiHubChatArtifact.class);

        verify(chatArtifactRepository).save(captor.capture());

        AiHubChatArtifact saved = captor.getValue();

        assertThat(saved.getId()).isEqualTo(500L);
        assertThat(saved.getKind()).isEqualTo(AiHubChatArtifactKind.WORKFLOW_CREATED);
        assertThat(saved.getArtifactName()).isEqualTo("New Name");
    }

    @Test
    void testRecordWorkflowArtifactDropsWhenChatMissing() {
        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        chatArtifactService.recordWorkflowArtifact(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(chatArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordWorkflowArtifactSkipsWhenUserIdNull() {
        chatArtifactService.recordWorkflowArtifact(
            THREAD_ID, null, AiHubChatArtifactKind.WORKFLOW_CREATED, "wf-1", 7L, 55L, "My Flow");

        verify(chatService, never()).findByThreadIdViewable(anyString(), anyLong());
        verify(chatArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceCollapsesWorkflowOntoExistingCreatedRow() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);

        AiHubChatArtifact existing = buildArtifact(500L, "WORKFLOW_CREATED", "wf-1");

        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(chat);
        // Cross-kind workflow lookup finds the row already written server-side by createProjectWorkflow.
        when(chatArtifactRepository.findFirstByChatIdAndArtifactIdAndKindIn(eq(CHAT_ID), eq("wf-1"), any()))
            .thenReturn(Optional.of(existing));

        AiHubChatArtifact result = chatArtifactService.recordReference(
            CHAT_ID, WORKSPACE_ID, USER_ID, AiHubChatArtifactKind.WORKFLOW_REFERENCED, "wf-1",
            "Gmail to Slack", null);

        // The client-driven WORKFLOW_REFERENCED record must return the existing server-side WORKFLOW_CREATED
        // row rather than inserting a second sidebar entry for the same workflow.
        assertThat(result).isSameAs(existing);

        verify(chatArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceByThreadDedupsOnThreadKindArtifactId() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);
        AiHubChatArtifact existing = buildArtifact(500L, "SKILL_REFERENCED", "5");

        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.of(chat));
        when(
            chatArtifactRepository.findFirstByChatIdAndKindAndArtifactId(
                CHAT_ID, AiHubChatArtifactKind.SKILL_REFERENCED.ordinal(), "5"))
                    .thenReturn(Optional.empty())
                    .thenReturn(Optional.of(existing));
        when(chatArtifactRepository.save(any())).thenReturn(existing);

        AiHubChatArtifact firstResult = chatArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.SKILL_REFERENCED, "5", "My Skill");
        AiHubChatArtifact secondResult = chatArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        // The second call must collapse onto the row saved by the first call rather than inserting a duplicate.
        assertThat(firstResult).isSameAs(existing);
        assertThat(secondResult).isSameAs(existing);
        verify(chatArtifactRepository, times(1)).save(any());
    }

    @Test
    void testRecordReferenceByThreadReturnsNullWhenUserIdNull() {
        AiHubChatArtifact result = chatArtifactService.recordReferenceByThread(
            THREAD_ID, null, AiHubChatArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        assertThat(result).isNull();

        verify(chatService, never()).findByThreadIdViewable(anyString(), anyLong());
        verify(chatArtifactRepository, never()).save(any());
    }

    @Test
    void testRecordReferenceByThreadReturnsNullWhenChatNotFound() {
        when(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).thenReturn(Optional.empty());

        AiHubChatArtifact result = chatArtifactService.recordReferenceByThread(
            THREAD_ID, USER_ID, AiHubChatArtifactKind.SKILL_REFERENCED, "5", "My Skill");

        assertThat(result).isNull();

        verify(chatArtifactRepository, never()).save(any());
    }

    @Test
    void testDeleteReferenceRemovesWorkflowReferencedRow() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);
        AiHubChatArtifact referenced = buildArtifact(600L, "WORKFLOW_REFERENCED", "wf-1");

        when(chatArtifactRepository.findById(600L)).thenReturn(Optional.of(referenced));
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(chat);

        chatArtifactService.deleteReference(600L, WORKSPACE_ID, USER_ID);

        verify(chatArtifactRepository).deleteById(600L);
    }

    @Test
    void testDeleteReferenceRemovesAiAgentReferencedRow() {
        AiHubChat chat = buildChat(CHAT_ID, USER_ID, THREAD_ID);
        AiHubChatArtifact referenced = buildArtifact(604L, "AI_AGENT_REFERENCED", "42");

        when(chatArtifactRepository.findById(604L)).thenReturn(Optional.of(referenced));
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID)).thenReturn(chat);

        chatArtifactService.deleteReference(604L, WORKSPACE_ID, USER_ID);

        verify(chatArtifactRepository).deleteById(604L);
    }

    @Test
    void testDeleteReferenceIsIdempotentWhenRowIsGone() {
        when(chatArtifactRepository.findById(600L)).thenReturn(Optional.empty());

        chatArtifactService.deleteReference(600L, WORKSPACE_ID, USER_ID);

        verify(chatArtifactRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteReferenceRefusesNonReferenceKinds() {
        AiHubChatArtifact created = buildArtifact(601L, "WORKFLOW_CREATED", "wf-1");

        when(chatArtifactRepository.findById(601L)).thenReturn(Optional.of(created));
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID))
            .thenReturn(buildChat(CHAT_ID, USER_ID, THREAD_ID));

        assertThatThrownBy(() -> chatArtifactService.deleteReference(601L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("WORKFLOW_CREATED");

        verify(chatArtifactRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteReferenceRefusesReferenceKindsOtherThanWorkflowAndAiAgent() {
        AiHubChatArtifact referenced = buildArtifact(602L, "FILE_REFERENCED", "f-1");

        when(chatArtifactRepository.findById(602L)).thenReturn(Optional.of(referenced));
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, USER_ID))
            .thenReturn(buildChat(CHAT_ID, USER_ID, THREAD_ID));

        assertThatThrownBy(() -> chatArtifactService.deleteReference(602L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(IllegalArgumentException.class);

        verify(chatArtifactRepository, never()).deleteById(anyLong());
    }

    @Test
    void testDeleteReferenceThrowsWhenChatNotViewable() {
        AiHubChatArtifact referenced = buildArtifact(603L, "WORKFLOW_REFERENCED", "wf-1");

        when(chatArtifactRepository.findById(603L)).thenReturn(Optional.of(referenced));
        when(chatService.getViewable(CHAT_ID, WORKSPACE_ID, OTHER_USER_ID))
            .thenThrow(new NotFoundException("AiHubChat not found"));

        assertThatThrownBy(() -> chatArtifactService.deleteReference(603L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class);

        verify(chatArtifactRepository, never()).deleteById(anyLong());
    }

    private AiHubChat buildChat(long id, long userId, String threadId) {
        AiHubChat chat = new AiHubChat();

        chat.setId(id);
        chat.setUserId(userId);
        chat.setThreadId(threadId);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        // Fixtures live in WORKSPACE_ID; the ownership check compares the requester's workspace against this column.
        chat.setWorkspaceId(WORKSPACE_ID);

        return chat;
    }

    private AiHubChatArtifact buildArtifact(long id, String kind, String artifactId) {
        AiHubChatArtifact artifact = new AiHubChatArtifact();

        artifact.setId(id);
        artifact.setChatId(CHAT_ID);
        artifact.setKind(AiHubChatArtifactKind.valueOf(kind));
        artifact.setArtifactId(artifactId);
        artifact.setArtifactName("Artifact " + artifactId);

        return artifact;
    }
}
