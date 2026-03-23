/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.mcp.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
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
public class McpIntegrationInstanceConfigurationFacadeImpl implements McpIntegrationInstanceConfigurationFacade {

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationFacadeImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
    }

    @Override
    public McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration(integrationInstanceConfigurationId, mcpServerId);

        mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService.create(mcpIntegrationInstanceConfiguration);

        List<IntegrationInstanceConfigurationWorkflow> existingWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        Map<String, IntegrationInstanceConfigurationWorkflow> existingWorkflowsByWorkflowId = new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow existingWorkflow : existingWorkflows) {
            existingWorkflowsByWorkflowId.put(existingWorkflow.getWorkflowId(), existingWorkflow);
        }

        for (String workflowId : selectedWorkflowIds) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                existingWorkflowsByWorkflowId.get(workflowId);

            if (integrationInstanceConfigurationWorkflow == null) {
                integrationInstanceConfigurationWorkflow = new IntegrationInstanceConfigurationWorkflow();

                integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                    integrationInstanceConfigurationId);
                integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);
                integrationInstanceConfigurationWorkflow.setEnabled(true);
                integrationInstanceConfigurationWorkflow.setInputs(Map.of());

                integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
            } else if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                integrationInstanceConfigurationWorkflowService.updateEnabled(
                    integrationInstanceConfigurationWorkflow.getId(), true);
            }

            mcpIntegrationInstanceConfigurationWorkflowService.create(
                mcpIntegrationInstanceConfiguration.getId(), integrationInstanceConfigurationWorkflow.getId());
        }

        return mcpIntegrationInstanceConfiguration;
    }

    @Override
    public void deleteMcpIntegrationInstanceConfiguration(long mcpIntegrationInstanceConfigurationId) {
        mcpIntegrationInstanceConfigurationService
            .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationId)
            .orElseThrow(() -> new IllegalArgumentException(
                "McpIntegrationInstanceConfiguration not found: " + mcpIntegrationInstanceConfigurationId));

        List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
            mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfigurationId);

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflows) {
            long integrationInstanceConfigurationWorkflowId =
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();

            integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                integrationInstanceConfigurationWorkflowId);

            mcpIntegrationInstanceConfigurationWorkflowService
                .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());

            integrationInstanceConfigurationWorkflowService.updateEnabled(
                integrationInstanceConfigurationWorkflowId, false);
        }

        mcpIntegrationInstanceConfigurationService.delete(mcpIntegrationInstanceConfigurationId);
    }

    @Override
    public McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService
                .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "McpIntegrationInstanceConfiguration not found: " + mcpIntegrationInstanceConfigurationId));

        List<McpIntegrationInstanceConfigurationWorkflow> existingMcpIntegrationInstanceConfigurationWorkflows =
            mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfigurationId);

        List<IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());

        Map<Long, IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflowMap =
            new HashMap<>();
        Map<String, IntegrationInstanceConfigurationWorkflow> integrationInstanceConfigurationWorkflowByWorkflowIdMap =
            new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow : integrationInstanceConfigurationWorkflows) {

            integrationInstanceConfigurationWorkflowMap.put(
                integrationInstanceConfigurationWorkflow.getId(), integrationInstanceConfigurationWorkflow);
            integrationInstanceConfigurationWorkflowByWorkflowIdMap.put(
                integrationInstanceConfigurationWorkflow.getWorkflowId(), integrationInstanceConfigurationWorkflow);
        }

        Map<String, McpIntegrationInstanceConfigurationWorkflow> existingWorkflowIdMap = new HashMap<>();

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : existingMcpIntegrationInstanceConfigurationWorkflows) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowMap.get(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (integrationInstanceConfigurationWorkflow != null) {
                existingWorkflowIdMap.put(
                    integrationInstanceConfigurationWorkflow.getWorkflowId(),
                    mcpIntegrationInstanceConfigurationWorkflow);
            }
        }

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowByWorkflowIdMap.get(workflowId);

                if (integrationInstanceConfigurationWorkflow == null) {
                    integrationInstanceConfigurationWorkflow = new IntegrationInstanceConfigurationWorkflow();

                    integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                        mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());
                    integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);
                    integrationInstanceConfigurationWorkflow.setEnabled(true);
                    integrationInstanceConfigurationWorkflow.setInputs(Map.of());

                    integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.create(
                            integrationInstanceConfigurationWorkflow);
                } else if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                    integrationInstanceConfigurationWorkflowService.updateEnabled(
                        integrationInstanceConfigurationWorkflow.getId(), true);
                }

                mcpIntegrationInstanceConfigurationWorkflowService.create(
                    mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId());
            }
        }

        for (Map.Entry<String, McpIntegrationInstanceConfigurationWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                    entry.getValue();

                long integrationInstanceConfigurationWorkflowId =
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId();

                integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                    integrationInstanceConfigurationWorkflowId);

                mcpIntegrationInstanceConfigurationWorkflowService
                    .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());

                integrationInstanceConfigurationWorkflowService.updateEnabled(
                    integrationInstanceConfigurationWorkflowId, false);
            }
        }

        return mcpIntegrationInstanceConfiguration;
    }

    @Override
    public void updateMcpIntegrationInstanceConfigurationVersion(
        long mcpIntegrationInstanceConfigurationId, int integrationVersion, List<String> workflowUuids) {

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService
                .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "McpIntegrationInstanceConfiguration not found: " + mcpIntegrationInstanceConfigurationId));

        long integrationInstanceConfigurationId =
            mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId();

        List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
            mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfigurationId);

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflows) {
            integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            mcpIntegrationInstanceConfigurationWorkflowService
                .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());
        }

        List<IntegrationInstanceConfigurationWorkflow> oldIntegrationInstanceConfigurationWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        for (IntegrationInstanceConfigurationWorkflow oldWorkflow : oldIntegrationInstanceConfigurationWorkflows) {
            integrationInstanceWorkflowService.deleteByIntegrationInstanceConfigurationWorkflowId(
                oldWorkflow.getId());

            integrationInstanceConfigurationWorkflowService.delete(oldWorkflow.getId());
        }

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        integrationInstanceConfiguration.setIntegrationVersion(integrationVersion);

        integrationInstanceConfigurationService.update(integrationInstanceConfiguration);

        IntegrationInstanceConfiguration updatedIntegrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        long integrationId = updatedIntegrationInstanceConfiguration.getIntegrationId();

        List<IntegrationWorkflow> integrationWorkflows =
            integrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion);

        Set<String> workflowUuidSet = new HashSet<>(workflowUuids);

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {
            if (workflowUuidSet.contains(integrationWorkflow.getUuidAsString())) {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    new IntegrationInstanceConfigurationWorkflow();

                integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                    integrationInstanceConfigurationId);
                integrationInstanceConfigurationWorkflow.setWorkflowId(integrationWorkflow.getWorkflowId());
                integrationInstanceConfigurationWorkflow.setEnabled(true);
                integrationInstanceConfigurationWorkflow.setInputs(Map.of());

                integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);

                mcpIntegrationInstanceConfigurationWorkflowService.create(
                    mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId());
            }
        }
    }
}
