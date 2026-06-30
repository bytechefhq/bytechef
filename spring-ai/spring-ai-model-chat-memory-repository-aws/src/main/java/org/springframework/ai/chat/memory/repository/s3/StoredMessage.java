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

package org.springframework.ai.chat.memory.repository.s3;

import java.util.List;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

/**
 * JSON-serializable representation of a single Spring AI {@link Message}, mirroring the column split used by the JDBC
 * chat-memory repository: {@code type} (the {@link MessageType} name), {@code text} (plain content), and {@code data}
 * (a JSON blob carrying tool-call / tool-response payloads, {@code null} for plain messages).
 *
 * @author Ivica Cardic
 */
record StoredMessage(String type, String text, @Nullable String data) {

    static StoredMessage from(Message message, JsonMapper jsonMapper) {
        String data = null;

        if (message instanceof AssistantMessage assistantMessage && assistantMessage.hasToolCalls()) {
            data = jsonMapper.writeValueAsString(assistantMessage.getToolCalls());
        } else if (message instanceof ToolResponseMessage toolResponseMessage) {
            data = jsonMapper.writeValueAsString(toolResponseMessage.getResponses());
        }

        MessageType messageType = message.getMessageType();

        return new StoredMessage(messageType.name(), message.getText(), data);
    }

    Message toMessage(JsonMapper jsonMapper) {
        return switch (MessageType.valueOf(type)) {
            case USER -> new UserMessage(text);
            case SYSTEM -> new SystemMessage(text);
            case ASSISTANT -> {
                if (data != null && !data.isBlank()) {
                    List<AssistantMessage.ToolCall> toolCalls = jsonMapper.readValue(data, new TypeReference<>() {});

                    yield AssistantMessage.builder()
                        .content(text)
                        .toolCalls(toolCalls)
                        .build();
                }

                yield new AssistantMessage(text);
            }
            case TOOL -> {
                List<ToolResponseMessage.ToolResponse> responses = (data != null && !data.isBlank())
                    ? jsonMapper.readValue(data, new TypeReference<>() {})
                    : List.of();

                yield ToolResponseMessage.builder()
                    .responses(responses)
                    .build();
            }
        };
    }
}
