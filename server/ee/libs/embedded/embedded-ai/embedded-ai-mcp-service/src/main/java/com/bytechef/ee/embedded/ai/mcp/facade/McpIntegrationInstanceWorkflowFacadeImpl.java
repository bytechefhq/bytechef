/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.ee.embedded.ai.mcp.audit.McpIntegrationInstanceAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.facade.ConnectedUserIntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.security.access.prepost.PreAuthorize;
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

    private final ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceWorkflowFacadeImpl(
        ConnectedUserIntegrationInstanceFacade connectedUserIntegrationInstanceFacade,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService) {

        this.connectedUserIntegrationInstanceFacade = connectedUserIntegrationInstanceFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceWorkflowService = mcpIntegrationInstanceWorkflowService;
    }

    @Override
    @Audited(event = McpIntegrationInstanceAuditEvents.MCP_INTEGRATION_INSTANCE_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "integrationInstanceId", value = "#integrationInstanceId"),
        @AuditData(key = "workflowUuid", value = "#workflowUuid"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("isTenantAdmin() or isConnectedUser()")
    public void enableMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, boolean enable) {

        connectedUserIntegrationInstanceFacade.validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : getMcpIntegrationInstanceConfigurationWorkflows(
            integrationInstanceId, workflowUuid)) {

            long mcpIntegrationInstanceConfigurationWorkflowId = mcpIntegrationInstanceConfigurationWorkflow.getId();

            Optional<McpIntegrationInstanceWorkflow> mcpIntegrationInstanceWorkflowOptional =
                mcpIntegrationInstanceWorkflowService.fetchMcpIntegrationInstanceWorkflow(
                    integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId);

            if (mcpIntegrationInstanceWorkflowOptional.isPresent()) {
                McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow =
                    mcpIntegrationInstanceWorkflowOptional.get();

                mcpIntegrationInstanceWorkflowService.updateEnabled(mcpIntegrationInstanceWorkflow.getId(), enable);
            } else {
                mcpIntegrationInstanceWorkflowService.createMcpIntegrationInstanceWorkflow(
                    integrationInstanceId, mcpIntegrationInstanceConfigurationWorkflowId, enable);
            }

            long integrationInstanceConfigurationWorkflowId =
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();

            if (integrationInstanceWorkflowService
                .fetchIntegrationInstanceWorkflow(integrationInstanceId, integrationInstanceConfigurationWorkflowId)
                .isEmpty()) {

                integrationInstanceWorkflowService.createIntegrationInstanceWorkflow(
                    integrationInstanceId, integrationInstanceConfigurationWorkflowId);
            }
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isConnectedUser()")
    public void updateMcpIntegrationInstanceWorkflow(
        long integrationInstanceId, String workflowUuid, Map<String, Object> inputs) {

        connectedUserIntegrationInstanceFacade.validateCurrentPrincipalIntegrationInstanceOwnership(
            integrationInstanceId);

        List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
            getMcpIntegrationInstanceConfigurationWorkflows(integrationInstanceId, workflowUuid);

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflows.getFirst();

        long integrationInstanceConfigurationWorkflowId =
            mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();

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

    private List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceId, String workflowUuid) {

        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        String workflowId = integrationWorkflowService.getWorkflowId(integrationInstanceId, workflowUuid);

        List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
            mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows(
                integrationInstance.getIntegrationInstanceConfigurationId(), workflowId);

        if (mcpIntegrationInstanceConfigurationWorkflows.isEmpty()) {
            throw new IllegalArgumentException("MCP integration workflow not found for workflow: " + workflowUuid);
        }

        return mcpIntegrationInstanceConfigurationWorkflows;
    }
}
