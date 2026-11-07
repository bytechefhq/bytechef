/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public final class AiHubMcpToolCallback implements ToolCallback {

    private final ToolCallback delegate;
    private final long mcpServerId;
    private final boolean readOnly;

    @SuppressFBWarnings("EI_EXPOSE_REP2")
    public AiHubMcpToolCallback(ToolCallback delegate, long mcpServerId, boolean readOnly) {
        this.delegate = delegate;
        this.mcpServerId = mcpServerId;
        this.readOnly = readOnly;
    }

    @Override
    public ToolDefinition getToolDefinition() {
        return delegate.getToolDefinition();
    }

    public long getMcpServerId() {
        return mcpServerId;
    }

    public boolean isReadOnly() {
        return readOnly;
    }

    @Override
    public String call(String toolInput) {
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return delegate.call(toolInput, toolContext);
    }
}
