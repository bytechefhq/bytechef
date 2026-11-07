/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

import com.agui.spring.ai.AgUiFunctionToolCallback;
import com.bytechef.ee.ai.hub.progress.SubagentProgressEmitter.Phase;
import java.util.HashMap;
import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentProgressScope {

    public static final String TOOL_CONTEXT_KEY = "aiHubSubagentProgressScope";

    static final int MAX_STEPS = 100;

    private final SubagentProgressEmitter emitter;
    private final Object lock = new Object();
    private final String subagentName;
    private final @Nullable String toolCallId;

    private boolean closed;
    private @Nullable String lastStep;
    private int stepCount;

    private SubagentProgressScope(
        SubagentProgressEmitter emitter, @Nullable String toolCallId, String subagentName) {

        this.emitter = emitter;
        this.subagentName = subagentName;
        this.toolCallId = toolCallId;
    }

    static SubagentProgressScope open(
        SubagentProgressEmitter emitter, @Nullable ToolContext toolContext, String subagentName, String label) {

        SubagentProgressScope scope = new SubagentProgressScope(emitter, fetchToolCallId(toolContext), subagentName);

        emitter.emit(scope.toolCallId, subagentName, Phase.STARTED, label);

        return scope;
    }

    public static @Nullable SubagentProgressScope fetch(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        return context.get(TOOL_CONTEXT_KEY) instanceof SubagentProgressScope scope ? scope : null;
    }

    public void step(String text) {
        synchronized (lock) {
            if (closed || text.isBlank() || text.equals(lastStep) || stepCount >= MAX_STEPS) {
                return;
            }

            lastStep = text;
            stepCount++;

            emitter.emit(toolCallId, subagentName, Phase.STEP, text);
        }
    }

    void close(boolean failed) {
        synchronized (lock) {
            if (closed) {
                return;
            }

            closed = true;

            emitter.emit(toolCallId, subagentName, failed ? Phase.FAILED : Phase.DONE, failed ? "failed" : "done");
        }
    }

    ToolContext attachTo(@Nullable ToolContext toolContext) {
        Map<String, Object> context = new HashMap<>();

        if (toolContext != null) {
            context.putAll(toolContext.getContext());
        }

        context.put(TOOL_CONTEXT_KEY, this);

        return new ToolContext(context);
    }

    @Override
    public String toString() {
        return "SubagentProgressScope{subagentName=" + subagentName + "}";
    }

    private static @Nullable String fetchToolCallId(@Nullable ToolContext toolContext) {
        if (toolContext == null) {
            return null;
        }

        Map<String, Object> context = toolContext.getContext();

        return context.get(AgUiFunctionToolCallback.TOOL_CALL_ID_KEY) instanceof String toolCallId ? toolCallId : null;
    }
}
