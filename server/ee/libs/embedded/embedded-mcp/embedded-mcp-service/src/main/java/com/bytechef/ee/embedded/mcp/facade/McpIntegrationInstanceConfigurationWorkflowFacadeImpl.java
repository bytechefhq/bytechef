/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
class McpIntegrationInstanceConfigurationWorkflowFacadeImpl
    implements McpIntegrationInstanceConfigurationWorkflowFacade {

    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceConfigurationWorkflowFacadeImpl(
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @Override
    public void deleteMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId) {
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService
                .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "McpIntegrationInstanceConfigurationWorkflow not found: "
                            + mcpIntegrationInstanceConfigurationWorkflowId));

        long integrationInstanceConfigurationWorkflowId =
            mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();

        integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
            integrationInstanceConfigurationWorkflowId);

        mcpIntegrationInstanceConfigurationWorkflowService.delete(mcpIntegrationInstanceConfigurationWorkflowId);

        integrationInstanceConfigurationWorkflowService.updateEnabled(integrationInstanceConfigurationWorkflowId,
            false);
    }
}
