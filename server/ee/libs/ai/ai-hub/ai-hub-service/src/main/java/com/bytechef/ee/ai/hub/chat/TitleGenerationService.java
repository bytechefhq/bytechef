/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.chat;

import com.bytechef.ee.ai.hub.chat.AiHubChatService.AiHubChatMessage;
import com.bytechef.ee.ai.hub.config.ConditionalOnAiHub;
import com.bytechef.ee.ai.hub.exception.TitleGenerationFailedException;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.client.ChatClient;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnAiHub
public class TitleGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TitleGenerationService.class);

    private static final int MAX_USER_MESSAGES = 3;
    private static final int MAX_ASSISTANT_MESSAGES = 2;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 60;

    private static final String SYSTEM_PROMPT =
        "Given this short exchange, produce a concise 4-8 word title describing the topic. " +
            "Return only the title, no quotes, no trailing punctuation.";

    private final ChatClient chatClient;

    public TitleGenerationService(ChatModel chatModel) {
        this.chatClient = ChatClient.builder(chatModel)
            .build();
    }

    public String generateTitle(List<AiHubChatMessage> messages) {
        List<AiHubChatMessage> selectedMessages = selectMessages(messages);

        if (selectedMessages.isEmpty()) {
            return "";
        }

        String formattedExchange = formatExchange(selectedMessages);

        String promptText = SYSTEM_PROMPT + "\n\n" + formattedExchange;

        String response;

        try {
            ChatResponse chatResponse = chatClient
                .prompt(promptText)
                .call()
                .chatResponse();

            Generation generation = chatResponse == null ? null : chatResponse.getResult();

            response = generation == null ? null
                : generation.getOutput()
                    .getText();
        } catch (RuntimeException exception) {
            log.warn(
                "Title generation chat-model call failed for {} message(s): {}",
                selectedMessages.size(), exception.getMessage(), exception);

            throw new TitleGenerationFailedException(
                "Title generation failed: " + exception.getMessage(), exception);
        }

        if (response == null) {
            log.warn("Title generation returned null response for {} message(s)", selectedMessages.size());

            return "";
        }

        String trimmed = response.trim();

        if (trimmed.isEmpty()) {
            log.warn("Title generation returned blank response for {} message(s)", selectedMessages.size());

            return "";
        }

        if (trimmed.length() > MAX_TITLE_LENGTH) {
            log.warn(
                "Title generation returned over-length response ({} chars; max {}): '{}'",
                trimmed.length(), MAX_TITLE_LENGTH,
                trimmed.substring(0, Math.min(MAX_TITLE_LENGTH * 2, trimmed.length())));

            return "";
        }

        return trimmed;
    }

    private List<AiHubChatMessage> selectMessages(List<AiHubChatMessage> messages) {
        List<AiHubChatMessage> selected = new ArrayList<>();

        int userCount = 0;
        int assistantCount = 0;

        for (AiHubChatMessage message : messages) {
            String role = message.role();

            if ("USER".equalsIgnoreCase(role) && userCount < MAX_USER_MESSAGES) {
                selected.add(message);
                userCount++;
            } else if ("ASSISTANT".equalsIgnoreCase(role) && assistantCount < MAX_ASSISTANT_MESSAGES) {
                selected.add(message);
                assistantCount++;
            }

            if (userCount >= MAX_USER_MESSAGES && assistantCount >= MAX_ASSISTANT_MESSAGES) {
                break;
            }
        }

        return selected;
    }

    private String formatExchange(List<AiHubChatMessage> messages) {
        StringBuilder builder = new StringBuilder();

        for (AiHubChatMessage message : messages) {
            String content = message.content();

            if (content.length() > MAX_CONTENT_LENGTH) {
                content = content.substring(0, MAX_CONTENT_LENGTH);
            }

            builder.append(message.role())
                .append(": ")
                .append(content)
                .append("\n");
        }

        return builder.toString()
            .trim();
    }
}
