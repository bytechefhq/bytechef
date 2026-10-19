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
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.agui.server.LocalAgent;
import com.bytechef.ee.ai.hub.agent.AiHubChatStreamer;
import com.bytechef.ee.ai.hub.agent.AiHubContinuationStarterImpl;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubContinuationStarter;
import com.bytechef.ee.ai.hub.approval.AiHubToolApproval;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatPatch;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.platform.security.constant.AuthorityConstants;
import java.time.Instant;
import java.time.LocalDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.stream.IntStream;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.TransactionDefinition;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;
import tools.jackson.databind.json.JsonMapper;

/**
 * Unit tests for {@link AiHubChatServiceImpl}.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@ExtendWith(MockitoExtension.class)
class AiHubChatServiceTest {

    private static final int DELETED_ORDINAL = AiHubChatStatus.DELETED.ordinal();

    private static final long WORKSPACE_ID = 1L;
    private static final long OTHER_WORKSPACE_ID = 2L;
    private static final long USER_ID = 10L;
    private static final long OTHER_USER_ID = 99L;
    private static final String THREAD_ID = "thread-abc";

    @Mock
    private AiHubChatRepository chatRepository;

    @Mock
    private AiHubToolApprovalService approvalService;

    @Mock
    private InFlightAiHubRunRegistry inFlightRunRegistry;

    @Mock
    private ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider;

    @Mock
    private AiHubSessionMemory aiHubSessionMemory;

    @Mock
    private SessionService sessionService;

    @Mock
    private SessionRepository sessionRepository;

    private AiHubChatServiceImpl chatService;

    @BeforeEach
    void setUp() {
        lenient().when(aiHubSessionMemoryProvider.getIfAvailable())
            .thenReturn(aiHubSessionMemory);
        lenient().when(aiHubSessionMemory.sessionService())
            .thenReturn(sessionService);
        lenient().when(aiHubSessionMemory.sessionRepository())
            .thenReturn(sessionRepository);

        chatService = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider,
            inFlightRunRegistry, mock(PlatformTransactionManager.class));
    }

    @AfterEach
    void clearSecurityContext() {
        SecurityContextHolder.clearContext();
    }

    private static SessionEvent sessionEvent(MessageType type, String text, Instant timestamp) {
        SessionEvent event = mock(SessionEvent.class);

        Message message = type == MessageType.USER ? new UserMessage(text) : new AssistantMessage(text);

        lenient().when(event.getId())
            .thenReturn("event-" + timestamp.toEpochMilli());
        lenient().when(event.getMessageType())
            .thenReturn(type);
        lenient().when(event.getMessage())
            .thenReturn(message);
        lenient().when(event.getTimestamp())
            .thenReturn(timestamp);

        return event;
    }

    private static AiHubChat buildChat(long id, long userId, String threadId, AiHubChatStatus status) {
        AiHubChat chat = new AiHubChat();

        chat.setId(id);
        chat.setUserId(userId);
        chat.setThreadId(threadId);
        chat.setStatus(status);
        chat.setMessageCount(0);
        chat.setCreatedAt(LocalDateTime.now());
        chat.setUpdatedAt(LocalDateTime.now());
        chat.setWorkspaceId(WORKSPACE_ID);

        return chat;
    }

    private static void authenticate(String login, String... authorities) {
        SecurityContextHolder.getContext()
            .setAuthentication(new UsernamePasswordAuthenticationToken(
                login, "n/a", Arrays.stream(authorities)
                    .map(SimpleGrantedAuthority::new)
                    .toList()));
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
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.loadMessages(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");
    }

    @Test
    void testLoadMessagesThrowsNotFoundOnWorkspaceMismatch() {
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

        List<SessionEvent> events =
            List.of(sessionEvent(MessageType.USER, "Hello", now));

        when(sessionService.getEvents(THREAD_ID, EventFilter.all())).thenReturn(events);

        List<AiHubChatMessage> messages = chatService.loadMessages(1L, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(1);
        assertThat(messages.get(0)
            .id()).isEqualTo("event-" + now.toEpochMilli());
        assertThat(messages.get(0)
            .role()).isEqualTo("USER");
        assertThat(messages.get(0)
            .content()).isEqualTo("Hello");
    }

    @Test
    void testLoadMessagesKeepsTheLatestMessagesInConversationOrderWhenTheChatExceedsTheLimit() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        Instant start = Instant.parse("2026-04-23T10:00:00Z");

        List<SessionEvent> events = IntStream.range(0, 502)
            .mapToObj(index -> sessionEvent(
                index % 2 == 0 ? MessageType.USER : MessageType.ASSISTANT, "message " + index,
                start.plusSeconds(index)))
            .toList();

        when(sessionService.getEvents(THREAD_ID, EventFilter.all())).thenReturn(events);

        List<AiHubChatMessage> messages = chatService.loadMessages(1L, WORKSPACE_ID, USER_ID);

        assertThat(messages).hasSize(500);
        assertThat(messages.getFirst()
            .content()).isEqualTo("message 2");
        assertThat(messages.getLast()
            .content()).isEqualTo("message 501");
        assertThat(messages).extracting(AiHubChatMessage::timestamp)
            .isSorted();
    }

    @Test
    void testPatchAppliesNonNullFieldsOnly() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        chat.setTitle("Old title");
        chat.setMessageCount(3);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        when(chatRepository.updateTitleUnlessDeleted(anyLong(), any(), anyInt(), any())).thenReturn(1);

        AiHubChatPatch patch = new AiHubChatPatch("New title", null, null, null);

        AiHubChat result = chatService.patch(1L, WORKSPACE_ID, USER_ID, patch);

        assertThat(result.getTitle()).isEqualTo("New title");
        assertThat(result.getMessageCount()).isEqualTo(3);
        assertThat(result.getStatus()).isEqualTo(AiHubChatStatus.ACTIVE);

        verify(chatRepository).updateTitleUnlessDeleted(eq(1L), eq("New title"), eq(DELETED_ORDINAL),
            any(LocalDateTime.class));
        verify(chatRepository, never()).updateLastPreviewUnlessDeleted(anyLong(), any(), anyInt(), any());
        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
        verify(chatRepository, never()).updateStatusUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testPatchUpdatesPreviewAndMessageCountWithoutTouchingTheTitle() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        when(chatRepository.updateLastPreviewUnlessDeleted(anyLong(), any(), anyInt(), any())).thenReturn(1);
        when(chatRepository.updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any())).thenReturn(1);

        chatService.patch(1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, "Latest preview", 4, null));

        verify(chatRepository).updateLastPreviewUnlessDeleted(
            eq(1L), eq("Latest preview"), eq(DELETED_ORDINAL), any(LocalDateTime.class));
        verify(chatRepository).updateMessageCountUnlessDeleted(eq(1L), eq(4), eq(DELETED_ORDINAL),
            any(LocalDateTime.class));
        verify(chatRepository, never()).updateTitleUnlessDeleted(anyLong(), any(), anyInt(), any());
        verify(chatRepository, never()).updateStatusUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testPatchRejectsANegativeMessageCountBeforeWriting() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(
            () -> chatService.patch(1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, -1, null)))
                .isInstanceOf(IllegalArgumentException.class);

        verify(chatRepository, never()).updateMessageCountUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void testPatchWithNoFieldsCannotBeBuilt() {
        assertThatThrownBy(() -> new AiHubChatPatch(null, null, null, null))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("at least one");
    }

    @Test
    void testPatchTitleOfAChatDeletedConcurrentlyThrowsConflict() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(
            () -> chatService.patch(1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch("New title", null, null, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testPatchPreviewOfAChatDeletedConcurrentlyThrowsConflict() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.updateLastPreviewUnlessDeleted(
            eq(1L), eq("Latest preview"), eq(DELETED_ORDINAL), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(
            () -> chatService.patch(
                1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, "Latest preview", null, null)))
                    .isInstanceOf(ConflictException.class);
    }

    @Test
    void testPatchMessageCountOfAChatDeletedConcurrentlyThrowsConflict() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.updateMessageCountUnlessDeleted(
            eq(1L), eq(7), eq(DELETED_ORDINAL), any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(
            () -> chatService.patch(1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, 7, null)))
                .isInstanceOf(ConflictException.class);
    }

    @Test
    void testPatchOfAChatInDeletedStatusIsNotFoundBeforeWriting() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.DELETED);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(
            () -> chatService.patch(
                1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.ARCHIVED)))
                    .isInstanceOf(NotFoundException.class);

        verify(chatRepository, never()).updateStatusUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void testAChatInDeletedStatusCannotBeReadOrWritten() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.DELETED);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.findByThreadId(THREAD_ID)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.getViewable(1L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> chatService.loadMessages(1L, WORKSPACE_ID, USER_ID))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> chatService.appendAssistantMessage(1L, WORKSPACE_ID, USER_ID, "Hello"))
            .isInstanceOf(NotFoundException.class);
        assertThatThrownBy(() -> chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-1", true))
            .isInstanceOf(NotFoundException.class);
        assertThat(chatService.findByThreadIdViewable(THREAD_ID, USER_ID)).isEmpty();

        verify(chatRepository, never()).updateUpdatedAtUnlessDeleted(anyLong(), anyInt(), any());
    }

    @Test
    void testAChatInDeletedStatusCanStillBeHardDeleted() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.DELETED);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(chatRepository).delete(chat);
    }

    @Test
    void testListWithoutAStatusExcludesDeletedChatsAndListingDeletedReturnsNothing() {
        List<AiHubChat> chats = List.of(buildChat(1L, USER_ID, "t-1", AiHubChatStatus.ACTIVE));

        when(chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusNotOrderByUpdatedAtDesc(
            eq(WORKSPACE_ID), eq(USER_ID), eq(0), eq(DELETED_ORDINAL), anyInt()))
                .thenReturn(chats);

        assertThat(chatService.list(WORKSPACE_ID, USER_ID, 0, null)).isEqualTo(chats);
        assertThat(chatService.list(WORKSPACE_ID, USER_ID, 0, AiHubChatStatus.DELETED)).isEmpty();
    }

    @Test
    void testPatchStatusOfAChatDeletedConcurrentlyThrowsConflict() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(chatRepository.updateStatusUnlessDeleted(
            eq(1L), eq(AiHubChatStatus.ARCHIVED.ordinal()), eq(AiHubChatStatus.DELETED.ordinal()),
            any(LocalDateTime.class)))
                .thenReturn(0);

        assertThatThrownBy(
            () -> chatService.patch(
                1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.ARCHIVED)))
                    .isInstanceOf(ConflictException.class);
    }

    @Test
    void testPatchStatusToDeletedIsRejectedBeforeAnyLookup() {
        assertThatThrownBy(
            () -> chatService.patch(
                1L, WORKSPACE_ID, USER_ID, new AiHubChatPatch(null, null, null, AiHubChatStatus.DELETED)))
                    .isInstanceOf(IllegalArgumentException.class);

        verify(chatRepository, never()).findById(anyLong());
        verify(chatRepository, never()).updateStatusUnlessDeleted(anyLong(), anyInt(), anyInt(), any());
    }

    @Test
    void testPatchThrowsNotFoundOnOwnershipMismatch() {
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

    @Test
    void testDeleteRemovesTheChatsToolApprovals() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(approvalService).deleteByChat(1L);
    }

    @Test
    void testDeleteStopsTheChatsInFlightRunBeforeRemovingItsApprovals() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        InFlightAiHubRunRegistry.InFlightRun run = registry.tryStart(THREAD_ID, "run-1")
            .orElseThrow();

        Mono<Void> stopRequested = registry.stopRequested(THREAD_ID);

        doAnswer(invocation -> {
            assertThat(run.isTerminated()).isTrue();

            return null;
        }).when(approvalService)
            .deleteByChat(1L);

        serviceWithRegistry.delete(1L, WORKSPACE_ID, USER_ID);

        StepVerifier.create(stopRequested)
            .verifyComplete();
        verify(approvalService).deleteByChat(1L);
        verify(chatRepository).delete(chat);
    }

    @Test
    void testDeleteThrowsNotFoundOnOwnershipMismatch() {
        AiHubChat chat =
            buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.delete(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");

        verify(approvalService, never()).deleteByChat(anyLong());
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

        assertThatThrownBy(() -> chatService.create(WORKSPACE_ID, USER_ID, 0, THREAD_ID))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining(THREAD_ID);

        verify(chatRepository, never()).save(any());
    }

    @Test
    void testTruncateMessagesDropsTheMessageAndEverythingAfterIt() {
        List<SessionEvent> events = givenActiveEvents();

        when(sessionRepository.applyCompaction(eq(THREAD_ID), any(), anyLong())).thenReturn(true);

        OptionalInt deleted = chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-200", false);

        assertThat(deleted).hasValue(2);

        verify(chatRepository).updateUpdatedAtUnlessDeleted(eq(1L), eq(DELETED_ORDINAL), any(LocalDateTime.class));
        verify(chatRepository, never()).save(any());
        assertThat(archivedEventIds()).containsExactlyInAnyOrder(events.get(1)
            .getId(),
            events.get(2)
                .getId());
    }

    @Test
    void testTruncateMessagesKeepingTheMessageDropsOnlyWhatFollowsIt() {
        List<SessionEvent> events = givenActiveEvents();

        when(sessionRepository.applyCompaction(eq(THREAD_ID), any(), anyLong())).thenReturn(true);

        OptionalInt deleted = chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-100", true);

        assertThat(deleted).hasValue(2);
        assertThat(archivedEventIds()).containsExactlyInAnyOrder(events.get(1)
            .getId(),
            events.get(2)
                .getId());
    }

    @Test
    void testTruncateMessagesKeepingTheLastMessageDropsNothing() {
        givenActiveEvents();

        OptionalInt deleted = chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-300", true);

        assertThat(deleted).hasValue(0);

        verify(sessionRepository, never()).applyCompaction(any(), any(), anyLong());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testTruncateMessagesIsEmptyForAMessageTheChatDoesNotHold() {
        givenActiveEvents();

        OptionalInt deleted = chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-unknown", false);

        assertThat(deleted).isEmpty();

        verify(sessionRepository, never()).applyCompaction(any(), any(), anyLong());
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testTruncateMessagesRefusesWhileATurnRunsBeforeDroppingAnything() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        when(inFlightRunRegistry.isInFlight(THREAD_ID)).thenReturn(true);

        assertThatThrownBy(() -> chatService.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-200", false))
            .isInstanceOf(ConflictException.class)
            .hasMessageContaining("A turn is already running");

        verify(sessionService, never()).getEvents(anyString(), any());
        verify(sessionRepository, never()).applyCompaction(any(), any(), anyLong());
    }

    @Test
    void testTruncateMessagesAfterAStopIsRefusedUntilTheStoppedRunEnds() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        givenActiveEvents();

        InFlightAiHubRunRegistry.InFlightRun run = registry.tryStart(THREAD_ID, "run-1")
            .orElseThrow();

        registry.cancel(THREAD_ID, "run-1");

        assertThatThrownBy(() -> serviceWithRegistry.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-300", true))
            .isInstanceOf(ConflictException.class);

        run.complete();

        assertThat(serviceWithRegistry.truncateMessages(1L, WORKSPACE_ID, USER_ID, "event-300", true)).hasValue(0);
    }

    @Test
    void testTruncateMessagesRefusesCrossUserAccess() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThatThrownBy(() -> chatService.truncateMessages(1L, WORKSPACE_ID, OTHER_USER_ID, "event-100", false))
            .isInstanceOf(NotFoundException.class);
    }

    private List<SessionEvent> givenActiveEvents() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        List<SessionEvent> events = List.of(
            sessionEvent(MessageType.USER, "one", Instant.ofEpochMilli(100)),
            sessionEvent(MessageType.ASSISTANT, "two", Instant.ofEpochMilli(200)),
            sessionEvent(MessageType.USER, "three", Instant.ofEpochMilli(300)));

        when(sessionService.getEvents(THREAD_ID, EventFilter.active())).thenReturn(events);

        return events;
    }

    private Set<String> archivedEventIds() {
        ArgumentCaptor<CompactionPlan> planCaptor = ArgumentCaptor.forClass(CompactionPlan.class);

        verify(sessionRepository).applyCompaction(eq(THREAD_ID), planCaptor.capture(), eq(0L));

        assertThat(planCaptor.getValue()
            .inserts()).isEmpty();

        return planCaptor.getValue()
            .archiveIds();
    }

    /**
     * Measures the repository round-trips the {@code /status} poll costs, at the actual database boundary rather than
     * at the service seam. Three thread ids resolve in ONE {@code findAllByThreadIdIn}, and the per-id
     * {@code findByThreadId} the old loop issued once per thread is not called at all — so a tick over N sidebar
     * threads is 1 query, not N.
     *
     * <p>
     * The companion assertion is the one that makes this a comparison rather than a claim: three calls to the
     * single-row {@code findByThreadIdViewable} still cost three {@code findByThreadId} queries, which is exactly what
     * the endpoint used to do per tick.
     * </p>
     */
    @Test
    void testFindAllByThreadIdViewableCostsOneQueryForManyThreads() {
        AiHubChat firstChat = buildChat(1L, USER_ID, "t-1", AiHubChatStatus.ACTIVE);
        AiHubChat secondChat = buildChat(2L, USER_ID, "t-2", AiHubChatStatus.ACTIVE);
        AiHubChat strangersChat = buildChat(3L, OTHER_USER_ID, "t-3", AiHubChatStatus.ACTIVE);

        List<String> threadIds = List.of("t-1", "t-2", "t-3");

        when(chatRepository.findAllByThreadIdIn(threadIds))
            .thenReturn(List.of(firstChat, secondChat, strangersChat));

        Map<String, AiHubChat> chatByThreadId = chatService.findAllByThreadIdViewable(threadIds, USER_ID);

        assertThat(chatByThreadId).containsOnlyKeys("t-1", "t-2");

        verify(chatRepository, times(1)).findAllByThreadIdIn(threadIds);
        verify(chatRepository, never()).findByThreadId(anyString());

        when(chatRepository.findByThreadId(anyString())).thenReturn(Optional.empty());

        for (String threadId : threadIds) {
            chatService.findByThreadIdViewable(threadId, USER_ID);
        }

        verify(chatRepository, times(3)).findByThreadId(anyString());
    }

    @Test
    void testFindAllByThreadIdViewableIssuesNoQueryForAnEmptyRequest() {
        assertThat(chatService.findAllByThreadIdViewable(List.of(), USER_ID)).isEmpty();

        verify(chatRepository, never()).findAllByThreadIdIn(any());
    }

    @Test
    void testOtherUsersChatIsNotViewable() {
        authenticate("bob", AuthorityConstants.USER);

        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));

        assertThatThrownBy(() -> chatService.getViewable(1L, WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class)
            .hasMessageContaining("AiHubChat not found");
    }

    @Test
    void testAdminCanViewAnyChat() {
        authenticate("root", AuthorityConstants.ADMIN);

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        assertThat(chatService.getViewable(1L, WORKSPACE_ID, OTHER_USER_ID)).isSameAs(chat);
        assertThat(chatService.getManageable(1L, WORKSPACE_ID, OTHER_USER_ID)).isSameAs(chat);
    }

    @Test
    void testAdminCannotReachAChatInAnotherWorkspace() {
        authenticate("root", AuthorityConstants.ADMIN);

        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));

        assertThatThrownBy(() -> chatService.getViewable(1L, OTHER_WORKSPACE_ID, OTHER_USER_ID))
            .isInstanceOf(NotFoundException.class);
    }

    @Test
    void testDeletingAChatPurgesItsSession() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        TransactionSynchronizationManager.initSynchronization();

        try {
            chatService.delete(1L, WORKSPACE_ID, USER_ID);

            verify(chatRepository).delete(chat);
            verify(sessionService, never()).delete(any());

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

            verify(sessionService).delete(THREAD_ID);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void testDeletingAChatRemovesApprovalsTheGateLeftAfterTheCancel() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        TransactionSynchronizationManager.initSynchronization();

        try {
            chatService.delete(1L, WORKSPACE_ID, USER_ID);

            verify(approvalService, times(1)).deleteByChat(1L);

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);

            verify(approvalService, times(2)).deleteByChat(1L);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }
    }

    @Test
    void testDeletingAChatNeverStartsItsQueuedApprovalContinuation() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        lenient().when(chatRepository.findByThreadId(THREAD_ID))
            .thenReturn(Optional.of(chat));

        LocalAgent askAgent = mock(LocalAgent.class);

        when(askAgent.getAgentId()).thenReturn("ai_hub_ask");

        AiHubContinuationStarter continuationStarter = new AiHubContinuationStarterImpl(
            new AiHubChatStreamer(registry, new JsonMapper()), registry, List.of(askAgent), serviceWithRegistry);

        InFlightAiHubRunRegistry.InFlightRun runningTurn = registry.tryStart(THREAD_ID, "run-running")
            .orElseThrow();

        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setMode("ASK");
        approval.setRequestedByUserId(USER_ID);

        assertThat(continuationStarter.startContinuation(approval, chat, WORKSPACE_ID, "[tool-approval #5]"))
            .isEmpty();

        serviceWithRegistry.delete(1L, WORKSPACE_ID, USER_ID);

        runningTurn.complete();

        verify(askAgent, never()).runAgent(any(), any());
        assertThat(registry.isInFlight(THREAD_ID)).isFalse();
    }

    @Test
    void testARolledBackDeleteLeavesTheChatsThreadUsable() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        TransactionSynchronizationManager.initSynchronization();

        try {
            serviceWithRegistry.delete(1L, WORKSPACE_ID, USER_ID);

            assertThat(registry.tryStart(THREAD_ID, "run-during-delete")).isEmpty();

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(registry.tryStart(THREAD_ID, "run-after-rollback")).isPresent();
        verify(sessionService, never()).delete(any());
    }

    @Test
    void testADeleteThatFailsAfterRetiringStillLiftsTheRetirementOnRollback() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));
        doThrow(new IllegalStateException("database down")).when(approvalService)
            .deleteByChat(1L);

        TransactionSynchronizationManager.initSynchronization();

        try {
            assertThatThrownBy(() -> serviceWithRegistry.delete(1L, WORKSPACE_ID, USER_ID))
                .isInstanceOf(IllegalStateException.class);

            assertThat(registry.isRetired(THREAD_ID)).isTrue();

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(synchronization -> synchronization.afterCompletion(
                    TransactionSynchronization.STATUS_ROLLED_BACK));
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(registry.isRetired(THREAD_ID)).isFalse();
        assertThat(registry.tryStart(THREAD_ID, "run-after-rollback")).isPresent();
    }

    @Test
    void testACommittedDeleteKeepsTheChatsThreadRetired() {
        InFlightAiHubRunRegistry registry = new InFlightAiHubRunRegistry();
        AiHubChatServiceImpl serviceWithRegistry = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider, registry,
            mock(PlatformTransactionManager.class));

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        TransactionSynchronizationManager.initSynchronization();

        try {
            serviceWithRegistry.delete(1L, WORKSPACE_ID, USER_ID);

            for (TransactionSynchronization synchronization : TransactionSynchronizationManager
                .getSynchronizations()) {

                synchronization.afterCommit();
                synchronization.afterCompletion(TransactionSynchronization.STATUS_COMMITTED);
            }
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        assertThat(registry.tryStart(THREAD_ID, "run-after-commit")).isEmpty();
    }

    @Test
    void testAfterCommitCleanupsRunInRequiresNewTransactions() {
        PlatformTransactionManager transactionManager = mock(PlatformTransactionManager.class);
        AiHubChatServiceImpl serviceWithTransactionManager = new AiHubChatServiceImpl(
            chatRepository, new AiHubChatAccessPolicyImpl(), approvalService, aiHubSessionMemoryProvider,
            inFlightRunRegistry, transactionManager);

        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        TransactionSynchronizationManager.initSynchronization();

        try {
            serviceWithTransactionManager.delete(1L, WORKSPACE_ID, USER_ID);

            verify(transactionManager, never()).getTransaction(any());

            TransactionSynchronizationManager.getSynchronizations()
                .forEach(TransactionSynchronization::afterCommit);
        } finally {
            TransactionSynchronizationManager.clearSynchronization();
        }

        ArgumentCaptor<TransactionDefinition> definitionCaptor = ArgumentCaptor.forClass(TransactionDefinition.class);

        verify(transactionManager, times(2)).getTransaction(definitionCaptor.capture());

        assertThat(definitionCaptor.getAllValues())
            .extracting(TransactionDefinition::getPropagationBehavior)
            .containsOnly(TransactionDefinition.PROPAGATION_REQUIRES_NEW);
        verify(sessionService).delete(THREAD_ID);
        verify(approvalService, times(2)).deleteByChat(1L);
    }

    @Test
    void testDeleteSucceedsWhenTheSessionPurgeFails() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        doThrow(new IllegalStateException("session store unavailable")).when(sessionService)
            .delete(THREAD_ID);

        chatService.delete(1L, WORKSPACE_ID, USER_ID);

        verify(chatRepository).delete(chat);
    }

    @Test
    void testAppendAssistantMessageAppendsToTheSession() {
        AiHubChat chat = buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE);

        when(chatRepository.findById(1L)).thenReturn(Optional.of(chat));

        chatService.appendAssistantMessage(1L, WORKSPACE_ID, USER_ID, "hello");

        verify(sessionRepository).appendEvent(any(SessionEvent.class));
        verify(chatRepository).updateUpdatedAtUnlessDeleted(eq(1L), eq(DELETED_ORDINAL), any(LocalDateTime.class));
        verify(chatRepository, never()).save(any());
    }

    @Test
    void testAppendAssistantMessageRefusesAnotherUser() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));

        assertThatThrownBy(() -> chatService.appendAssistantMessage(1L, WORKSPACE_ID, OTHER_USER_ID, "hello"))
            .isInstanceOf(NotFoundException.class);

        verify(sessionRepository, never()).appendEvent(any());
    }

    @Test
    void testCancelAiHubRunCancelsTheOwnersRunThroughTheRegistry() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));
        when(inFlightRunRegistry.cancel(THREAD_ID, "run-1")).thenReturn(true);

        assertThat(chatService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, "run-1")).isTrue();

        verify(inFlightRunRegistry).cancel(THREAD_ID, "run-1");
    }

    @Test
    void testCancelAiHubRunSupersedesThePendingApprovalsWhoseCallWasNeverRecorded() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));
        when(inFlightRunRegistry.cancel(THREAD_ID, "run-1")).thenReturn(true);

        AiHubToolApproval idlessPendingApproval = approval(AiHubToolApproval.Status.PENDING, null);
        AiHubToolApproval recordedPendingApproval = approval(AiHubToolApproval.Status.PENDING, "call-1");
        AiHubToolApproval idlessApprovedApproval = approval(AiHubToolApproval.Status.APPROVED, null);

        when(approvalService.list(1L))
            .thenReturn(List.of(idlessPendingApproval, recordedPendingApproval, idlessApprovedApproval));

        chatService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, "run-1");

        assertThat(idlessPendingApproval.getStatus()).isEqualTo(AiHubToolApproval.Status.SUPERSEDED);
        assertThat(recordedPendingApproval.getStatus()).isEqualTo(AiHubToolApproval.Status.PENDING);
        assertThat(idlessApprovedApproval.getStatus()).isEqualTo(AiHubToolApproval.Status.APPROVED);

        verify(approvalService).save(idlessPendingApproval);
        verify(approvalService, times(1)).save(any());
    }

    @Test
    void testCancelAiHubRunWithNoLiveRunLeavesApprovalsAlone() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));
        when(inFlightRunRegistry.cancel(THREAD_ID, "run-1")).thenReturn(false);

        chatService.cancelAiHubRun(1L, WORKSPACE_ID, USER_ID, "run-1");

        verify(approvalService, never()).list(anyLong());
    }

    private static AiHubToolApproval approval(AiHubToolApproval.Status status, @Nullable String toolCallId) {
        AiHubToolApproval approval = new AiHubToolApproval();

        approval.setStatus(status);
        approval.setToolCallId(toolCallId);
        approval.setToolName("deleteWorkflow");
        approval.setArguments("{}");

        return approval;
    }

    @Test
    void testCancelAiHubRunRefusesAnotherUser() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));

        assertThatThrownBy(() -> chatService.cancelAiHubRun(1L, WORKSPACE_ID, OTHER_USER_ID, "run-1"))
            .isInstanceOf(NotFoundException.class);

        verify(inFlightRunRegistry, never()).cancel(any(), any());
    }

    @Test
    void testCancelAiHubRunRefusesAChatInAnotherWorkspace() {
        when(chatRepository.findById(1L))
            .thenReturn(Optional.of(buildChat(1L, USER_ID, THREAD_ID, AiHubChatStatus.ACTIVE)));

        assertThatThrownBy(() -> chatService.cancelAiHubRun(1L, OTHER_WORKSPACE_ID, USER_ID, "run-1"))
            .isInstanceOf(NotFoundException.class);

        verify(inFlightRunRegistry, never()).cancel(any(), any());
    }
}
