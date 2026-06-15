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

import java.util.ArrayList;
import java.util.List;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.SystemMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.UserMessage;

/**
 * Serializable representation of an agent conversation (a Spring AI {@code List<Message>}). Stored in a suspended
 * agent's {@code continueParameters} so the tool-calling loop can be reconstructed on resume. Only the message kinds
 * the agent loop produces are supported.
 *
 * @author Ivica Cardic
 */
public record ConversationState(List<MessageEntry> messages) {

    public ConversationState {
        messages = List.copyOf(messages);
    }

    public record MessageEntry(
        String kind, String text, List<ToolCallEntry> toolCalls, List<ToolResponseEntry> toolResponses) {

        public MessageEntry {
            toolCalls = List.copyOf(toolCalls);
            toolResponses = List.copyOf(toolResponses);
        }
    }

    public record ToolCallEntry(String id, String type, String name, String arguments) {
    }

    public record ToolResponseEntry(String id, String name, String responseData) {
    }

    public static ConversationState from(List<Message> messages) {
        List<MessageEntry> entries = new ArrayList<>();

        for (Message message : messages) {
            entries.add(toEntry(message));
        }

        return new ConversationState(entries);
    }

    public List<Message> toMessages() {
        List<Message> result = new ArrayList<>();

        for (MessageEntry entry : messages) {
            result.add(toMessage(entry));
        }

        return result;
    }

    private static MessageEntry toEntry(Message message) {
        if (message instanceof SystemMessage systemMessage) {
            return new MessageEntry("system", systemMessage.getText(), List.of(), List.of());
        }

        if (message instanceof UserMessage userMessage) {
            return new MessageEntry("user", userMessage.getText(), List.of(), List.of());
        }

        if (message instanceof AssistantMessage assistantMessage) {
            List<ToolCallEntry> toolCalls = new ArrayList<>();

            for (AssistantMessage.ToolCall toolCall : assistantMessage.getToolCalls()) {
                toolCalls.add(
                    new ToolCallEntry(toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
            }

            return new MessageEntry("assistant", assistantMessage.getText(), toolCalls, List.of());
        }

        if (message instanceof ToolResponseMessage toolResponseMessage) {
            List<ToolResponseEntry> toolResponses = new ArrayList<>();

            for (ToolResponseMessage.ToolResponse toolResponse : toolResponseMessage.getResponses()) {
                toolResponses.add(
                    new ToolResponseEntry(toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
            }

            return new MessageEntry("tool", "", List.of(), toolResponses);
        }

        throw new IllegalArgumentException(
            "Unsupported message type for conversation serialization: " + message.getClass());
    }

    private static Message toMessage(MessageEntry entry) {
        return switch (entry.kind()) {
            case "system" -> new SystemMessage(entry.text());
            case "user" -> new UserMessage(entry.text());
            case "assistant" -> {
                List<AssistantMessage.ToolCall> toolCalls = new ArrayList<>();

                for (ToolCallEntry toolCall : entry.toolCalls()) {
                    toolCalls.add(
                        new AssistantMessage.ToolCall(
                            toolCall.id(), toolCall.type(), toolCall.name(), toolCall.arguments()));
                }

                yield AssistantMessage.builder()
                    .content(entry.text())
                    .toolCalls(toolCalls)
                    .build();
            }
            case "tool" -> {
                List<ToolResponseMessage.ToolResponse> toolResponses = new ArrayList<>();

                for (ToolResponseEntry toolResponse : entry.toolResponses()) {
                    toolResponses.add(
                        new ToolResponseMessage.ToolResponse(
                            toolResponse.id(), toolResponse.name(), toolResponse.responseData()));
                }

                yield ToolResponseMessage.builder()
                    .responses(toolResponses)
                    .build();
            }
            default -> throw new IllegalArgumentException("Unknown message kind: " + entry.kind());
        };
    }
}
