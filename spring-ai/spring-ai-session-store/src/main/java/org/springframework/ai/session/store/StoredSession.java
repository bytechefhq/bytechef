/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.springframework.ai.session.store;

import com.fasterxml.jackson.annotation.JsonSetter;
import com.fasterxml.jackson.annotation.Nulls;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.session.Session;
import org.springframework.ai.session.SessionEvent;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * @author Ivica Cardic
 */
@SuppressFBWarnings({
    "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
})
public record StoredSession(
    String id, String userId, long createdAtEpochMilli, @Nullable Long expiresAtEpochMilli,
    Map<String, Object> metadata, long version, @JsonSetter(nulls = Nulls.AS_EMPTY) long revision,
    List<StoredEvent> events) {

    public static final int DEFAULT_MAX_ARCHIVED_EVENTS = 1000;

    public StoredSession {
        Objects.requireNonNull(id, "id must not be null");
        Objects.requireNonNull(userId, "userId must not be null");

        if (version < 0) {
            throw new IllegalArgumentException("version must not be negative");
        }

        if (revision < 0) {
            throw new IllegalArgumentException("revision must not be negative");
        }

        metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        events = events == null ? List.of() : List.copyOf(events);

        for (StoredEvent event : events) {
            if (!id.equals(event.sessionId())) {
                throw new IllegalArgumentException(
                    "Event session id " + event.sessionId() + " does not match the stored session id " + id);
            }
        }
    }

    public static StoredSession fromSession(Session session, long version, List<StoredEvent> events) {
        Instant expiresAt = session.expiresAt();

        return new StoredSession(
            session.id(), session.userId(), session.createdAt()
                .toEpochMilli(),
            expiresAt != null ? expiresAt.toEpochMilli() : null, session.metadata(), version, 0L, events);
    }

    public StoredSession withSession(Session session) {
        if (!id.equals(session.id())) {
            throw new IllegalArgumentException(
                "Session id " + session.id() + " does not match the stored session id " + id);
        }

        Instant expiresAt = session.expiresAt();

        return new StoredSession(
            id, session.userId(), createdAtEpochMilli, expiresAt != null ? expiresAt.toEpochMilli() : null,
            session.metadata(), version, revision + 1, events);
    }

    public StoredSession appendEvent(StoredEvent event, int maxArchivedEvents) {
        Objects.requireNonNull(event, "event must not be null");

        List<StoredEvent> appendedEvents = new ArrayList<>(events);

        appendedEvents.add(event);

        return new StoredSession(
            id, userId, createdAtEpochMilli, expiresAtEpochMilli, metadata, version + 1, revision + 1,
            pruneArchivedEvents(appendedEvents, maxArchivedEvents));
    }

    public StoredSession replaceEvents(List<StoredEvent> replacementEvents, int maxArchivedEvents) {
        Objects.requireNonNull(replacementEvents, "replacementEvents must not be null");

        return new StoredSession(
            id, userId, createdAtEpochMilli, expiresAtEpochMilli, metadata, version + 1, revision + 1,
            pruneArchivedEvents(replacementEvents, maxArchivedEvents));
    }

    public boolean isExpired(Instant before) {
        return expiresAtEpochMilli != null && expiresAtEpochMilli < before.toEpochMilli();
    }

    public boolean containsEvent(String eventId) {
        for (StoredEvent event : events) {
            if (event.id()
                .equals(eventId)) {

                return true;
            }
        }

        return false;
    }

    public List<SessionEvent> toEvents(JsonMapper jsonMapper) {
        List<SessionEvent> sessionEvents = new ArrayList<>();

        for (StoredEvent event : events) {
            sessionEvents.add(event.toEvent(jsonMapper));
        }

        return sessionEvents;
    }

    public Session toSession() {
        Session.Builder builder = Session.builder()
            .id(id)
            .userId(userId)
            .createdAt(Instant.ofEpochMilli(createdAtEpochMilli))
            .metadata(metadata);

        if (expiresAtEpochMilli != null) {
            builder.expiresAt(Instant.ofEpochMilli(expiresAtEpochMilli));
        }

        return builder.build();
    }

    @SuppressFBWarnings({
        "EI_EXPOSE_REP", "EI_EXPOSE_REP2"
    })
    public record StoredEvent(
        String id, String sessionId, long timestampEpochMilli, MessageType messageType, @Nullable String messageContent,
        @Nullable String messageData, boolean synthetic, boolean archived,
        Map<String, Object> metadata) {

        public StoredEvent {
            Objects.requireNonNull(id, "id must not be null");
            Objects.requireNonNull(sessionId, "sessionId must not be null");
            Objects.requireNonNull(messageType, "messageType must not be null");

            if (messageType == MessageType.USER || messageType == MessageType.SYSTEM) {
                Objects.requireNonNull(messageContent,
                    "messageContent must not be null for " + messageType + " events");
            }

            metadata = metadata == null ? Map.of() : Collections.unmodifiableMap(new HashMap<>(metadata));
        }

        private static final TypeReference<List<ToolCall>> TOOL_CALL_LIST_TYPE =
            new TypeReference<List<ToolCall>>() {};
        private static final TypeReference<List<ToolResponse>> TOOL_RESPONSE_LIST_TYPE =
            new TypeReference<List<ToolResponse>>() {};

        public static StoredEvent fromEvent(SessionEvent event, JsonMapper jsonMapper) {
            Message message = event.getMessage();

            String messageData = null;

            if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
                messageData = jsonMapper.writeValueAsString(assistantMessage.getToolCalls());
            } else if (message instanceof ToolResponseMessage toolResponseMessage) {
                messageData = jsonMapper.writeValueAsString(toolResponseMessage.getResponses());
            }

            return new StoredEvent(
                event.getId(), event.getSessionId(), event.getTimestamp()
                    .toEpochMilli(),
                message.getMessageType(), message.getText(), messageData, event.isSynthetic(), event.isArchived(),
                new HashMap<>(event.getMetadata()));
        }

        public SessionEvent toEvent(JsonMapper jsonMapper) {
            Map<String, Object> mergedMetadata = new HashMap<>(metadata);

            if (synthetic) {
                mergedMetadata.put(SessionEvent.METADATA_SYNTHETIC, true);
            }

            return SessionEvent.builder()
                .id(id)
                .sessionId(sessionId)
                .timestamp(Instant.ofEpochMilli(timestampEpochMilli))
                .message(toMessage(jsonMapper))
                .archived(archived)
                .metadata(mergedMetadata)
                .build();
        }

        private Message toMessage(JsonMapper jsonMapper) {
            return switch (messageType) {
                case USER -> new UserMessage(messageContent);
                case SYSTEM -> new SystemMessage(messageContent);
                case ASSISTANT -> {
                    if (messageData != null && !messageData.isBlank()) {
                        List<ToolCall> toolCalls = jsonMapper.readValue(messageData, TOOL_CALL_LIST_TYPE);

                        yield AssistantMessage.builder()
                            .content(messageContent)
                            .toolCalls(toolCalls)
                            .build();
                    }

                    yield new AssistantMessage(messageContent);
                }
                case TOOL -> {
                    List<ToolResponse> responses =
                        (messageData != null && !messageData.isBlank())
                            ? jsonMapper.readValue(messageData, TOOL_RESPONSE_LIST_TYPE)
                            : List.of();

                    yield ToolResponseMessage.builder()
                        .responses(responses)
                        .build();
                }
            };
        }
    }

    public static List<StoredEvent> toStoredEvents(List<SessionEvent> events, JsonMapper jsonMapper) {
        List<StoredEvent> stored = new ArrayList<>();

        for (SessionEvent event : events) {
            stored.add(StoredEvent.fromEvent(event, jsonMapper));
        }

        return stored;
    }

    private static List<StoredEvent> pruneArchivedEvents(List<StoredEvent> events, int maxArchivedEvents) {
        if (maxArchivedEvents < 0) {
            throw new IllegalArgumentException("maxArchivedEvents must not be negative");
        }

        int archivedEventCount = 0;

        for (StoredEvent event : events) {
            if (event.archived()) {
                archivedEventCount++;
            }
        }

        int archivedEventsToDrop = archivedEventCount - maxArchivedEvents;

        if (archivedEventsToDrop <= 0) {
            return events;
        }

        List<StoredEvent> keptEvents = new ArrayList<>(events.size() - archivedEventsToDrop);

        for (StoredEvent event : events) {
            if (event.archived() && archivedEventsToDrop > 0) {
                archivedEventsToDrop--;

                continue;
            }

            keptEvents.add(event);
        }

        return keptEvents;
    }
}
