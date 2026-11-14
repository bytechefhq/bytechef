/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpAutomationRows {

    void addMissingRowsForLink(long mcpAutomationProjectConfigurationId);

    void addMissingRowsForReference(long connectedUserProjectWorkflowId);

    void provisionMcpServer(long connectedUserId, long mcpServerId);

    void remapConnectedUser(
        long connectedUserId, long previousAutomationProjectConfigurationId, long automationProjectConfigurationId);
}
