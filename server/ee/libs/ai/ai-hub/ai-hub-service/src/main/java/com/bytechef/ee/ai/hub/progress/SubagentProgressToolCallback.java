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
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class SubagentProgressToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final String subagentName;

    private SubagentProgressToolCallback(ToolCallback delegate, String subagentName) {
        this.delegate = delegate;
        this.subagentName = subagentName;
    }

    public static ToolCallback wrap(ToolCallback delegate) {
        ToolDefinition toolDefinition = delegate.getToolDefinition();

        return wrap(delegate, toolDefinition.name());
    }

    public static ToolCallback wrap(ToolCallback delegate, String subagentName) {
        if (delegate instanceof SubagentProgressToolCallback) {
            return delegate;
        }

        return new SubagentProgressToolCallback(delegate, subagentName);
    }

    @SuppressFBWarnings("EI_EXPOSE_REP")
    public ToolCallback getDelegate() {
        return delegate;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    @Override
    public ToolMetadata getToolMetadata() {
        return delegate.getToolMetadata();
    }

    @Override
    public String call(String toolInput) {
        return call(toolInput, null);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        SubagentProgressEmitter emitter = SubagentProgressEmitter.fetch(toolContext);

        if (emitter == null) {
            return delegate.call(toolInput, toolContext);
        }

        String label = SubagentProgressText.describeRequest(toolInput);

        SubagentProgressScope parentScope = SubagentProgressScope.fetch(toolContext);

        if (parentScope != null) {
            parentScope.step(label.isEmpty() ? subagentName : subagentName + ": " + label);

            return delegate.call(toolInput, toolContext);
        }

        SubagentProgressScope scope = SubagentProgressScope.open(emitter, toolContext, subagentName, label);

        boolean failed = true;

        try {
            String result = delegate.call(toolInput, scope.attachTo(toolContext));

            failed = isErrorResult(result);

            return result;
        } finally {
            scope.close(failed);
        }
    }

    private static boolean isErrorResult(@Nullable String result) {
        if (result == null) {
            return false;
        }

        String strippedResult = result.stripLeading();

        return strippedResult.startsWith("{\"error\"");
    }
}
