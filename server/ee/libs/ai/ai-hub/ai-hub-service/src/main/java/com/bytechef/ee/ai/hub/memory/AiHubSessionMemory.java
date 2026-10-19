/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.memory;

import com.bytechef.platform.component.definition.ai.agent.ChatMemoryFunction;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Objects;
import java.util.Optional;
import java.util.UUID;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.memory.ChatMemory;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.session.DefaultSessionService;
import org.springframework.ai.session.EventFilter;
import org.springframework.ai.session.SessionEvent;
import org.springframework.ai.session.SessionRepository;
import org.springframework.ai.session.SessionService;
import org.springframework.ai.session.advisor.SessionMemoryAdvisor;

/**
 * Holder for the AI Hub's session-based conversation memory: the resolved {@link SessionRepository} (application
 * session backend — {@code bytechef.ai.memory.provider}, jdbc by default), a {@link SessionService} over it, and the
 * factory for the per-agent {@link SessionMemoryAdvisor}.
 *
 * <p>
 * Session memory persists the full tool request/response transcript, so the advisor is built with an
 * <b>inside-the-tool-loop</b> order ({@code ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER}, greater than
 * the tool-search advisor's) and the tool-search advisor's own in-loop conversation history is disabled (see
 * {@code PinnedToolSearchToolCallingAdvisor}) so the transcript is not written twice.
 * </p>
 *
 * <p>
 * Implements {@link AutoCloseable} so Spring disposes the owning client (Redis/S3) on shutdown; the jdbc and in-memory
 * backends have no client to close.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSessionMemory implements AutoCloseable {

    /**
     * User id assigned to sessions the AI Hub creates. Conversation enumeration goes through {@code ai_hub_chat} rows
     * (threadIds), not session listing, so the value only needs to be stable.
     */
    public static final String SESSION_USER_ID = "ai-hub";

    /**
     * Advisor param carrying the AG-UI message id of the user message a run sends. The session memory advisor stores
     * that message under this id, so the client can name the stored row, and a run that sends again a message the
     * session still holds, as a reload does, does not store it again.
     */
    public static final String USER_MESSAGE_ID_PARAM = "ai_hub_user_message_id";

    private static final Logger log = LoggerFactory.getLogger(AiHubSessionMemory.class);

    private final SessionRepository sessionRepository;
    private final SessionService sessionService;
    private final @Nullable AutoCloseable closeable;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubSessionMemory(SessionRepository sessionRepository, @Nullable AutoCloseable closeable) {
        this.sessionRepository = sessionRepository;
        this.sessionService = DefaultSessionService.builder()
            .sessionRepository(sessionRepository)
            .build();
        this.closeable = closeable;
    }

    /**
     * Builds the per-agent memory advisor: session-backed, keyed by the {@code chat_memory_conversation_id} advisor
     * param (the same literal as {@code ChatMemory.CONVERSATION_ID}, which {@code AiHubSpringAIAgent.advisorParams}
     * sets to the thread id), placed inside the tool-calling loop.
     */
    public SessionMemoryAdvisor createSessionMemoryAdvisor() {
        return SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(SESSION_USER_ID)
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER)
            .requestEventIdGenerator(this::requestEventId)
            .build();
    }

    /**
     * Builds the advisor that keeps a re-sent user message out of the prompt, since the session memory advisor already
     * puts the stored copy in it. Ordered right before the session memory advisor.
     */
    public ResentUserMessageAdvisor createResentUserMessageAdvisor() {
        return new ResentUserMessageAdvisor(this, ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER - 1);
    }

    /**
     * The id the session memory advisor stores a request message under. For the user message it is the AG-UI message id
     * the run carries in {@link #USER_MESSAGE_ID_PARAM}, when that id is a UUID and the session holds no event with it,
     * or holds this very message under it (a re-send, which the store then skips). Otherwise, and for every tool
     * response, it is a random UUID, so a message whose id was truncated away or names a different message is stored as
     * a new row. Accepting only a UUID keeps a client-chosen id as unlikely to collide with another session's event as
     * the random ids the store assigns otherwise.
     */
    String requestEventId(ChatClientRequest request, Message message) {
        String userMessageId = userMessageId(request);

        if (message.getMessageType() == MessageType.USER && userMessageId != null) {
            Optional<SessionEvent> storedEvent = findStoredEvent(request, userMessageId);

            if (storedEvent.isEmpty() || isStoredCopy(storedEvent.get(), message)) {
                return userMessageId;
            }
        }

        return UUID.randomUUID()
            .toString();
    }

    /**
     * Whether {@code message} is the run's user message sent again while the session still holds it, active and with
     * the same text, under the run's AG-UI message id.
     */
    boolean isResentUserMessage(ChatClientRequest request, Message message) {
        String userMessageId = userMessageId(request);

        if (message.getMessageType() != MessageType.USER || userMessageId == null) {
            return false;
        }

        return findStoredEvent(request, userMessageId).filter(storedEvent -> isStoredCopy(storedEvent, message))
            .isPresent();
    }

    private Optional<SessionEvent> findStoredEvent(ChatClientRequest request, String eventId) {
        if (!(request.context()
            .get(ChatMemory.CONVERSATION_ID) instanceof String sessionId)) {

            return Optional.empty();
        }

        return sessionService.getEvents(sessionId, EventFilter.all())
            .stream()
            .filter(event -> eventId.equals(event.getId()))
            .findFirst();
    }

    private static boolean isStoredCopy(SessionEvent storedEvent, Message message) {
        return !storedEvent.isArchived() && storedEvent.getMessageType() == MessageType.USER
            && Objects.equals(storedEvent.getMessage()
                .getText(), message.getText());
    }

    private static @Nullable String userMessageId(ChatClientRequest request) {
        return request.context()
            .get(USER_MESSAGE_ID_PARAM) instanceof String userMessageId && isUuid(userMessageId) ? userMessageId : null;
    }

    private static boolean isUuid(String value) {
        try {
            return UUID.fromString(value)
                .toString()
                .equals(value);
        } catch (IllegalArgumentException exception) {
            return false;
        }
    }

    /**
     * Builds the advisor that keeps the result of a tool call that ended the turn (a picker, a question or a call held
     * for approval) in the chat history for display, outside the tool-calling loop.
     */
    public AiHubTurnEndingToolResultAdvisor createTurnEndingToolResultAdvisor() {
        return new AiHubTurnEndingToolResultAdvisor(sessionService);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public SessionRepository sessionRepository() {
        return sessionRepository;
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public SessionService sessionService() {
        return sessionService;
    }

    @Override
    public void close() {
        if (closeable != null) {
            try {
                closeable.close();
            } catch (Exception exception) {
                log.warn("Failed to close AI Hub session memory client", exception);
            }
        }
    }
}
