/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.progress;

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
public final class SubagentStepToolCallback implements ToolCallback {

    private final ToolCallback delegate;

    private SubagentStepToolCallback(ToolCallback delegate) {
        this.delegate = delegate;
    }

    public static ToolCallback wrap(ToolCallback delegate) {
        if (delegate instanceof SubagentStepToolCallback) {
            return delegate;
        }

        return new SubagentStepToolCallback(delegate);
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
        SubagentProgressScope scope = SubagentProgressScope.fetch(toolContext);

        if (scope != null) {
            ToolDefinition toolDefinition = delegate.getToolDefinition();

            scope.step(SubagentProgressText.describeStep(toolDefinition.name(), toolInput));
        }

        return delegate.call(toolInput, toolContext);
    }
}
