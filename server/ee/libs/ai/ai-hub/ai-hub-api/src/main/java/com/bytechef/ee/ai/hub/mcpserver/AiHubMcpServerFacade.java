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
 * @version ee
 * @author Ivica Cardic
 */
public interface AiHubMcpServerFacade {

    long addMcpServer(
        long userId, long workspaceId, int environment, String name, String url, @Nullable String authToken);

    List<AiHubMcpServer> listMcpServers(long userId, long workspaceId);

    boolean removeMcpServer(long userId, long workspaceId, long mcpServerId);

    boolean setMcpServerEnabled(long userId, long workspaceId, long mcpServerId, boolean enabled);

    List<McpServerToolInfo> listMcpServerTools(long userId, long workspaceId, long mcpServerId);

    boolean setMcpServerToolEnabled(long userId, long workspaceId, long mcpServerId, String toolName, boolean enabled);

    record McpServerToolInfo(String name, @Nullable String description, boolean enabled) {
    }
}
