/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.facade;

import com.bytechef.ee.embedded.ai.mcp.facade.McpIntegrationInstanceRows;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceRowsClient implements McpIntegrationInstanceRows {

    @Override
    public void addMissingRows(long connectedUserId, long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMissingRowsForConnectedUsers(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMissingRowsForIntegrationInstance(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMissingRowsForIntegrationInstanceConfiguration(long integrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getConnectedUserIds(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getMcpServerIds(long connectedUserId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void provisionMcpServer(long connectedUserId, long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remapIntegrationInstance(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }
}
