/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClientRequest;
import org.springframework.ai.chat.client.ChatClientResponse;
import org.springframework.ai.chat.client.advisor.api.CallAdvisor;
import org.springframework.ai.chat.client.advisor.api.CallAdvisorChain;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisor;
import org.springframework.ai.chat.client.advisor.api.StreamAdvisorChain;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.messages.AssistantMessage.ToolCall;
import org.springframework.ai.chat.messages.Message;
import org.springframework.ai.chat.messages.MessageType;
import org.springframework.ai.chat.messages.ToolResponseMessage;
import org.springframework.ai.chat.messages.ToolResponseMessage.ToolResponse;
import org.springframework.ai.chat.messages.UserMessage;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class NonEmptyMessagesAdvisor implements CallAdvisor, StreamAdvisor {

    private static final Logger log = LoggerFactory.getLogger(NonEmptyMessagesAdvisor.class);

    private static final String USER_PLACEHOLDER = "no content";
    private static final String TOOL_PLACEHOLDER = "{\"result\":\"no content\"}";

    private static final int ORDER = Integer.MAX_VALUE - 1;

    @Override
    public String getName() {
        return "ai-hub-non-empty-messages";
    }

    @Override
    public int getOrder() {
        return ORDER;
    }

    @Override
    public ChatClientResponse adviseCall(ChatClientRequest chatClientRequest, CallAdvisorChain callAdvisorChain) {
        return callAdvisorChain.nextCall(patch(chatClientRequest));
    }

    @Override
    public Flux<ChatClientResponse> adviseStream(
        ChatClientRequest chatClientRequest, StreamAdvisorChain streamAdvisorChain) {

        return streamAdvisorChain.nextStream(patch(chatClientRequest));
    }

    private ChatClientRequest patch(ChatClientRequest chatClientRequest) {
        Prompt prompt = chatClientRequest.prompt();

        List<Message> original = prompt.getInstructions();
        List<Message> patched = new ArrayList<>(original.size());

        boolean changed = false;

        for (int index = 0; index < original.size(); index++) {
            Message message = original.get(index);
            Message replacement = patchMessage(message, index);

            if (replacement == null) {
                changed = true;

                continue;
            }

            if (replacement != message) {
                changed = true;
            }

            patched.add(replacement);
        }

        List<Message> paired = enforceToolCallPairing(patched);

        if (paired != patched) {
            changed = true;
        }

        if (!changed) {
            return chatClientRequest;
        }

        Prompt patchedPrompt = new Prompt(paired, prompt.getOptions());

        return chatClientRequest.mutate()
            .prompt(patchedPrompt)
            .build();
    }

    private List<Message> enforceToolCallPairing(List<Message> messages) {
        List<Message> result = null;

        for (int index = 0; index < messages.size(); index++) {
            Message message = messages.get(index);
            Message replacement = pairToolCalls(messages, message, index);

            if (replacement == message) {
                if (result != null) {
                    result.add(message);
                }

                continue;
            }

            if (result == null) {
                result = new ArrayList<>(messages.subList(0, index));
            }

            if (replacement != null) {
                result.add(replacement);
            }
        }

        return result == null ? messages : result;
    }

    private Message pairToolCalls(List<Message> messages, Message message, int index) {
        if (!(message instanceof AssistantMessage assistantMessage) || !assistantMessage.hasToolCalls()) {
            return message;
        }

        if (index + 1 >= messages.size()) {
            return message;
        }

        Set<String> answeredToolCallIds = answeredToolCallIds(messages.get(index + 1));

        List<ToolCall> toolCalls = assistantMessage.getToolCalls();
        List<ToolCall> keptToolCalls = toolCalls.stream()
            .filter(toolCall -> answeredToolCallIds.contains(toolCall.id()))
            .toList();

        if (keptToolCalls.size() == toolCalls.size()) {
            return message;
        }

        String text = assistantMessage.getText();
        boolean hasText = text != null && !text.isBlank();

        log.warn(
            "Stripping {} unanswered tool_call(s) from assistant message at position {} before Anthropic dispatch; "
                + "the turn was interrupted before the tool_result was persisted, and Anthropic rejects a tool_use "
                + "without a matching tool_result.",
            toolCalls.size() - keptToolCalls.size(), index);

        if (keptToolCalls.isEmpty() && !hasText) {
            return null;
        }

        return AssistantMessage.builder()
            .content(hasText ? text : "")
            .properties(assistantMessage.getMetadata())
            .toolCalls(keptToolCalls)
            .build();
    }

    private static Set<String> answeredToolCallIds(Message message) {
        if (!(message instanceof ToolResponseMessage toolResponseMessage)) {
            return Set.of();
        }

        return toolResponseMessage.getResponses()
            .stream()
            .filter(response -> response.responseData() != null && !response.responseData()
                .isBlank())
            .map(ToolResponse::id)
            .collect(Collectors.toSet());
    }

    private Message patchMessage(Message message, int index) {
        MessageType type = message.getMessageType();

        if (type == MessageType.USER) {
            String text = message.getText();

            if (text != null && !text.isBlank()) {
                return message;
            }

            log.warn(
                "Substituting placeholder for empty USER message at position {} before Anthropic dispatch; "
                    + "Anthropic rejects empty user content with HTTP 400.",
                index);

            return UserMessage.builder()
                .text(USER_PLACEHOLDER)
                .metadata(message.getMetadata())
                .build();
        }

        if (type == MessageType.ASSISTANT && message instanceof AssistantMessage assistantMessage) {
            String text = assistantMessage.getText();
            boolean hasText = text != null && !text.isBlank();
            boolean hasToolCalls = assistantMessage.hasToolCalls();

            if (hasText || hasToolCalls) {
                return message;
            }

            return null;
        }

        if (type == MessageType.TOOL && message instanceof ToolResponseMessage toolMessage) {
            List<ToolResponse> responses = toolMessage.getResponses();

            if (responses.isEmpty()) {
                return null;
            }

            boolean allEmpty = responses.stream()
                .allMatch(response -> response.responseData() == null || response.responseData()
                    .isBlank());

            if (allEmpty) {
                return null;
            }

            return patchMixedResponses(toolMessage, responses, index);
        }

        return message;
    }

    private Message patchMixedResponses(ToolResponseMessage toolMessage, List<ToolResponse> responses, int index) {
        List<ToolResponse> patchedResponses = null;

        for (int i = 0; i < responses.size(); i++) {
            ToolResponse response = responses.get(i);
            String data = response.responseData();

            if (data != null && !data.isBlank()) {
                if (patchedResponses != null) {
                    patchedResponses.add(response);
                }

                continue;
            }

            if (patchedResponses == null) {
                patchedResponses = new ArrayList<>(responses.subList(0, i));
            }

            log.warn(
                "Substituting placeholder for empty tool response at message {} response {} "
                    + "(tool='{}', toolCallId='{}'); parallel-call race within the current turn.",
                index, i, response.name(), response.id());

            patchedResponses.add(new ToolResponse(response.id(), response.name(), TOOL_PLACEHOLDER));
        }

        if (patchedResponses == null) {
            return toolMessage;
        }

        return ToolResponseMessage.builder()
            .responses(patchedResponses)
            .metadata(toolMessage.getMetadata())
            .build();
    }
}
