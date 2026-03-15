/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.remote.client.service;

import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpIntegrationInstanceConfigurationWorkflowServiceClient
    implements McpIntegrationInstanceConfigurationWorkflowService {

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        create(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        create(Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long mcpIntegrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void deleteByIntegrationInstanceConfigurationWorkflowId(long integrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByIntegrationInstanceConfigurationWorkflowId(
            long integrationInstanceConfigurationWorkflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpIntegrationInstanceConfigurationWorkflow>
        fetchMcpIntegrationInstanceConfigurationWorkflowByWorkflowId(String workflowId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            Long mcpIntegrationInstanceConfigurationId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows() {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow
        update(McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {
        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow update(
        long id, Long mcpIntegrationInstanceConfigurationId, Long integrationInstanceConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public McpIntegrationInstanceConfigurationWorkflow updateParameters(long id, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
}
