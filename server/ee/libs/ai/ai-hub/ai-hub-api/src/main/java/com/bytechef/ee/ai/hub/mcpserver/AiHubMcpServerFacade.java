/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.ai.hub.mcpserver;

import java.util.List;
import org.jspecify.annotations.Nullable;

/**
 * Facade for managing a user's external MCP server registrations. The optional bearer token is encrypted at rest by the
 * implementation; callers pass and never receive the plaintext token through the GraphQL surface.
 *
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface AiHubMcpServerFacade {

    /**
     * Register a new MCP server for the user. The {@code authToken} (if present) is encrypted before persistence.
     * Returns the new server id.
     */
    long addMcpServer(
        long userId, long workspaceId, int environment, String name, String url, @Nullable String authToken);

    /**
     * The user's registered MCP servers (scoped by user + workspace).
     */
    List<AiHubMcpServer> listMcpServers(long userId, long workspaceId);

    /**
     * Remove an MCP server registration. Idempotent: removing a non-existent id is a no-op.
     */
    void removeMcpServer(long mcpServerId);

    /**
     * Toggle an MCP server on/off. Throws if the id doesn't exist.
     */
    void setMcpServerEnabled(long mcpServerId, boolean enabled);

    /**
     * The server's tools discovered live from the server, each joined with its persisted enabled state (a tool with no
     * stored row is enabled). Throws when the server is unreachable.
     */
    List<McpServerToolInfo> listMcpServerTools(long mcpServerId);

    /**
     * Toggle a single tool of an MCP server on/off. Upserts a per-tool row carrying the enabled flag.
     */
    void setMcpServerToolEnabled(long mcpServerId, String toolName, boolean enabled);

    /**
     * A discovered MCP tool plus its persisted on/off state.
     */
    record McpServerToolInfo(String name, @Nullable String description, boolean enabled) {
    }
}
