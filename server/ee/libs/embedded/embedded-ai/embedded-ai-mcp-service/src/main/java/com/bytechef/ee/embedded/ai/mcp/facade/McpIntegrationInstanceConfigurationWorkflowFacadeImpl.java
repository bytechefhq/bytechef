/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import org.jspecify.annotations.Nullable;
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

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceConfigurationWorkflowFacadeImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        WorkflowService workflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow createMcpIntegrationInstanceConfigurationWorkflow(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId) {

        return mcpIntegrationInstanceConfigurationWorkflowService.create(
            mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId) {

        return mcpIntegrationInstanceConfigurationWorkflowService.update(
            id, mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters) {

        return mcpIntegrationInstanceConfigurationWorkflowService.updateParameters(id, parameters);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow getMcpIntegrationInstanceConfigurationWorkflow(long id) {
        return mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(id)
            .orElse(null);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpIntegrationInstanceConfigurationWorkflow> getMcpIntegrationInstanceConfigurationWorkflows() {
        return mcpIntegrationInstanceConfigurationWorkflowService.getMcpIntegrationInstanceConfigurationWorkflows();
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public List<McpIntegrationInstanceConfigurationWorkflow>
        getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
            long mcpIntegrationInstanceConfigurationId) {

        return mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfigurationId);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
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
    @PreAuthorize("isTenantAdmin()")
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
