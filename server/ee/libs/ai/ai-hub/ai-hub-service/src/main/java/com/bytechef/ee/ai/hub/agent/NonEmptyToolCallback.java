/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import com.bytechef.component.ai.llm.tool.DelegatingToolCallback;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class NonEmptyToolCallback implements DelegatingToolCallback {

    private static final Logger log = LoggerFactory.getLogger(NonEmptyToolCallback.class);

    private static final String EMPTY_RESULT_PLACEHOLDER = "{\"result\":\"no content\"}";

    private final ToolCallback delegate;
    private final String toolName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private NonEmptyToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
        this.toolName = delegate.getToolDefinition()
            .name();
    }

    public static ToolCallback wrap(ToolCallback delegate) {
        if (delegate instanceof NonEmptyToolCallback) {
            return delegate;
        }

        return new NonEmptyToolCallback(delegate);
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolCallback getDelegate() {
        return delegate;
    }

    @Override
    public String call(String toolInput) {
        logInvocation(toolInput);

        return guardEmpty(delegate.call(toolInput));
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        logInvocation(toolInput);

        return guardEmpty(delegate.call(toolInput, toolContext));
    }

    private void logInvocation(@Nullable String toolInput) {
        if (log.isDebugEnabled()) {
            String preview = toolInput == null ? "<null>" : toolInput.substring(0, Math.min(toolInput.length(), 200));

            log.debug("Tool '{}' invoked with input: {}", toolName, preview);
        }
    }

    private String guardEmpty(String result) {
        if (result != null && !result.isBlank()) {
            return result;
        }

        log.warn(
            "Tool '{}' returned {}; substituting placeholder so the Anthropic Messages API does not reject the next "
                + "agent iteration with empty user-message content. Investigate the underlying callback to see why "
                + "it returned no content; the LLM will see the placeholder in this turn's history.",
            toolName, result == null ? "null" : "empty/whitespace");

        return EMPTY_RESULT_PLACEHOLDER;
    }
}
