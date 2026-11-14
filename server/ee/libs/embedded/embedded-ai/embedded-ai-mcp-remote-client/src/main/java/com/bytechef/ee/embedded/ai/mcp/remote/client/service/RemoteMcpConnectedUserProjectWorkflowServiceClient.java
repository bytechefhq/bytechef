/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.remote.client.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import java.util.List;
import java.util.Optional;
import java.util.Set;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
@ConditionalOnEEVersion
public class RemoteMcpConnectedUserProjectWorkflowServiceClient implements McpConnectedUserProjectWorkflowService {

    @Override
    public McpConnectedUserProjectWorkflow create(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId, boolean enabled) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpConnectedUserProjectWorkflow> fetch(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<McpConnectedUserProjectWorkflow> fetch(
        long connectedUserProjectWorkflowId, long mcpAutomationProjectConfigurationWorkflowId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Set<Long> getConnectedUserIdsByMcpServerId(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpConnectedUserProjectWorkflow> getConnectedUserMcpConnectedUserProjectWorkflows(
        long connectedUserId) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<McpConnectedUserProjectWorkflow> getMcpServerMcpConnectedUserProjectWorkflows(long mcpServerId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void updateEnabled(long id, boolean enabled) {
        throw new UnsupportedOperationException();
    }
}
