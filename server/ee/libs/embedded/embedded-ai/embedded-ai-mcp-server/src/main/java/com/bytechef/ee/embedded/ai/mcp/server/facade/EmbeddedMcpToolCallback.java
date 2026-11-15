/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.server.facade;

import java.util.Map;
import org.jspecify.annotations.Nullable;
import org.springframework.ai.chat.model.ToolContext;
import org.springframework.ai.tool.ToolCallback;
import org.springframework.ai.tool.definition.ToolDefinition;
import org.springframework.ai.tool.metadata.ToolMetadata;

/**
 * A tool callback with the MCP {@code _meta} the embedded MCP server lists alongside the tool, so a client can tell
 * which integration a tool belongs to before calling it.
 *
 * @param delegate the tool callback that does the work
 * @param meta     the MCP {@code _meta} entries of the tool
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public record EmbeddedMcpToolCallback(ToolCallback delegate, Map<String, Object> meta) implements ToolCallback {

    /**
     * The {@code _meta} key holding the id of the integration a tool belongs to.
     */
    public static final String INTEGRATION_ID_META_KEY = "bytechef.io/integrationId";

    public EmbeddedMcpToolCallback {
        meta = Map.copyOf(meta);
    }

    public static EmbeddedMcpToolCallback ofIntegration(ToolCallback delegate, long integrationId) {
        return new EmbeddedMcpToolCallback(delegate, Map.of(INTEGRATION_ID_META_KEY, integrationId));
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
        return delegate.call(toolInput);
    }

    @Override
    public String call(String toolInput, @Nullable ToolContext toolContext) {
        return delegate.call(toolInput, toolContext);
    }
}
