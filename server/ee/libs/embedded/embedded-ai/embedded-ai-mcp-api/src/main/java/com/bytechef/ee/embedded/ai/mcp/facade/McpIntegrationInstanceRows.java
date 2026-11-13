/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import java.util.Set;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceRows {

    void addMissingRows(long connectedUserId, long mcpServerId);

    void addMissingRowsForConnectedUsers(long mcpServerId);

    void addMissingRowsForIntegrationInstance(long integrationInstanceId);

    void addMissingRowsForIntegrationInstanceConfiguration(long integrationInstanceConfigurationId);

    Set<Long> getConnectedUserIds(long mcpServerId);

    Set<Long> getMcpServerIds(long connectedUserId);

    void provisionMcpServer(long connectedUserId, long mcpServerId);

    void remapIntegrationInstance(long integrationInstanceId);
}
