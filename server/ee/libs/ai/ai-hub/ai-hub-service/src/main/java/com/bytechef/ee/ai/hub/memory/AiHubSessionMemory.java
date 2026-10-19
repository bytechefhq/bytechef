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
 * @version ee
 *
 * @author Ivica Cardic
 */
public class AiHubSessionMemory implements AutoCloseable {

    public static final String SESSION_USER_ID = "ai-hub";

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

    public SessionMemoryAdvisor createSessionMemoryAdvisor() {
        return SessionMemoryAdvisor.builder(sessionService)
            .defaultUserId(SESSION_USER_ID)
            .order(ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER)
            .requestEventIdGenerator(this::requestEventId)
            .build();
    }

    public ResentUserMessageAdvisor createResentUserMessageAdvisor() {
        return new ResentUserMessageAdvisor(this, ChatMemoryFunction.TOOL_MESSAGE_PERSISTENCE_ADVISOR_ORDER - 1);
    }

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
