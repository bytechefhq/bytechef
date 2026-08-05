/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.task;

import com.bytechef.ee.ai.hub.exception.TitleGenerationFailedException;
import com.bytechef.ee.ai.hub.task.AiHubTaskService.AiHubTaskMessage;
import java.util.ArrayList;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.prompt.Prompt;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Service;

/**
 * Generates a short human-readable title for a task by sending a condensed message excerpt to the configured
 * {@link ChatModel}.
 *
 * <p>
 * The generated title is intended to be at most 60 characters. If the model returns blank text or text exceeding 60
 * characters, an empty string is returned so the caller can decide to skip or retry. If the chat model itself throws
 * (network error, rate limit, upstream 5xx), {@link TitleGenerationFailedException} is propagated so the controller can
 * return a non-2xx — distinguishing model-unavailable from "model returned blank" matters because only the former is
 * worth retrying or surfacing as a user-facing toast.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@ConditionalOnProperty(prefix = "bytechef.ai.hub", name = "enabled", havingValue = "true")
public class TitleGenerationService {

    private static final Logger log = LoggerFactory.getLogger(TitleGenerationService.class);

    private static final int MAX_USER_MESSAGES = 3;
    private static final int MAX_ASSISTANT_MESSAGES = 2;
    private static final int MAX_CONTENT_LENGTH = 500;
    private static final int MAX_TITLE_LENGTH = 60;

    private static final String SYSTEM_PROMPT =
        "Given this short exchange, produce a concise 4-8 word title describing the topic. " +
            "Return only the title, no quotes, no trailing punctuation.";

    private final ChatModel chatModel;

    public TitleGenerationService(ChatModel chatModel) {
        this.chatModel = chatModel;
    }

    /**
     * Generates a title from the first few messages in the task.
     *
     * @param messages the full message list (may be empty)
     * @return a trimmed title string, or an empty string if the model response is blank or too long
     * @throws TitleGenerationFailedException when the upstream chat model fails (network, rate-limit, 5xx); allows the
     *                                        controller to map to 503 so the client toasts the failure
     */
    public String generateTitle(List<AiHubTaskMessage> messages) {
        List<AiHubTaskMessage> selectedMessages = selectMessages(messages);

        if (selectedMessages.isEmpty()) {
            return "";
        }

        String formattedExchange = formatExchange(selectedMessages);

        String promptText = SYSTEM_PROMPT + "\n\n" + formattedExchange;

        String response;

        try {
            ChatResponse chatResponse = chatModel.call(new Prompt(promptText));

            Generation generation = chatResponse == null ? null : chatResponse.getResult();

            response = generation == null ? null
                : generation.getOutput()
                    .getText();
        } catch (RuntimeException exception) {
            // Surface the failure with a typed exception so the controller can return 503 — silently swallowing
            // and returning "" leaves the user with a permanent "Untitled" and no signal that the upstream
            // model is unhealthy. The task itself remains usable; the controller still returns the
            // current row state on the error path.
            log.warn(
                "Title generation chat-model call failed for {} message(s): {}",
                selectedMessages.size(), exception.getMessage(), exception);

            throw new TitleGenerationFailedException(
                "Title generation failed: " + exception.getMessage(), exception);
        }

        if (response == null) {
            // Without this log, the caller silently no-ops the title patch and the user sees "Untitled" with
            // zero observability into whether the model failed or returned garbage.
            log.warn("Title generation returned null response for {} message(s)", selectedMessages.size());

            return "";
        }

        String trimmed = response.trim();

        if (trimmed.isEmpty()) {
            log.warn("Title generation returned blank response for {} message(s)", selectedMessages.size());

            return "";
        }

        if (trimmed.length() > MAX_TITLE_LENGTH) {
            // Log a snippet so an operator can compare the generated title to the prompt — long titles often
            // mean the model ignored the "concise 4-8 word" instruction.
            log.warn(
                "Title generation returned over-length response ({} chars; max {}): '{}'",
                trimmed.length(), MAX_TITLE_LENGTH,
                trimmed.substring(0, Math.min(MAX_TITLE_LENGTH * 2, trimmed.length())));

            return "";
        }

        return trimmed;
    }

    private List<AiHubTaskMessage> selectMessages(List<AiHubTaskMessage> messages) {
        List<AiHubTaskMessage> selected = new ArrayList<>();

        int userCount = 0;
        int assistantCount = 0;

        for (AiHubTaskMessage message : messages) {
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

    private String formatExchange(List<AiHubTaskMessage> messages) {
        StringBuilder builder = new StringBuilder();

        for (AiHubTaskMessage message : messages) {
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
