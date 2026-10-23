/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
import org.springframework.context.annotation.Lazy;
import org.springframework.security.access.AccessDeniedException;
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
class McpIntegrationInstanceConfigurationWorkflowFacadeImpl
    implements McpIntegrationInstanceConfigurationWorkflowFacade {

    private final EmbeddedMcpReader embeddedMcpReader;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpServerService mcpServerService;
    private final PermissionService permissionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceConfigurationWorkflowFacadeImpl(
        EmbeddedMcpReader embeddedMcpReader,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpServerService mcpServerService, @Lazy PermissionService permissionService,
        WorkflowService workflowService) {

        this.embeddedMcpReader = embeddedMcpReader;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.permissionService = permissionService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationWorkflowId, " +
        "'McpIntegrationInstanceConfigurationWorkflow', 'INTEGRATION_INSTANCE_CONFIGURATION_DELETE')")
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

    @Override
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfigurationWorkflow createMcpIntegrationInstanceConfigurationWorkflow(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId) {

        checkSameEnvironment(mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        return mcpIntegrationInstanceConfigurationWorkflowService.create(
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpIntegrationInstanceConfigurationWorkflow', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId) {

        if (mcpIntegrationInstanceConfigurationId != null && !permissionService.hasResourceScope(
            mcpIntegrationInstanceConfigurationId, "McpIntegrationInstanceConfiguration",
            "INTEGRATION_INSTANCE_CONFIGURATION_EDIT")) {

            throw new AccessDeniedException("Access Denied");
        }

        if (mcpIntegrationInstanceConfigurationId != null || integrationInstanceConfigurationWorkflowId != null) {
            checkUpdateTargetEnvironment(
                id, mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
        }

        return mcpIntegrationInstanceConfigurationWorkflowService.update(
            id, mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpIntegrationInstanceConfigurationWorkflow', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters) {

        return mcpIntegrationInstanceConfigurationWorkflowService.updateParameters(id, parameters);
    }

    @Override
    @PreAuthorize("hasPermission(#id, 'McpIntegrationInstanceConfigurationWorkflow', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public McpIntegrationInstanceConfigurationWorkflow getMcpIntegrationInstanceConfigurationWorkflow(long id) {
        return mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(id)
            .orElse(null);
    }

    @Override
    @PreAuthorize("hasEmbeddedScope('INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows() {
        Map<Long, Environment> environmentsByMcpServerId = new HashMap<>();

        for (McpServer mcpServer : mcpServerService.getMcpServers(PlatformType.EMBEDDED)) {
            environmentsByMcpServerId.put(mcpServer.getId(), mcpServer.getEnvironment());
        }

        Map<Long, Long> mcpServerIdsByMcpIntegrationInstanceConfigurationId = new HashMap<>();

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurations()) {

            mcpServerIdsByMcpIntegrationInstanceConfigurationId.put(
                mcpIntegrationInstanceConfiguration.getId(), mcpIntegrationInstanceConfiguration.getMcpServerId());
        }

        return mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows()
            .stream()
            .filter(
                mcpIntegrationInstanceConfigurationWorkflow -> permissionService.hasEmbeddedScope(
                    "INTEGRATION_INSTANCE_CONFIGURATION_VIEW",
                    environmentsByMcpServerId.get(
                        mcpServerIdsByMcpIntegrationInstanceConfigurationId.get(
                            mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId()))))
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            long mcpIntegrationInstanceConfigurationId) {

        return mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfigurationId);
    }

    @Override
    @PreAuthorize("hasPermission(#integrationId, 'Integration', 'WORKFLOW_VIEW')")
    public List<IntegrationWorkflowDTO> getToolEligibleIntegrationVersionWorkflows(
        long integrationId, int integrationVersion) {

        return integrationWorkflowService.getIntegrationWorkflows(integrationId, integrationVersion)
            .stream()
            .map(integrationWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

                return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
            })
            .filter(integrationWorkflowDTO -> getToolCallableTrigger(integrationWorkflowDTO.getWorkflow()) != null)
            .toList();
    }

    @Override
    @PreAuthorize("hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_VIEW')")
    public List<IntegrationWorkflowDTO> getToolEligibleIntegrationInstanceConfigurationWorkflows(
        long integrationInstanceConfigurationId) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        return integrationWorkflowService
            .getIntegrationWorkflows(
                integrationInstanceConfiguration.getIntegrationId(),
                integrationInstanceConfiguration.getIntegrationVersion())
            .stream()
            .map(integrationWorkflow -> {
                Workflow workflow = workflowService.getWorkflow(integrationWorkflow.getWorkflowId());

                return new IntegrationWorkflowDTO(workflow, integrationWorkflow);
            })
            .filter(integrationWorkflowDTO -> getToolCallableTrigger(integrationWorkflowDTO.getWorkflow()) != null)
            .toList();
    }

    private void checkSameEnvironment(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService
                .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "McpIntegrationInstanceConfiguration not found: " + mcpIntegrationInstanceConfigurationId));

        McpServer mcpServer = embeddedMcpReader.getMcpServer(mcpIntegrationInstanceConfiguration.getMcpServerId());

        IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
            integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                integrationInstanceConfigurationWorkflowId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationId());

        if (mcpServer.getEnvironment() != integrationInstanceConfiguration.getEnvironment()) {
            throw new IllegalArgumentException(
                "MCP server %s (%s) and integration instance configuration %s (%s) are in different environments"
                    .formatted(
                        mcpServer.getId(), mcpServer.getEnvironment(), integrationInstanceConfiguration.getId(),
                        integrationInstanceConfiguration.getEnvironment()));
        }
    }

    private void checkUpdateTargetEnvironment(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationInstanceConfigurationWorkflow existingMcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(id)
                .orElseThrow(
                    () -> new IllegalArgumentException(
                        "McpIntegrationInstanceConfigurationWorkflow not found: " + id));

        long targetMcpIntegrationInstanceConfigurationId = mcpIntegrationInstanceConfigurationId == null
            ? existingMcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId()
            : mcpIntegrationInstanceConfigurationId;
        long targetIntegrationInstanceConfigurationWorkflowId = integrationInstanceConfigurationWorkflowId == null
            ? existingMcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId()
            : integrationInstanceConfigurationWorkflowId;

        checkSameEnvironment(
            targetMcpIntegrationInstanceConfigurationId, targetIntegrationInstanceConfigurationWorkflowId);
    }

    private static @Nullable WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
