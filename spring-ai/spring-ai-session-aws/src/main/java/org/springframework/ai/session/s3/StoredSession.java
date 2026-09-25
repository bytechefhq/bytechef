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

package org.springframework.ai.session.s3;

import java.time.Instant;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
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
 * JSON DTO that represents a persisted session and its events on S3.
 *
 * @author Ivica Cardic
 */
record StoredSession(
    String id, String userId, long createdAtEpochMilli, @Nullable Long expiresAtEpochMilli,
    Map<String, Object> metadata, long version, List<StoredEvent> events) {

    static StoredSession fromSession(Session session, long version, List<StoredEvent> events) {
        Instant expiresAt = session.expiresAt();

        return new StoredSession(
            session.id(), session.userId(), session.createdAt()
                .toEpochMilli(),
            expiresAt != null ? expiresAt.toEpochMilli() : null, new HashMap<>(session.metadata()), version, events);
    }

    Session toSession() {
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

    record StoredEvent(
        String id, String sessionId, long timestampEpochMilli, String messageType, String messageContent,
        @Nullable String messageData, boolean synthetic, boolean archived, @Nullable String branch,
        Map<String, Object> metadata) {

        private static final TypeReference<List<ToolCall>> TOOL_CALL_LIST_TYPE =
            new TypeReference<List<ToolCall>>() {};
        private static final TypeReference<List<ToolResponse>> TOOL_RESPONSE_LIST_TYPE =
            new TypeReference<List<ToolResponse>>() {};

        static StoredEvent fromEvent(SessionEvent event, JsonMapper jsonMapper) {
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
                message.getMessageType()
                    .name(),
                message.getText(), messageData, event.isSynthetic(), event.isArchived(),
                event.getBranch(), new HashMap<>(event.getMetadata()));
        }

        SessionEvent toEvent(JsonMapper jsonMapper) {
            Map<String, Object> mergedMetadata = new HashMap<>(metadata);

            if (synthetic) {
                mergedMetadata.put(SessionEvent.METADATA_SYNTHETIC, true);
            }

            return SessionEvent.builder()
                .id(id)
                .sessionId(sessionId)
                .timestamp(Instant.ofEpochMilli(timestampEpochMilli))
                .message(toMessage(jsonMapper))
                .branch(branch)
                .archived(archived)
                .metadata(mergedMetadata)
                .build();
        }

        private Message toMessage(JsonMapper jsonMapper) {
            return switch (MessageType.valueOf(messageType)) {
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

    static List<StoredEvent> toStoredEvents(List<SessionEvent> events, JsonMapper jsonMapper) {
        List<StoredEvent> stored = new ArrayList<>();

        for (SessionEvent event : events) {
            stored.add(StoredEvent.fromEvent(event, jsonMapper));
        }

        return stored;
    }
}
