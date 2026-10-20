/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.audit.McpIntegrationInstanceConfigurationAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.dto.WorkflowToolParametersDTO;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
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
public class McpIntegrationInstanceConfigurationFacadeImpl implements McpIntegrationInstanceConfigurationFacade {

    private final EmbeddedMcpReader embeddedMcpReader;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade;
    private final McpServerService mcpServerService;
    private final PermissionService permissionService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationFacadeImpl(
        EmbeddedMcpReader embeddedMcpReader,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade, McpServerService mcpServerService,
        @Lazy PermissionService permissionService) {

        this.embeddedMcpReader = embeddedMcpReader;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceRowFacade = mcpIntegrationInstanceRowFacade;
        this.mcpServerService = mcpServerService;
        this.permissionService = permissionService;
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_CREATED, data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#result?.id"),
            @AuditData(key = "mcpServerId", value = "#mcpServerId"),
            @AuditData(key = "integrationInstanceConfigurationId", value = "#integrationInstanceConfigurationId"),
            @AuditData(key = "workflowIds", value = "#selectedWorkflowIds")
        })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_CREATE')")
    public McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        return createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, selectedWorkflowIds, List.of());
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_CREATED, data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#result?.id"),
            @AuditData(key = "mcpServerId", value = "#mcpServerId"),
            @AuditData(key = "integrationInstanceConfigurationId", value = "#integrationInstanceConfigurationId"),
            @AuditData(key = "workflowIds", value = "#selectedWorkflowIds")
        })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_CREATE')")
    public McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds,
        List<WorkflowToolParametersDTO> toolParameters) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        checkSameEnvironment(mcpServerId, integrationInstanceConfiguration);

        List<IntegrationInstanceConfigurationWorkflow> existingWorkflows =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflows(
                integrationInstanceConfigurationId);

        Map<String, IntegrationInstanceConfigurationWorkflow> existingWorkflowsByWorkflowId = new HashMap<>();

        for (IntegrationInstanceConfigurationWorkflow existingWorkflow : existingWorkflows) {
            existingWorkflowsByWorkflowId.put(existingWorkflow.getWorkflowId(), existingWorkflow);
        }

        for (String workflowId : selectedWorkflowIds) {
            requireEnabledWorkflow(
                integrationInstanceConfigurationId, existingWorkflowsByWorkflowId.get(workflowId), workflowId);
        }

        Map<String, Map<String, ?>> toolParametersMap = toToolParametersMap(toolParameters);

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            new McpIntegrationInstanceConfiguration(integrationInstanceConfigurationId, mcpServerId);

        mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService.create(mcpIntegrationInstanceConfiguration);

        for (String workflowId : selectedWorkflowIds) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                existingWorkflowsByWorkflowId.get(workflowId);

            McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                mcpIntegrationInstanceConfigurationWorkflowService.create(
                    mcpIntegrationInstanceConfiguration.getId(), integrationInstanceConfigurationWorkflow.getId());

            storeToolParameters(mcpIntegrationInstanceConfigurationWorkflow, toolParametersMap.get(workflowId));
        }

        mcpIntegrationInstanceRowFacade.addMissingRowsForConnectedUsers(mcpServerId);

        return mcpIntegrationInstanceConfiguration;
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_DELETED, data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#mcpIntegrationInstanceConfigurationId")
        })
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_DELETE')")
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
            mcpIntegrationInstanceConfigurationWorkflowService
                .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());
        }

        mcpIntegrationInstanceConfigurationService.delete(mcpIntegrationInstanceConfigurationId);
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_UPDATED, data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#mcpIntegrationInstanceConfigurationId"),
            @AuditData(key = "mcpServerId", value = "#result?.mcpServerId"),
            @AuditData(key = "workflowIds", value = "#selectedWorkflowIds")
        })
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        return updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfigurationId, selectedWorkflowIds, List.of());
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_UPDATED, data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#mcpIntegrationInstanceConfigurationId"),
            @AuditData(key = "mcpServerId", value = "#result?.mcpServerId"),
            @AuditData(key = "workflowIds", value = "#selectedWorkflowIds")
        })
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds,
        List<WorkflowToolParametersDTO> toolParameters) {

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

        Map<String, Map<String, ?>> toolParametersMap = toToolParametersMap(toolParameters);

        Set<String> selectedWorkflowIdSet = new HashSet<>(selectedWorkflowIds);

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                requireEnabledWorkflow(
                    mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId(),
                    integrationInstanceConfigurationWorkflowByWorkflowIdMap.get(workflowId), workflowId);
            }
        }

        for (String workflowId : selectedWorkflowIds) {
            if (!existingWorkflowIdMap.containsKey(workflowId)) {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowByWorkflowIdMap.get(workflowId);

                McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                    mcpIntegrationInstanceConfigurationWorkflowService.create(
                        mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflow.getId());

                storeToolParameters(mcpIntegrationInstanceConfigurationWorkflow, toolParametersMap.get(workflowId));
            } else {
                storeToolParameters(existingWorkflowIdMap.get(workflowId), toolParametersMap.get(workflowId));
            }
        }

        for (Map.Entry<String, McpIntegrationInstanceConfigurationWorkflow> entry : existingWorkflowIdMap.entrySet()) {
            if (!selectedWorkflowIdSet.contains(entry.getKey())) {
                McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                    entry.getValue();

                mcpIntegrationInstanceConfigurationWorkflowService
                    .delete(mcpIntegrationInstanceConfigurationWorkflow.getId());
            }
        }

        mcpIntegrationInstanceRowFacade
            .addMissingRowsForConnectedUsers(mcpIntegrationInstanceConfiguration.getMcpServerId());

        return mcpIntegrationInstanceConfiguration;
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public McpIntegrationInstanceConfiguration getMcpIntegrationInstanceConfiguration(long id) {
        return mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(id)
            .orElse(null);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<McpIntegrationInstanceConfiguration> getMcpIntegrationInstanceConfigurations() {
        Map<Long, Environment> environmentsByMcpServerId = new HashMap<>();

        for (McpServer mcpServer : mcpServerService.getMcpServers(PlatformType.EMBEDDED)) {
            environmentsByMcpServerId.put(mcpServer.getId(), mcpServer.getEnvironment());
        }

        return mcpIntegrationInstanceConfigurationService.getMcpIntegrationInstanceConfigurations()
            .stream()
            .filter(
                mcpIntegrationInstanceConfiguration -> permissionService.hasEmbeddedScope(
                    "INTEGRATION_INSTANCE_CONFIGURATION_VIEW",
                    environmentsByMcpServerId.get(mcpIntegrationInstanceConfiguration.getMcpServerId())))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<McpIntegrationInstanceConfiguration> getMcpServerMcpIntegrationInstanceConfigurations(
        long mcpServerId) {

        return mcpIntegrationInstanceConfigurationService.getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId);
    }

    private static void requireEnabledWorkflow(
        long integrationInstanceConfigurationId,
        @Nullable IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow,
        String workflowId) {

        if (integrationInstanceConfigurationWorkflow == null || !integrationInstanceConfigurationWorkflow.isEnabled()) {
            throw new IllegalArgumentException(
                "Workflow " + workflowId + " is turned off in integration instance configuration " +
                    integrationInstanceConfigurationId);
        }
    }

    private void storeToolParameters(
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow,
        @Nullable Map<String, ?> parameters) {

        if (parameters != null) {
            mcpIntegrationInstanceConfigurationWorkflowService.updateParameters(
                Objects.requireNonNull(mcpIntegrationInstanceConfigurationWorkflow.getId()), parameters);
        }
    }

    private void checkSameEnvironment(
        long mcpServerId, IntegrationInstanceConfiguration integrationInstanceConfiguration) {

        McpServer mcpServer = embeddedMcpReader.getMcpServer(mcpServerId);

        if (mcpServer.getEnvironment() != integrationInstanceConfiguration.getEnvironment()) {
            throw new IllegalArgumentException(
                "MCP server %s (%s) and integration instance configuration %s (%s) are in different environments"
                    .formatted(
                        mcpServerId, mcpServer.getEnvironment(), integrationInstanceConfiguration.getId(),
                        integrationInstanceConfiguration.getEnvironment()));
        }
    }

    private static Map<String, Map<String, ?>> toToolParametersMap(List<WorkflowToolParametersDTO> toolParameters) {
        Map<String, Map<String, ?>> toolParametersMap = new HashMap<>();

        for (WorkflowToolParametersDTO workflowToolParameters : toolParameters) {
            toolParametersMap.put(workflowToolParameters.workflowId(), workflowToolParameters.parameters());
        }

        return toolParametersMap;
    }
}
