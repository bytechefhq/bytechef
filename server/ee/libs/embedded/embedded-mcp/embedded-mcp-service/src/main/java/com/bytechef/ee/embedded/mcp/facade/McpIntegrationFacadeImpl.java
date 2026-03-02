/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationWorkflowService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.mcp.domain.McpServer;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Implementation of {@link McpIntegrationFacade}.
 *
 * @author Ivica Cardic
 * @version ee
 */
@Service
@Transactional
public class McpIntegrationFacadeImpl implements McpIntegrationFacade {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpIntegrationService mcpIntegrationService;
    private final McpIntegrationWorkflowService mcpIntegrationWorkflowService;

    @SuppressFBWarnings("EI")
    public McpIntegrationFacadeImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        McpIntegrationService mcpIntegrationService,
        McpIntegrationWorkflowService mcpIntegrationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationService = mcpIntegrationService;
        this.mcpIntegrationWorkflowService = mcpIntegrationWorkflowService;
    }

    @Override
    public McpIntegration createMcpIntegration(
        long mcpServerId, long integrationId, int integrationVersion, List<String> selectedWorkflowIds) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration = new IntegrationInstanceConfiguration();

        integrationInstanceConfiguration.setName(
            McpServer.MCP_SERVER_NAME_PREFIX + integrationId + "_v" + integrationVersion);
        integrationInstanceConfiguration.setIntegrationId(integrationId);
        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);
        integrationInstanceConfiguration.setEnvironment(Environment.DEVELOPMENT);
        integrationInstanceConfiguration.setEnabled(false);

        integrationInstanceConfiguration =
            integrationInstanceConfigurationService.create(integrationInstanceConfiguration);

        McpIntegration mcpIntegration =
            new McpIntegration(integrationInstanceConfiguration.getId(), mcpServerId);

        mcpIntegration = mcpIntegrationService.create(mcpIntegration);

        for (String workflowId : selectedWorkflowIds) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfiguration.getId());
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);
            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setInputs(Map.of());

            integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

            mcpIntegrationWorkflowService.create(
                mcpIntegration.getId(), integrationInstanceConfigurationWorkflow.getId());
        }

        return mcpIntegration;
    }

    @Override
    public void deleteMcpIntegration(long mcpIntegrationId) {
        McpIntegration mcpIntegration = mcpIntegrationService.fetchMcpIntegration(mcpIntegrationId)
            .orElseThrow(() -> new IllegalArgumentException("McpIntegration not found: " + mcpIntegrationId));

        List<McpIntegrationWorkflow> mcpIntegrationWorkflows =
            mcpIntegrationWorkflowService.getMcpIntegrationMcpIntegrationWorkflows(mcpIntegrationId);

        for (McpIntegrationWorkflow mcpIntegrationWorkflow : mcpIntegrationWorkflows) {
            mcpIntegrationWorkflowService.delete(mcpIntegrationWorkflow.getId());

            integrationInstanceConfigurationWorkflowService.delete(
                mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
        }

        mcpIntegrationService.delete(mcpIntegrationId);

        Long integrationInstanceConfigurationId = mcpIntegration.getIntegrationInstanceConfigurationId();

        if (integrationInstanceConfigurationId != null) {
            integrationInstanceConfigurationService.delete(integrationInstanceConfigurationId);
        }
    }

    @Override
    public McpIntegration updateMcpIntegration(long mcpIntegrationId, List<String> selectedWorkflowIds) {
        McpIntegration mcpIntegration = mcpIntegrationService.fetchMcpIntegration(mcpIntegrationId)
            .orElseThrow(() -> new IllegalArgumentException("McpIntegration not found: " + mcpIntegrationId));

        List<McpIntegrationWorkflow> existingMcpIntegrationWorkflows =
            mcpIntegrationWorkflowService.getMcpIntegrationMcpIntegrationWorkflows(mcpIntegrationId);

        Map<String, McpIntegrationWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (McpIntegrationWorkflow mcpIntegrationWorkflow : existingMcpIntegrationWorkflows) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            existingWorkflowIdMap.put(
                integrationInstanceConfigurationWorkflow.getWorkflowId(), mcpIntegrationWorkflow);
        }

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    new IntegrationInstanceConfigurationWorkflow();

                integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                    mcpIntegration.getIntegrationInstanceConfigurationId());
                integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);
                integrationInstanceConfigurationWorkflow.setEnabled(true);
                integrationInstanceConfigurationWorkflow.setInputs(Map.of());

                integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

                mcpIntegrationWorkflowService.create(
                    mcpIntegrationId, integrationInstanceConfigurationWorkflow.getId());
            }
        }

        for (Map.Entry<String, McpIntegrationWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                McpIntegrationWorkflow mcpIntegrationWorkflow = entry.getValue();

                mcpIntegrationWorkflowService.delete(mcpIntegrationWorkflow.getId());

                integrationInstanceConfigurationWorkflowService.delete(
                    mcpIntegrationWorkflow.getIntegrationInstanceConfigurationWorkflowId());
            }
        }

        return mcpIntegration;
    }
}
