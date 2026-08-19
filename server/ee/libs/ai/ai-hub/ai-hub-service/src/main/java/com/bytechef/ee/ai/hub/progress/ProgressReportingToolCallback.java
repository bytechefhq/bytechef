/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * Decorating {@link ToolCallback} that emits subagent progress events via {@link SubagentProgressEmitter} immediately
 * before invoking the wrapped callback.
 *
 * <p>
 * The progress text is: {@code "<subagentName>: <first 80 chars of input>"}. This gives the user a real-time signal
 * that the parent agent has delegated work to a named subagent and shows the first few characters of the chat it was
 * given.
 * </p>
 *
 * <p>
 * Wrap each subagent {@link ToolCallback} with this class in the parent agent's configuration:
 * </p>
 *
 * <pre>{@code
 * toolCallbacks.add(new ProgressReportingToolCallback(
 *     ResearchConfiguration.createResearchToolCallback(researchChatClient), "research"));
 * }</pre>
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public class ProgressReportingToolCallback implements ToolCallback {

    private static final int INPUT_PREVIEW_LENGTH = 80;

    private final ToolCallback delegate;
    private final String subagentName;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public ProgressReportingToolCallback(ToolCallback delegate, String subagentName) {
        this.delegate = delegate;
        this.subagentName = subagentName;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        String inputPreview = toolInput != null && toolInput.length() > INPUT_PREVIEW_LENGTH
            ? toolInput.substring(0, INPUT_PREVIEW_LENGTH) + "…"
            : toolInput;

        SubagentProgressEmitter.emitProgress(subagentName, inputPreview != null ? inputPreview : "");

        // Emit a paired completion signal regardless of delegate outcome. Without this, a delegate that throws
        // (network / model / NPE) leaves the UI showing the subagent as still working — the "running" indicator
        // never decrements because the only emit was the start. Today every wrapped subagent catches RuntimeException
        // internally so this branch is unreachable, but the wrapper's contract should not depend on the delegate's
        // discipline; any future contributor decorating a non-protected callback would otherwise leave a perpetual
        // spinner. A `try/finally` (rather than `try/catch`) preserves the original throw semantics: we do not eat
        // the exception, only ensure the UI sees completion.
        boolean delegateThrew = false;

        try {
            return delegate.call(toolInput, toolContext);
        } catch (RuntimeException exception) {
            delegateThrew = true;

            throw exception;
        } finally {
            SubagentProgressEmitter.emitProgress(subagentName, delegateThrew ? "failed" : "done");
        }
    }
}
