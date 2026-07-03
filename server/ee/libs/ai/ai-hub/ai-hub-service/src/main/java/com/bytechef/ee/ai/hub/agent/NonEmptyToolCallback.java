/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.agent;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Decorator that guarantees a {@link ToolCallback} never returns {@code null}, empty, or whitespace-only content.
 * Anthropic's Messages API rejects requests where any user message (which is how Spring AI represents tool results) has
 * empty content with HTTP 400 {@code messages.<N>: user messages must have non-empty content}, and that error aborts
 * the entire streaming turn — no events past {@code RUN_ERROR} reach the client.
 *
 * <p>
 * Some upstream tool-callback implementations (notably AG-UI's passthrough {@code ToolMapper#toSpringTool} and a few
 * Spring AI community tools) return {@code ""} for valid-but-uninteresting cases. This wrapper substitutes a structured
 * {@code {"result":"<tool> returned no content"}} envelope when that happens so the agent loop survives and the LLM
 * gets a usable signal. The substitution is logged at WARN with the tool name so the underlying empty-return path can
 * be investigated.
 * </p>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class NonEmptyToolCallback implements ToolCallback {

    private static final Logger log = LoggerFactory.getLogger(NonEmptyToolCallback.class);

    /**
     * Sentinel content the agent loop receives instead of an empty string. JSON-shaped so an LLM that consumes the tool
     * result as JSON doesn't trip on a bare placeholder; {@code result} (not {@code error}) so the LLM doesn't
     * misinterpret an uninteresting empty result as a hard failure.
     */
    private static final String EMPTY_RESULT_PLACEHOLDER = "{\"result\":\"no content\"}";

    private final ToolCallback delegate;
    private final String toolName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    private NonEmptyToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
        this.toolName = delegate.getToolDefinition()
            .name();
    }

    /**
     * Wraps {@code delegate} so callers can chain registrations without an instanceof check. Returns the delegate
     * unchanged when it's already wrapped — re-wrapping would log misleading WARN messages attributing the substitution
     * to the outer wrapper instead of the inner offender.
     */
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
            "Tool '{}' returned {} — substituting placeholder so the Anthropic Messages API does not reject the next "
                + "agent iteration with empty user-message content. Investigate the underlying callback to see why "
                + "it returned no content; the LLM will see the placeholder in this turn's history.",
            toolName, result == null ? "null" : "empty/whitespace");

        return EMPTY_RESULT_PLACEHOLDER;
    }
}
