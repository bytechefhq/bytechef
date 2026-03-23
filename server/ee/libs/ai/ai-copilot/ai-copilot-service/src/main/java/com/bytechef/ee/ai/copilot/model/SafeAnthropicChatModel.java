/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Collections;
import org.jspecify.annotations.NonNull;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.anthropic.AnthropicChatModel;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

/**
 * Wrapper for ChatModel that ensures ChatResponse.getResult() is never null during streaming. This fixes compatibility
 * issues with the agui library which doesn't handle null results.
 *
 * @version ee
 *
 * @author Marko Kriskovic
 */
public class SafeAnthropicChatModel implements ChatModel, StreamingChatModel {

    private static final Logger logger = LoggerFactory.getLogger(SafeAnthropicChatModel.class);

    private final AnthropicChatModel delegate;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public SafeAnthropicChatModel(AnthropicChatModel delegate) {
        this.delegate = delegate;

        if (logger.isTraceEnabled()) {
            logger.trace("SafeAnthropicChatModel initialized with delegate: {}", delegate.getClass()
                .getName());
        }
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.call(prompt);
    }

    @Override
    public @NonNull Flux<ChatResponse> stream(Prompt prompt) {
        return delegate.stream(prompt)
            .map(this::ensureNonNullResult);
    }

    /**
     * Ensures ChatResponse has a non-null result by creating an empty Generation if needed. This prevents NPE in
     * libraries that assume getResult() is always non-null.
     */
    private ChatResponse ensureNonNullResult(ChatResponse response) {
        try {
            Generation result = response.getResult();

            if (result != null && result.getOutput() != null) {
                return response;
            }
        } catch (Exception exception) {
            if (logger.isDebugEnabled()) {
                logger.debug("Exception checking getResult(): {}", exception.getMessage());
            }
        }

        if (logger.isDebugEnabled()) {
            logger.debug("Response getResult() returned null or output is null, creating safe response");
        }

        Generation safeGeneration = new Generation(new AssistantMessage(""));

        return new ChatResponse(Collections.singletonList(safeGeneration), response.getMetadata());
    }

}
