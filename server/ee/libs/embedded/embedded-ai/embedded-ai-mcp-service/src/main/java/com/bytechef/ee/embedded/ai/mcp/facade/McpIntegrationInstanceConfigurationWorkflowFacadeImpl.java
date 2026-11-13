/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.ee.embedded.ai.mcp.audit.McpIntegrationInstanceConfigurationAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.dto.IntegrationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
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
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceRows mcpIntegrationInstanceRows;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    McpIntegrationInstanceConfigurationWorkflowFacadeImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceRows mcpIntegrationInstanceRows, WorkflowService workflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceRows = mcpIntegrationInstanceRows;
        this.workflowService = workflowService;
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_DELETED,
        data = {
            @AuditData(
                key = "mcpIntegrationInstanceConfigurationWorkflowId",
                value = "#mcpIntegrationInstanceConfigurationWorkflowId")
        })
    @PreAuthorize("isTenantAdmin()")
    public void deleteMcpIntegrationInstanceConfigurationWorkflow(long mcpIntegrationInstanceConfigurationWorkflowId) {
        getMcpIntegrationInstanceConfigurationWorkflowOrThrow(mcpIntegrationInstanceConfigurationWorkflowId);

        mcpIntegrationInstanceConfigurationWorkflowService.delete(mcpIntegrationInstanceConfigurationWorkflowId);
    }

    @Override
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_CREATED,
        data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationWorkflowId", value = "#result?.id"),
            @AuditData(key = "mcpIntegrationInstanceConfigurationId", value = "#mcpIntegrationInstanceConfigurationId"),
            @AuditData(
                key = "integrationInstanceConfigurationWorkflowId",
                value = "#integrationInstanceConfigurationWorkflowId")
        })
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow createMcpIntegrationInstanceConfigurationWorkflow(
        long mcpIntegrationInstanceConfigurationId, long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService.create(
                mcpIntegrationInstanceConfigurationId, integrationInstanceConfigurationWorkflowId);

        addMissingRowsForConnectedUsers(mcpIntegrationInstanceConfigurationId);

        return mcpIntegrationInstanceConfigurationWorkflow;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflow(
        long id, @Nullable Long mcpIntegrationInstanceConfigurationId,
        @Nullable Long integrationInstanceConfigurationWorkflowId) {

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            getMcpIntegrationInstanceConfigurationWorkflowOrThrow(id);

        if ((mcpIntegrationInstanceConfigurationId != null && !Objects.equals(
            mcpIntegrationInstanceConfigurationId,
            mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId())) ||
            (integrationInstanceConfigurationWorkflowId != null && !Objects.equals(
                integrationInstanceConfigurationWorkflowId,
                mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId()))) {

            throw new IllegalArgumentException(
                "McpIntegrationInstanceConfigurationWorkflow " + id + " cannot be moved to another workflow");
        }

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
    @Audited(
        event = McpIntegrationInstanceConfigurationAuditEvents.MCP_INTEGRATION_INSTANCE_CONFIGURATION_WORKFLOW_ENABLED_UPDATED,
        data = {
            @AuditData(key = "mcpIntegrationInstanceConfigurationWorkflowId", value = "#id"),
            @AuditData(
                key = "mcpIntegrationInstanceConfigurationId",
                value = "#result?.mcpIntegrationInstanceConfigurationId"),
            @AuditData(key = "enabled", value = "#enabled")
        })
    @PreAuthorize("isTenantAdmin()")
    public McpIntegrationInstanceConfigurationWorkflow updateMcpIntegrationInstanceConfigurationWorkflowEnabled(
        long id, boolean enabled) {

        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
            mcpIntegrationInstanceConfigurationWorkflowService.updateEnabled(id, enabled);

        if (enabled) {
            addMissingRowsForConnectedUsers(
                mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId());
        }

        return mcpIntegrationInstanceConfigurationWorkflow;
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

    private void addMissingRowsForConnectedUsers(long mcpIntegrationInstanceConfigurationId) {
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
            mcpIntegrationInstanceConfigurationService
                .fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationId)
                .orElseThrow(() -> new IllegalArgumentException(
                    "McpIntegrationInstanceConfiguration not found: " + mcpIntegrationInstanceConfigurationId));

        mcpIntegrationInstanceRows.addMissingRowsForConnectedUsers(
            mcpIntegrationInstanceConfiguration.getMcpServerId());
    }

    private McpIntegrationInstanceConfigurationWorkflow getMcpIntegrationInstanceConfigurationWorkflowOrThrow(
        long mcpIntegrationInstanceConfigurationWorkflowId) {

        return mcpIntegrationInstanceConfigurationWorkflowService
            .fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceConfigurationWorkflowId)
            .orElseThrow(
                () -> new IllegalArgumentException(
                    "McpIntegrationInstanceConfigurationWorkflow not found: "
                        + mcpIntegrationInstanceConfigurationWorkflowId));
    }

    private static @Nullable WorkflowTrigger getToolCallableTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return workflowTrigger;
            }
        }

        return null;
    }
}
