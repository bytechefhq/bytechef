/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link McpIntegrationWorkflowFacade}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
class McpIntegrationWorkflowFacadeImpl implements McpIntegrationWorkflowFacade {

    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationWorkflowFacadeImpl(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        McpIntegrationWorkflowService mcpIntegrationWorkflowService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
    }

    @Override
    public void deleteMcpIntegrationWorkflow(long mcpIntegrationWorkflowId) {
        McpIntegrationWorkflow mcpIntegrationWorkflow =
            mcpIntegrationWorkflowService.fetchMcpIntegrationWorkflow(mcpIntegrationWorkflowId)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "McpIntegrationWorkflow not found: " + mcpIntegrationWorkflowId));

        mcpIntegrationWorkflowService.delete(mcpIntegrationWorkflowId);

        integrationInstanceConfigurationWorkflowService.delete(
            mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
    }
}
