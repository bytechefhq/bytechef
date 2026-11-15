/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.facade;

import com.bytechef.ee.embedded.ai.mcp.facade.McpAutomationRowFacade;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpAutomationRowFacadeClient implements McpAutomationRowFacade {

    @Override
    public void addMissingRowsForLink(long mcpAutomationProjectConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void addMissingRowsForLinkWorkflows(
        long mcpAutomationProjectConfigurationId, List<Long> mcpAutomationProjectConfigurationWorkflowIds) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void addMissingRowsForReference(long connectedUserProjectWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void provisionMcpServer(long connectedUserId, long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void remapConnectedUser(
        long connectedUserId, long previousAutomationProjectConfigurationId, long automationProjectConfigurationId) {

        throw new UnsupportedOperationException();
    }
}
