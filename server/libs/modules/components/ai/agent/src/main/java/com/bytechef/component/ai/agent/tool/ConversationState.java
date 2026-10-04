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

package com.bytechef.component.ai.agent.tool;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonSubTypes;
import com.fasterxml.jackson.annotation.JsonTypeInfo;
import java.util.ArrayList;
import java.util.Base64;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AbstractMessage;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.content.Media;
import org.springframework.util.MimeTypeUtils;

/**
 * @author Ivica Cardic
 */
public record ConversationState(List<Entry> messages) {

    private static final String BYTES_KEY = "$bytes";

    public ConversationState {
        messages = messages == null ? List.of() : List.copyOf(messages);
    }

    @JsonTypeInfo(use = JsonTypeInfo.Id.NAME, property = "kind")
    @JsonSubTypes({
        @JsonSubTypes.Type(value = SystemEntry.class, name = "system"),
        @JsonSubTypes.Type(value = UserEntry.class, name = "user"),
        @JsonSubTypes.Type(value = AssistantEntry.class, name = "assistant"),
        @JsonSubTypes.Type(value = ToolEntry.class, name = "tool")
    })
    public sealed interface Entry permits SystemEntry, UserEntry, AssistantEntry, ToolEntry {
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record SystemEntry(String text, Map<String, Object> metadata) implements Entry {

        public SystemEntry {
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record UserEntry(String text, List<MediaEntry> media, Map<String, Object> metadata) implements Entry {

        public UserEntry {
            media = media == null ? List.of() : List.copyOf(media);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record AssistantEntry(
        @Nullable String text, List<ToolCallEntry> toolCalls, List<MediaEntry> media, Map<String, Object> metadata)
        implements Entry {

        public AssistantEntry {
            toolCalls = toolCalls == null ? List.of() : List.copyOf(toolCalls);
            media = media == null ? List.of() : List.copyOf(media);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    @JsonIgnoreProperties(ignoreUnknown = true)
    public record ToolEntry(List<ToolResponseEntry> toolResponses, Map<String, Object> metadata) implements Entry {

        public ToolEntry {
            toolResponses = toolResponses == null ? List.of() : List.copyOf(toolResponses);
            metadata = metadata == null ? Map.of() : Map.copyOf(metadata);
        }
    }

    public record ToolCallEntry(String id, String type, String name, String arguments) {
    }

    public record ToolResponseEntry(String id, String name, String responseData) {
    }

    public record MediaEntry(
        String mimeType, @Nullable String id, @Nullable String name, @Nullable String url, @Nullable String data) {
    }

    public static ConversationState from(List<Message> messages) {
        List<Entry> entries = new ArrayList<>();

        for (Message message : messages) {
            entries.add(toEntry(message));
        }

        return new ConversationState(entries);
    }

    public List<Message> toMessages() {
        List<Message> result = new ArrayList<>();

        for (Entry entry : messages) {
            result.add(toMessage(entry));
        }

        return result;
    }

    private static Entry toEntry(Message message) {
        Map<String, Object> metadata = toPersistableMetadata(message.getMetadata());

        if (message instanceof SystemMessage systemMessage) {
            return new SystemEntry(systemMessage.getText(), metadata);
        }

        if (message instanceof UserMessage userMessage) {
            return new UserEntry(userMessage.getText(), toMediaEntries(userMessage.getMedia()), metadata);
        }

        if (message instanceof AssistantMessage assistantMessage) {
            List<ToolCallEntry> toolCalls = new ArrayList<>();

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                toolCalls.add(
                    new ToolCallEntry(toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
            }

            return new AssistantEntry(
                assistantMessage.getText(), toolCalls, toMediaEntries(assistantMessage.getMedia()), metadata);
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            List<ToolResponseEntry> toolResponses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                toolResponses.add(
                    new ToolResponseEntry(toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
            }

            return new ToolEntry(toolResponses, metadata);
        }

        throw new IllegalArgumentException(
            "Unsupported message type for conversation serialization: " + message.getClass());
    }

    private static Message toMessage(Entry entry) {
        return switch (entry) {
            case SystemEntry systemEntry -> SystemMessage.builder()
                .text(systemEntry.text())
                .metadata(fromPersistableMetadata(systemEntry.metadata()))
                .build();
            case UserEntry userEntry -> UserMessage.builder()
                .text(userEntry.text())
                .media(toMedia(userEntry.media()))
                .metadata(fromPersistableMetadata(userEntry.metadata()))
                .build();
            case AssistantEntry assistantEntry -> {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

                for (ToolCallEntry toolCall : assistantEntry.toolCalls()) {
                    toolCalls.add(
                        new AssistantMessage.ToolCall(
                            toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
                }

                yield AssistantMessage.builder()
                    .content(assistantEntry.text())
                    .toolCalls(toolCalls)
                    .media(toMedia(assistantEntry.media()))
                    .properties(fromPersistableMetadata(assistantEntry.metadata()))
                    .build();
            }
            case ToolEntry toolEntry -> {
                List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

                for (ToolResponseEntry toolResponse : toolEntry.toolResponses()) {
                    toolResponses.add(
                        new ToolResponseMessage.ToolResponse(
                            toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
                }

                yield ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .metadata(fromPersistableMetadata(toolEntry.metadata()))
                    .build();
            }
        };
    }

    private static List<MediaEntry> toMediaEntries(List<Media> mediaList) {
        List<MediaEntry> mediaEntries = new ArrayList<>();

        for (Media media : mediaList) {
            String mimeType = String.valueOf(media.getMimeType());
            Object data = media.getData();

            if (data instanceof String url) {
                mediaEntries.add(new MediaEntry(mimeType, media.getId(), media.getName(), url, null));
            } else {
                Base64.Encoder encoder = Base64.getEncoder();

                mediaEntries.add(
                    new MediaEntry(
                        mimeType, media.getId(), media.getName(), null,
                        encoder.encodeToString(media.getDataAsByteArray())));
            }
        }

        return mediaEntries;
    }

    private static List<Media> toMedia(List<MediaEntry> mediaEntries) {
        List<Media> mediaList = new ArrayList<>();

        for (MediaEntry mediaEntry : mediaEntries) {
            Media.Builder builder = Media.builder()
                .mimeType(MimeTypeUtils.parseMimeType(mediaEntry.mimeType()));

            String url = mediaEntry.url();

            if (url != null) {
                builder.data(url);
            } else {
                Base64.Decoder decoder = Base64.getDecoder();

                builder.data(decoder.decode(mediaEntry.data()));
            }

            if (mediaEntry.id() != null) {
                builder.id(mediaEntry.id());
            }

            if (mediaEntry.name() != null) {
                builder.name(mediaEntry.name());
            }

            mediaList.add(builder.build());
        }

        return mediaList;
    }

    private static Map<String, Object> toPersistableMetadata(Map<String, Object> metadata) {
        Map<String, Object> persistableMetadata = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            if (AbstractMessage.MESSAGE_TYPE.equals(entry.getKey())) {
                continue;
            }

            Object value = toPersistableValue(entry.getValue());

            if (value != null) {
                persistableMetadata.put(entry.getKey(), value);
            }
        }

        return persistableMetadata;
    }

    private static @Nullable Object toPersistableValue(@Nullable Object value) {
        if (value instanceof String || value instanceof Number || value instanceof Boolean) {
            return value;
        }

        if (value instanceof byte[] bytes) {
            Base64.Encoder encoder = Base64.getEncoder();

            return Map.of(BYTES_KEY, encoder.encodeToString(bytes));
        }

        if (value instanceof List<?> list) {
            List<Object> persistableList = new ArrayList<>();

            for (Object item : list) {
                Object persistableItem = toPersistableValue(item);

                if (persistableItem == null) {
                    return null;
                }

                persistableList.add(persistableItem);
            }

            return persistableList;
        }

        if (value instanceof Map<?, ?> map) {
            Map<String, Object> persistableMap = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                Object persistableItem = toPersistableValue(entry.getValue());

                if (!(entry.getKey() instanceof String key) || persistableItem == null) {
                    return null;
                }

                persistableMap.put(key, persistableItem);
            }

            return persistableMap;
        }

        return null;
    }

    private static Map<String, Object> fromPersistableMetadata(Map<String, Object> metadata) {
        Map<String, Object> restoredMetadata = new LinkedHashMap<>();

        for (Map.Entry<String, Object> entry : metadata.entrySet()) {
            restoredMetadata.put(entry.getKey(), fromPersistableValue(entry.getValue()));
        }

        return restoredMetadata;
    }

    private static Object fromPersistableValue(Object value) {
        if (value instanceof List<?> list) {
            List<Object> restoredList = new ArrayList<>();

            for (Object item : list) {
                restoredList.add(fromPersistableValue(item));
            }

            return restoredList;
        }

        if (value instanceof Map<?, ?> map) {
            if (map.size() == 1 && map.get(BYTES_KEY) instanceof String encodedBytes) {
                Base64.Decoder decoder = Base64.getDecoder();

                return decoder.decode(encodedBytes);
            }

            Map<String, Object> restoredMap = new LinkedHashMap<>();

            for (Map.Entry<?, ?> entry : map.entrySet()) {
                restoredMap.put(String.valueOf(entry.getKey()), fromPersistableValue(entry.getValue()));
            }

            return restoredMap;
        }

        return value;
    }
}
