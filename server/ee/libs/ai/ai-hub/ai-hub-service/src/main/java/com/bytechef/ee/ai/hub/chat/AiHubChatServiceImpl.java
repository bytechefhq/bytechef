/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.commons.util.JsonUtils;
import com.bytechef.ee.ai.hub.agent.InFlightAiHubRunRegistry;
import com.bytechef.ee.ai.hub.approval.AiHubToolApprovalService;
import com.bytechef.ee.ai.hub.chat.repository.AiHubChatRepository;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.ConflictException;
import com.bytechef.ee.ai.hub.exception.NotFoundException;
import com.bytechef.ee.ai.hub.memory.AiHubSessionMemory;
import com.bytechef.ee.ai.hub.memory.AiHubTurnEndingToolResultAdvisor;
import com.bytechef.ee.ai.hub.util.EnumOrdinals;
import com.bytechef.platform.configuration.domain.Environment;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.OptionalInt;
import java.util.Set;
import java.util.function.BiPredicate;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.compaction.CompactionPlan;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

/**
 * Default implementation of {@link AiHubChatService}.
 *
 * <p>
 * Message contents are stored and owned by Spring AI's session store, keyed by {@code AiHubChat.threadId} as the
 * session id. This service reaches them through {@code AiHubSessionMemory}: reads via {@code SessionService}, and the
 * few write paths ({@code appendAssistantMessage}, {@code truncateMessages}, delete cleanup) via
 * {@code SessionRepository}. The agent's own turns are written by the session memory advisor, not here.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
@Transactional
public class AiHubChatServiceImpl implements AiHubChatService {

    private static final Logger log = LoggerFactory.getLogger(AiHubChatServiceImpl.class);

    private static final int LIST_LIMIT = 100;

    /**
     * Hard upper bound on the number of messages returned by {@link #loadMessages}. Without this cap a long-running
     * chat could accumulate thousands of messages and force the controller to materialise them all in memory; the UI
     * never needs more than the most recent slice.
     */
    private static final int MESSAGE_LIMIT = 500;

    private static final String TURN_IN_FLIGHT_MESSAGE = "A turn is already running in this chat.";

    private final AiHubChatRepository chatRepository;
    private final AiHubChatAccessPolicy accessPolicy;
    private final AiHubToolApprovalService approvalService;
    private final Clock clock;
    private final ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider;
    private final InFlightAiHubRunRegistry inFlightRunRegistry;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubChatServiceImpl(
        AiHubChatRepository chatRepository, AiHubChatAccessPolicy accessPolicy,
        AiHubToolApprovalService approvalService, ObjectProvider<AiHubSessionMemory> aiHubSessionMemoryProvider,
        InFlightAiHubRunRegistry inFlightRunRegistry) {

        this.chatRepository = chatRepository;
        this.accessPolicy = accessPolicy;
        this.approvalService = approvalService;
        this.clock = Clock.systemUTC();
        this.aiHubSessionMemoryProvider = aiHubSessionMemoryProvider;
        this.inFlightRunRegistry = inFlightRunRegistry;
    }

    @Override
    @Transactional(readOnly = true)
    public long getWorkspaceId(long chatId) {
        return chatRepository.findById(chatId)
            .map(AiHubChat::getWorkspaceId)
            .orElseThrow(() -> new NotFoundException("No workspace for ai_hub_chat id=" + chatId));
    }

    @Override
    @Transactional(readOnly = true)
    public String getThreadId(long chatId) {
        return chatRepository.findById(chatId)
            .orElseThrow(() -> new NotFoundException("No ai_hub_chat row for id=" + chatId))
            .getThreadId();
    }

    @Override
    public AiHubChat create(long workspaceId, long userId, int environment, String threadId) {
        Optional<AiHubChat> existing = chatRepository.findByThreadId(threadId);

        if (existing.isPresent()) {
            AiHubChat found = existing.get();

            if (found.getUserId() == userId && workspaceOwnsChat(workspaceId, found)) {
                return found;
            }

            throw new ConflictException("AiHubChat %s already exists".formatted(threadId));
        }

        LocalDateTime now = LocalDateTime.now(clock);

        AiHubChat chat = new AiHubChat(userId);

        chat.setThreadId(threadId);
        chat.setStatus(AiHubChatStatus.ACTIVE);
        chat.setMessageCount(0);
        chat.setEnvironment(EnumOrdinals.fromOrdinal(environment, Environment.class));
        chat.setCreatedAt(now);
        chat.setUpdatedAt(now);
        chat.setWorkspaceId(workspaceId);

        try {
            return chatRepository.save(chat);
        } catch (DataIntegrityViolationException exception) {
            throw new ConflictException("AiHubChat %s already exists".formatted(threadId), exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubChat> list(long workspaceId, long userId, int environment, @Nullable AiHubChatStatus status) {
        if (status == null) {
            return chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentOrderByUpdatedAtDesc(
                workspaceId, userId, environment, LIST_LIMIT);
        }

        return chatRepository.findByWorkspaceIdAndUserIdAndEnvironmentAndStatusOrderByUpdatedAtDesc(
            workspaceId, userId, environment, status.ordinal(), LIST_LIMIT);
    }

    @Override
    @Transactional(readOnly = true)
    public List<AiHubChatMessage> loadMessages(long chatId, long requesterWorkspaceId, long requesterUserId) {
        AiHubChat chat = loadViewable(chatId, requesterWorkspaceId, requesterUserId);

        AiHubSessionMemory sessionMemory = aiHubSessionMemoryProvider.getIfAvailable();

        if (sessionMemory == null) {
            return List.of();
        }

        List<SessionEvent> allEvents = withDisplayOnlyToolResults(
            sessionMemory.sessionService()
                .getEvents(chat.getThreadId(), EventFilter.all()));

        List<SessionEvent> visibleEvents = new ArrayList<>();
        List<List<Map<String, String>>> toolEventsPerRow = new ArrayList<>();
        List<Map<String, String>> leadingToolEvents = new ArrayList<>();

        for (SessionEvent event : allEvents) {
            if (isVisibleConversationEvent(event)) {
                visibleEvents.add(event);

                List<Map<String, String>> ownToolEvents = new ArrayList<>();

                appendToolEvents(ownToolEvents, event);

                toolEventsPerRow.add(ownToolEvents);
            } else {
                appendToolEvents(
                    visibleEvents.isEmpty() ? leadingToolEvents : toolEventsPerRow.get(visibleEvents.size() - 1),
                    event);
            }
        }

        int firstKeptIndex = Math.max(0, visibleEvents.size() - MESSAGE_LIMIT);

        if (firstKeptIndex == 0 && !leadingToolEvents.isEmpty() && !toolEventsPerRow.isEmpty()) {
            toolEventsPerRow.get(0)
                .addAll(0, leadingToolEvents);
        }

        List<AiHubChatMessage> messages = new ArrayList<>();

        for (int i = firstKeptIndex; i < visibleEvents.size(); i++) {
            SessionEvent event = visibleEvents.get(i);
            List<Map<String, String>> toolEvents = toolEventsPerRow.get(i);

            messages.add(new AiHubChatMessage(
                event.getId(),
                event.getMessageType()
                    .name(),
                event.getMessage()
                    .getText(),
                event.getTimestamp(),
                toolEvents.isEmpty() ? null : JsonUtils.write(toolEvents)));
        }

        return messages;
    }

    /**
     * Returns the active events with each display-only turn-ending tool result kept in place, when every response in it
     * answers a still-open call of the preceding active tool calls by a non-blank id. Each call is answered at most
     * once, in order.
     */
    private static List<SessionEvent> withDisplayOnlyToolResults(List<SessionEvent> events) {
        List<SessionEvent> mergedEvents = new ArrayList<>(events.size());
        Map<String, Integer> openToolCallCounts = new HashMap<>();

        for (SessionEvent event : events) {
            Message message = event.getMessage();

            if (!event.isArchived()) {
                mergedEvents.add(event);

                if (message instanceof AssistantMessage assistantMessage) {
                    for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                        if (toolCall.id() != null && !toolCall.id()
                            .isBlank()) {

                            openToolCallCounts.merge(toolCall.id(), 1, Integer::sum);
                        }
                    }
                } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                    toolResponseMessage.getResponses()
                        .forEach(toolResponse -> closeToolCall(openToolCallCounts, toolResponse.id()));
                }

                continue;
            }

            if (!AiHubTurnEndingToolResultAdvisor.isDisplayOnly(event)
                || !(message instanceof ToolResponseMessage toolResponseMessage)) {

                continue;
            }

            List<ToolResponseMessage.ToolResponse> toolResponses = toolResponseMessage.getResponses();

            boolean answersOpenToolCalls = !toolResponses.isEmpty() && toolResponses.stream()
                .allMatch(toolResponse -> toolResponse.id() != null && !toolResponse.id()
                    .isBlank() && openToolCallCounts.getOrDefault(toolResponse.id(), 0) > 0);

            if (answersOpenToolCalls) {
                mergedEvents.add(event);

                toolResponses.forEach(toolResponse -> closeToolCall(openToolCallCounts, toolResponse.id()));
            }
        }

        return mergedEvents;
    }

    private static void closeToolCall(Map<String, Integer> openToolCallCounts, @Nullable String toolCallId) {
        if (toolCallId != null) {
            openToolCallCounts.computeIfPresent(toolCallId, (id, count) -> count > 1 ? count - 1 : null);
        }
    }

    private static void appendToolEvents(List<Map<String, String>> target, SessionEvent event) {
        Message message = event.getMessage();

        if (message instanceof AssistantMessage assistantMessage) {
            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                target.add(Map.of(
                    "arguments", Objects.requireNonNullElse(toolCall.arguments(), ""),
                    "id", Objects.requireNonNullElse(toolCall.id(), ""),
                    "kind", "call",
                    "name", Objects.requireNonNullElse(toolCall.name(), "")));
            }
        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                target.add(Map.of(
                    "id", Objects.requireNonNullElse(toolResponse.id(), ""),
                    "kind", "result",
                    "name", Objects.requireNonNullElse(toolResponse.name(), ""),
                    "response", Objects.requireNonNullElse(toolResponse.responseData(), "")));
            }
        }
    }

    private static boolean isVisibleConversationEvent(SessionEvent event) {
        Message message = event.getMessage();

        if (message == null
            || (event.getMessageType() != MessageType.USER && event.getMessageType() != MessageType.ASSISTANT)) {

            return false;
        }

        String text = message.getText();

        return text != null && !text.isBlank();
    }

    @Override
    public AiHubChat patch(long chatId, long requesterWorkspaceId, long requesterUserId, AiHubChatPatch chatPatch) {
        AiHubChat chat = loadManageable(chatId, requesterWorkspaceId, requesterUserId);

        if (chatPatch.title() != null) {
            chat.setTitle(chatPatch.title());
            chat.setAutoTitled(false);
        }

        if (chatPatch.lastPreview() != null) {
            chat.setLastPreview(chatPatch.lastPreview());
        }

        Integer messageCount = chatPatch.messageCount();

        if (messageCount != null) {
            chat.setMessageCount(messageCount);
        }

        if (chatPatch.status() != null) {
            chat.setStatus(chatPatch.status());
        }

        chat.setUpdatedAt(LocalDateTime.now(clock));

        return chatRepository.save(chat);
    }

    @Override
    public OptionalInt truncateMessages(
        long chatId, long requesterWorkspaceId, long requesterUserId, String messageId, boolean keepMessage) {

        AiHubChat chat = loadParticipable(chatId, requesterWorkspaceId, requesterUserId);

        String threadId = chat.getThreadId();

        if (inFlightRunRegistry.isInFlight(threadId)) {
            throw new ConflictException(TURN_IN_FLIGHT_MESSAGE);
        }

        AiHubSessionMemory sessionMemory = aiHubSessionMemoryProvider.getIfAvailable();

        if (sessionMemory == null) {
            return OptionalInt.empty();
        }

        long eventVersion = sessionMemory.sessionRepository()
            .getEventVersion(threadId);

        List<SessionEvent> activeEvents = sessionMemory.sessionService()
            .getEvents(threadId, EventFilter.active());

        int messageEventIndex = -1;

        for (int index = 0; index < activeEvents.size(); index++) {
            SessionEvent event = activeEvents.get(index);

            if (messageId.equals(event.getId()) && isVisibleConversationEvent(event)) {
                messageEventIndex = index;

                break;
            }
        }

        if (messageEventIndex < 0) {
            return OptionalInt.empty();
        }

        int cutoffEventIndex = keepMessage ? messageEventIndex + 1 : messageEventIndex;

        if (cutoffEventIndex >= activeEvents.size()) {
            return OptionalInt.of(0);
        }

        Set<String> truncatedEventIds = new HashSet<>();

        for (SessionEvent event : activeEvents.subList(cutoffEventIndex, activeEvents.size())) {
            truncatedEventIds.add(event.getId());
        }

        boolean truncated = sessionMemory.sessionRepository()
            .applyCompaction(threadId, new CompactionPlan(truncatedEventIds, List.of()), eventVersion);

        if (!truncated) {
            throw new ConflictException("The conversation changed while truncating; retry");
        }

        chat.setUpdatedAt(LocalDateTime.now(clock));
        chatRepository.save(chat);

        return OptionalInt.of(activeEvents.size() - cutoffEventIndex);
    }

    @Override
    public void appendAssistantMessage(long chatId, long requesterWorkspaceId, long requesterUserId, String content) {
        AiHubChat chat = loadParticipable(chatId, requesterWorkspaceId, requesterUserId);

        if (content == null || content.isBlank()) {
            return;
        }

        AiHubSessionMemory sessionMemory = aiHubSessionMemoryProvider.getIfAvailable();

        if (sessionMemory == null) {
            return;
        }

        sessionMemory.sessionRepository()
            .appendEvent(
                SessionEvent.builder()
                    .sessionId(chat.getThreadId())
                    .message(new AssistantMessage(content))
                    .build());

        chat.setUpdatedAt(LocalDateTime.now(clock));
        chatRepository.save(chat);
    }

    @Override
    @Transactional(readOnly = true)
    public boolean cancelAiHubRun(
        long chatId, long requesterWorkspaceId, long requesterUserId, @Nullable String runId) {

        AiHubChat chat = loadParticipable(chatId, requesterWorkspaceId, requesterUserId);

        return inFlightRunRegistry.cancel(chat.getThreadId(), runId);
    }

    @Override
    public void delete(long chatId, long requesterWorkspaceId, long requesterUserId) {
        AiHubChat chat = loadManageable(chatId, requesterWorkspaceId, requesterUserId);

        approvalService.deleteByChat(chatId);

        chatRepository.delete(chat);

        scheduleChatMemoryDeleteAfterCommit(chat.getThreadId());
    }

    private void scheduleChatMemoryDeleteAfterCommit(String threadId) {
        if (!TransactionSynchronizationManager.isSynchronizationActive()) {
            deleteSessionMessages(threadId);

            return;
        }

        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {

            @Override
            public void afterCommit() {
                deleteSessionMessages(threadId);
            }
        });
    }

    private void deleteSessionMessages(String threadId) {
        AiHubSessionMemory sessionMemory = aiHubSessionMemoryProvider.getIfAvailable();

        if (sessionMemory == null) {
            return;
        }

        try {
            sessionMemory.sessionService()
                .delete(threadId);
        } catch (RuntimeException exception) {
            log.warn(
                "Failed to delete session messages for sessionId {}; leaving orphan messages for background cleanup",
                threadId, exception);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubChat> findByThreadId(String threadId) {
        return chatRepository.findByThreadId(threadId);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<AiHubChat> findByThreadIdViewable(String threadId, long requesterUserId) {
        return chatRepository.findByThreadId(threadId)
            .filter(chat -> accessPolicy.canView(chat, requesterUserId));
    }

    @Override
    @Transactional(readOnly = true)
    public Map<String, AiHubChat> findAllByThreadIdViewable(Collection<String> threadIds, long requesterUserId) {
        if (threadIds.isEmpty()) {
            return Map.of();
        }

        Map<String, AiHubChat> chatByThreadId = new LinkedHashMap<>();

        for (AiHubChat chat : chatRepository.findAllByThreadIdIn(threadIds)) {
            if (accessPolicy.canView(chat, requesterUserId)) {
                chatByThreadId.put(chat.getThreadId(), chat);
            }
        }

        return chatByThreadId;
    }

    @Override
    @Transactional(readOnly = true)
    public AiHubChat getById(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return loadViewable(chatId, requesterWorkspaceId, requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiHubChat getViewable(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return loadViewable(chatId, requesterWorkspaceId, requesterUserId);
    }

    @Override
    @Transactional(readOnly = true)
    public AiHubChat getManageable(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return loadManageable(chatId, requesterWorkspaceId, requesterUserId);
    }

    private static boolean workspaceOwnsChat(long workspaceId, AiHubChat chat) {
        return Objects.equals(chat.getWorkspaceId(), workspaceId);
    }

    private AiHubChat loadViewable(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return load(chatId, requesterWorkspaceId, requesterUserId, accessPolicy::canView);
    }

    private AiHubChat loadParticipable(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return load(chatId, requesterWorkspaceId, requesterUserId, accessPolicy::canParticipate);
    }

    private AiHubChat loadManageable(long chatId, long requesterWorkspaceId, long requesterUserId) {
        return load(chatId, requesterWorkspaceId, requesterUserId, accessPolicy::canManage);
    }

    /**
     * Loads {@code chatId}, verifies it lives in {@code requesterWorkspaceId}, and verifies {@code allowed} accepts the
     * requester. "Does not exist", "exists in another workspace" and "not permitted for this requester" all return 404
     * with an opaque message so an authenticated attacker cannot enumerate chat ids across the install; the WARN log
     * keeps the audit trail.
     */
    private AiHubChat load(
        long chatId, long requesterWorkspaceId, long requesterUserId, BiPredicate<AiHubChat, Long> allowed) {

        AiHubChat chat = chatRepository.findById(chatId)
            .orElseThrow(() -> new NotFoundException("AiHubChat not found"));

        if (!workspaceOwnsChat(requesterWorkspaceId, chat) || !allowed.test(chat, requesterUserId)) {
            log.warn(
                "AiHubChat access denied: requester userId={} workspaceId={} attempted to access chatId={} owned by "
                    + "userId={}. Returning 404 to avoid leaking existence.",
                requesterUserId, requesterWorkspaceId, chatId, chat.getUserId());

            throw new NotFoundException("AiHubChat not found");
        }

        return chat;
    }
}
