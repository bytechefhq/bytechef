/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.service;

import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import java.util.List;
import java.util.Optional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
public interface McpIntegrationInstanceWorkflowService {

    McpIntegrationInstanceWorkflow createMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId, boolean enabled);

    void delete(long id);

    Optional<McpIntegrationInstanceWorkflow> fetchMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, long mcpIntegrationInstanceConfigurationWorkflowId);

    List<Long> getIntegrationInstanceIdsByMcpServerId(long mcpServerId);

    McpIntegrationInstanceWorkflow getMcpIntegrationInstanceWorkflow(long id);

    List<McpIntegrationInstanceWorkflow> getMcpIntegrationInstanceWorkflows(long integrationInstanceId);

    void updateEnabled(long id, boolean enabled);
}
