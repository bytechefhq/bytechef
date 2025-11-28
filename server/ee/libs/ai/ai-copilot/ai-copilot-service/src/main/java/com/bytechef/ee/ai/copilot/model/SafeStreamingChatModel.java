/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.copilot.model;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.messages.AssistantMessage;
import org.springframework.ai.chat.model.ChatModel;
import org.springframework.ai.chat.model.ChatResponse;
import org.springframework.ai.chat.model.Generation;
import org.springframework.ai.chat.model.StreamingChatModel;
import org.springframework.ai.chat.prompt.ChatOptions;
import org.springframework.ai.chat.prompt.Prompt;
import reactor.core.publisher.Flux;

import java.util.Collections;

/**
 * Wrapper for ChatModel that ensures ChatResponse.getResult() is never null during streaming. This fixes compatibility
 * issues with the agui library which doesn't handle null results.
 *
 * @version ee
 * @author Marko Kriskovic
 */
@SuppressFBWarnings("EI")
public class SafeStreamingChatModel implements ChatModel, StreamingChatModel {

    private static final Logger logger = LoggerFactory.getLogger(SafeStreamingChatModel.class);

    private final ChatModel delegate;
    private final StreamingChatModel streamingDelegate;

    public SafeStreamingChatModel(ChatModel chatModel) {
        this.delegate = chatModel;
        this.streamingDelegate = (StreamingChatModel) chatModel;
        logger.info("SafeStreamingChatModel initialized with delegate: {}", chatModel.getClass()
            .getName());
    }

    @Override
    public ChatResponse call(Prompt prompt) {
        return delegate.call(prompt);
    }

    @Override
    public ChatOptions getDefaultOptions() {
        return delegate.getDefaultOptions();
    }

    @Override
    public Flux<ChatResponse> stream(Prompt prompt) {
        return streamingDelegate.stream(prompt)
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
                logger.debug("Response has valid result, passing through");
                return response;
            }
            logger.warn("Response getResult() returned null or output is null, creating safe response");
        } catch (Exception e) {
            // If getResult() throws, we need to create a safe response
            logger.warn("Exception checking getResult(): {}", e.getMessage());
        }

        Generation safeGeneration = new Generation(new AssistantMessage(""));
        logger.info("Created safe ChatResponse with empty generation");

        return new ChatResponse(
            Collections.singletonList(safeGeneration),
            response.getMetadata());
    }
}
