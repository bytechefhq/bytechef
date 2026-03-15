/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.Map;
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
class McpIntegrationInstanceWorkflowFacadeImpl implements McpIntegrationInstanceWorkflowFacade {

    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceWorkflowFacadeImpl(
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @Override
    public void enableMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, boolean enable) {

        long integrationInstanceConfigurationWorkflowId =
            getIntegrationInstanceConfigurationWorkflowId(integrationInstanceId, workflowUuid);

        integrationInstanceWorkflowService
            .fetchIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId)
            .ifPresentOrElse(
                integrationInstanceWorkflow -> integrationInstanceWorkflowService
                    .updateEnabled(integrationInstanceWorkflow.getId(), enable),
                () -> {
                    IntegrationInstanceWorkflow integrationInstanceWorkflow =
                        integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                            integrationInstanceId, integrationInstanceConfigurationWorkflowId);

                    if (enable) {
                        integrationInstanceWorkflowService.updateEnabled(
                            integrationInstanceWorkflow.getId(), true);
                    }
                });
    }

    @Override
    public void updateMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, Map<String, Object> inputs) {

        long integrationInstanceConfigurationWorkflowId =
            getIntegrationInstanceConfigurationWorkflowId(integrationInstanceId, workflowUuid);

        integrationInstanceWorkflowService
            .fetchIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId)
            .ifPresentOrElse(
                integrationInstanceWorkflow -> {
                    integrationInstanceWorkflow.setInputs(inputs);

                    integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
                },
                () -> {
                    IntegrationInstanceWorkflow integrationInstanceWorkflow =
                        integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                            integrationInstanceId, integrationInstanceConfigurationWorkflowId);

                    integrationInstanceWorkflow.setInputs(inputs);

                    integrationInstanceWorkflowService.update(integrationInstanceWorkflow);
                });
    }

    private long getIntegrationInstanceConfigurationWorkflowId(
        long integrationInstanceId, String workflowUuid) {

        String workflowId = integrationWorkflowService.getWorkflowId(integrationInstanceId, workflowUuid);

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService
                .fetchMcpIntegrationInstanceConfigurationWorkflowByWorkflowId(workflowId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "MCP integration workflow not found for workflow: " + workflowUuid));

        return mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();
    }
}
