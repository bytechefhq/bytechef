/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceWorkflowServiceClient implements McpIntegrationInstanceWorkflowService {

    @Override
    public McpIntegrationInstanceWorkflow createMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId, boolean enabled) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceWorkflow> fetchMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Long> getIntegrationInstanceIdsByMcpServerId(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceWorkflow getMcpIntegrationInstanceWorkflow(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceWorkflow> getMcpIntegrationInstanceWorkflows(long integrationInstanceId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
