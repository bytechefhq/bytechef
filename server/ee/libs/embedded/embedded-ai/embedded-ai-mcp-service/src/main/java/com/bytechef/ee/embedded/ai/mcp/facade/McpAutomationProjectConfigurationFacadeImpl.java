/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.domain.ProjectWorkflow;
import com.bytechef.automation.configuration.security.AutomationAuthorizationContext;
import com.bytechef.automation.configuration.service.ProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.audit.McpAutomationProjectConfigurationAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpAutomationProjectConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationProjectConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.McpAutomationWorkflowConfigurationDTO;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpAutomationProjectConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.AutomationProjectConfiguration;
import com.bytechef.ee.embedded.configuration.service.AutomationProjectConfigurationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
import com.bytechef.platform.component.constant.WorkflowConstants;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.definition.WorkflowNodeType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.UUID;
import java.util.function.Supplier;
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
public class McpAutomationProjectConfigurationFacadeImpl implements McpAutomationProjectConfigurationFacade {

    private final AutomationProjectConfigurationService automationProjectConfigurationService;
    private final McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService;
    private final McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService;
    private final McpAutomationRows mcpAutomationRows;
    private final McpServerService mcpServerService;
    private final ProjectWorkflowService projectWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public McpAutomationProjectConfigurationFacadeImpl(
        AutomationProjectConfigurationService automationProjectConfigurationService,
        McpAutomationProjectConfigurationService mcpAutomationProjectConfigurationService,
        McpAutomationProjectConfigurationWorkflowService mcpAutomationProjectConfigurationWorkflowService,
        McpAutomationRows mcpAutomationRows, McpServerService mcpServerService,
        ProjectWorkflowService projectWorkflowService, WorkflowService workflowService) {

        this.automationProjectConfigurationService = automationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationService = mcpAutomationProjectConfigurationService;
        this.mcpAutomationProjectConfigurationWorkflowService = mcpAutomationProjectConfigurationWorkflowService;
        this.mcpAutomationRows = mcpAutomationRows;
        this.mcpServerService = mcpServerService;
        this.projectWorkflowService = projectWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    @Audited(
        event = McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_CREATED, data = {
            @AuditData(key = "mcpAutomationProjectConfigurationId", value = "#result?.id"),
            @AuditData(key = "mcpServerId", value = "#mcpServerId"),
            @AuditData(key = "automationProjectConfigurationId", value = "#automationProjectConfigurationId"),
            @AuditData(key = "workflowUuids", value = "#workflowUuids")
        })
    @PreAuthorize("isTenantAdmin()")
    public McpAutomationProjectConfiguration createMcpAutomationProjectConfiguration(
        long mcpServerId, long automationProjectConfigurationId, List<String> workflowUuids,
        List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations) {

        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        AutomationProjectConfiguration automationProjectConfiguration =
            automationProjectConfigurationService.getConfiguration(automationProjectConfigurationId);

        if (automationProjectConfiguration.getEnvironmentId() != mcpServer.getEnvironmentId()) {
            throw new IllegalArgumentException(
                "Automation project configuration " + automationProjectConfigurationId +
                    " is not in the environment of MCP server " + mcpServerId);
        }

        Set<String> selectedWorkflowUuids = new LinkedHashSet<>(workflowUuids);

        Map<String, Map<String, ?>> parametersByWorkflowUuid = getParametersByWorkflowUuid(
            selectedWorkflowUuids, workflowConfigurations);

        for (String workflowUuid : selectedWorkflowUuids) {
            requireToolWorkflow(automationProjectConfiguration, workflowUuid);
        }

        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            mcpAutomationProjectConfigurationService.create(
                new McpAutomationProjectConfiguration(automationProjectConfigurationId, mcpServerId));

        for (String workflowUuid : selectedWorkflowUuids) {
            createLinkWorkflow(
                mcpAutomationProjectConfiguration.getId(), workflowUuid, parametersByWorkflowUuid.get(workflowUuid));
        }

        mcpAutomationRows.addMissingRowsForLink(mcpAutomationProjectConfiguration.getId());

        return mcpAutomationProjectConfiguration;
    }

    @Override
    @Audited(
        event = McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_DELETED, data = {
            @AuditData(key = "mcpAutomationProjectConfigurationId", value = "#id")
        })
    @PreAuthorize("isTenantAdmin()")
    public void deleteMcpAutomationProjectConfiguration(long id) {
        getMcpAutomationProjectConfigurationOrThrow(id);

        mcpAutomationProjectConfigurationService.delete(id);
    }

    @Override
    @Audited(
        event = McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_UPDATED, data = {
            @AuditData(key = "mcpAutomationProjectConfigurationId", value = "#id"),
            @AuditData(key = "mcpServerId", value = "#result?.mcpServerId"),
            @AuditData(key = "workflowUuids", value = "#workflowUuids")
        })
    @PreAuthorize("isTenantAdmin()")
    public McpAutomationProjectConfiguration updateMcpAutomationProjectConfiguration(
        long id, List<String> workflowUuids, List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations) {

        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            getMcpAutomationProjectConfigurationOrThrow(id);

        AutomationProjectConfiguration automationProjectConfiguration =
            automationProjectConfigurationService.getConfiguration(
                mcpAutomationProjectConfiguration.getAutomationProjectConfigurationId());

        Set<String> selectedWorkflowUuids = new LinkedHashSet<>(workflowUuids);

        Map<String, Map<String, ?>> parametersByWorkflowUuid = getParametersByWorkflowUuid(
            selectedWorkflowUuids, workflowConfigurations);

        Map<String, McpAutomationProjectConfigurationWorkflow> existingWorkflowsByWorkflowUuid = new HashMap<>();

        for (McpAutomationProjectConfigurationWorkflow existingWorkflow : mcpAutomationProjectConfigurationWorkflowService
            .getMcpAutomationProjectConfigurationWorkflows(id)) {

            existingWorkflowsByWorkflowUuid.put(existingWorkflow.getWorkflowUuid(), existingWorkflow);
        }

        for (String workflowUuid : selectedWorkflowUuids) {
            if (!existingWorkflowsByWorkflowUuid.containsKey(workflowUuid)) {
                requireToolWorkflow(automationProjectConfiguration, workflowUuid);
            }
        }

        List<Long> createdLinkWorkflowIds = new ArrayList<>();

        for (String workflowUuid : selectedWorkflowUuids) {
            McpAutomationProjectConfigurationWorkflow existingWorkflow =
                existingWorkflowsByWorkflowUuid.get(workflowUuid);

            Map<String, ?> parameters = parametersByWorkflowUuid.get(workflowUuid);

            if (existingWorkflow == null) {
                McpAutomationProjectConfigurationWorkflow createdWorkflow = createLinkWorkflow(
                    id, workflowUuid, parameters);

                createdLinkWorkflowIds.add(createdWorkflow.getId());
            } else if (parameters != null) {
                mcpAutomationProjectConfigurationWorkflowService.updateParameters(existingWorkflow.getId(), parameters);
            }
        }

        for (Map.Entry<String, McpAutomationProjectConfigurationWorkflow> entry : existingWorkflowsByWorkflowUuid
            .entrySet()) {

            if (!selectedWorkflowUuids.contains(entry.getKey())) {
                McpAutomationProjectConfigurationWorkflow removedWorkflow = entry.getValue();

                mcpAutomationProjectConfigurationWorkflowService.delete(removedWorkflow.getId());
            }
        }

        mcpAutomationRows.addMissingRowsForLinkWorkflows(id, createdLinkWorkflowIds);

        return mcpAutomationProjectConfiguration;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public Optional<Workflow> fetchMcpAutomationProjectConfigurationWorkflowPinnedWorkflow(
        long mcpAutomationProjectConfigurationWorkflowId) {

        Optional<McpAutomationProjectConfigurationWorkflow> mcpAutomationProjectConfigurationWorkflowOptional =
            mcpAutomationProjectConfigurationWorkflowService.fetch(mcpAutomationProjectConfigurationWorkflowId);

        if (mcpAutomationProjectConfigurationWorkflowOptional.isEmpty()) {
            return Optional.empty();
        }

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            mcpAutomationProjectConfigurationWorkflowOptional.get();

        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration =
            getMcpAutomationProjectConfigurationOrThrow(
                mcpAutomationProjectConfigurationWorkflow.getMcpAutomationProjectConfigurationId());

        AutomationProjectConfiguration automationProjectConfiguration =
            automationProjectConfigurationService.getConfiguration(
                mcpAutomationProjectConfiguration.getAutomationProjectConfigurationId());

        return fetchPinnedWorkflow(automationProjectConfiguration,
            mcpAutomationProjectConfigurationWorkflow.getWorkflowUuid());
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public McpAutomationProjectConfigurationDTO getMcpAutomationProjectConfiguration(long id) {
        return toDTO(getMcpAutomationProjectConfigurationOrThrow(id));
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    @Transactional(readOnly = true)
    public List<McpAutomationProjectConfigurationDTO> getMcpServerMcpAutomationProjectConfigurations(
        long mcpServerId) {

        List<McpAutomationProjectConfigurationDTO> mcpAutomationProjectConfigurationDTOs = new ArrayList<>();

        for (McpAutomationProjectConfiguration mcpAutomationProjectConfiguration : mcpAutomationProjectConfigurationService
            .getMcpServerMcpAutomationProjectConfigurations(mcpServerId)) {

            mcpAutomationProjectConfigurationDTOs.add(toDTO(mcpAutomationProjectConfiguration));
        }

        return mcpAutomationProjectConfigurationDTOs;
    }

    @Override
    @Audited(
        event = McpAutomationProjectConfigurationAuditEvents.MCP_AUTOMATION_PROJECT_CONFIGURATION_WORKFLOW_ENABLED_UPDATED,
        data = {
            @AuditData(key = "mcpAutomationProjectConfigurationWorkflowId", value = "#id"),
            @AuditData(
                key = "mcpAutomationProjectConfigurationId", value = "#result?.mcpAutomationProjectConfigurationId"),
            @AuditData(key = "enabled", value = "#enabled")
        })
    @PreAuthorize("isTenantAdmin()")
    public McpAutomationProjectConfigurationWorkflow updateMcpAutomationProjectConfigurationWorkflowEnabled(
        long id, boolean enabled) {

        return mcpAutomationProjectConfigurationWorkflowService.updateEnabled(id, enabled);
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public McpAutomationProjectConfigurationWorkflow updateMcpAutomationProjectConfigurationWorkflowParameters(
        long id, Map<String, ?> parameters) {

        return mcpAutomationProjectConfigurationWorkflowService.updateParameters(id, parameters);
    }

    private McpAutomationProjectConfigurationWorkflow createLinkWorkflow(
        long linkId, String workflowUuid, Map<String, ?> parameters) {

        McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow =
            mcpAutomationProjectConfigurationWorkflowService.create(linkId, workflowUuid);

        if (parameters != null) {
            mcpAutomationProjectConfigurationWorkflowService.updateParameters(
                mcpAutomationProjectConfigurationWorkflow.getId(), parameters);
        }

        return mcpAutomationProjectConfigurationWorkflow;
    }

    private McpAutomationProjectConfiguration getMcpAutomationProjectConfigurationOrThrow(long id) {
        return mcpAutomationProjectConfigurationService.fetch(id)
            .orElseThrow(() -> new IllegalArgumentException("McpAutomationProjectConfiguration not found: " + id));
    }

    private Optional<Workflow> fetchPinnedWorkflow(
        AutomationProjectConfiguration automationProjectConfiguration, String workflowUuid) {

        Optional<ProjectWorkflow> projectWorkflowOptional = callSkippingAutomationChecks(
            () -> projectWorkflowService.fetchProjectWorkflow(
                automationProjectConfiguration.getProjectId(), automationProjectConfiguration.getProjectVersion(),
                workflowUuid));

        return projectWorkflowOptional.map(projectWorkflow -> workflowService.getWorkflow(
            projectWorkflow.getWorkflowId()));
    }

    private void requireToolWorkflow(
        AutomationProjectConfiguration automationProjectConfiguration, String workflowUuid) {

        if (!automationProjectConfigurationService.isWorkflowEnabled(
            automationProjectConfiguration, UUID.fromString(workflowUuid))) {

            throw new IllegalArgumentException(
                "Workflow " + workflowUuid + " is turned off in automation project configuration " +
                    automationProjectConfiguration.getId());
        }

        Optional<Workflow> workflowOptional = fetchPinnedWorkflow(automationProjectConfiguration, workflowUuid);

        if (workflowOptional.isEmpty() || !hasWorkflowCallTrigger(workflowOptional.get())) {
            throw new IllegalArgumentException(
                "Workflow " + workflowUuid + " has no workflow call trigger in automation project configuration " +
                    automationProjectConfiguration.getId());
        }
    }

    private McpAutomationProjectConfigurationDTO toDTO(
        McpAutomationProjectConfiguration mcpAutomationProjectConfiguration) {

        AutomationProjectConfiguration automationProjectConfiguration =
            automationProjectConfigurationService.getConfiguration(
                mcpAutomationProjectConfiguration.getAutomationProjectConfigurationId());

        List<McpAutomationProjectConfigurationWorkflowDTO> workflowDTOs = new ArrayList<>();

        for (McpAutomationProjectConfigurationWorkflow mcpAutomationProjectConfigurationWorkflow : mcpAutomationProjectConfigurationWorkflowService
            .getMcpAutomationProjectConfigurationWorkflows(mcpAutomationProjectConfiguration.getId())) {

            String workflowUuid = mcpAutomationProjectConfigurationWorkflow.getWorkflowUuid();

            Optional<Workflow> workflowOptional = fetchPinnedWorkflow(automationProjectConfiguration, workflowUuid);

            boolean available = automationProjectConfiguration.isEnabled() &&
                automationProjectConfigurationService.isWorkflowEnabled(
                    automationProjectConfiguration, UUID.fromString(workflowUuid))
                &&
                workflowOptional.isPresent() && hasWorkflowCallTrigger(workflowOptional.get());

            String label = workflowOptional.map(Workflow::getLabel)
                .orElse(null);

            workflowDTOs.add(
                new McpAutomationProjectConfigurationWorkflowDTO(
                    mcpAutomationProjectConfigurationWorkflow.getId(), workflowUuid, label,
                    mcpAutomationProjectConfigurationWorkflow.getParameters(),
                    mcpAutomationProjectConfigurationWorkflow.isEnabled(), available));
        }

        return new McpAutomationProjectConfigurationDTO(
            mcpAutomationProjectConfiguration.getId(), mcpAutomationProjectConfiguration.getMcpServerId(),
            automationProjectConfiguration.getId(), automationProjectConfiguration.getName(),
            automationProjectConfiguration.getProjectId(), automationProjectConfiguration.getProjectVersion(),
            automationProjectConfiguration.isEnabled(), workflowDTOs);
    }

    private static <T> T callSkippingAutomationChecks(Supplier<T> supplier) {
        try {
            return AutomationAuthorizationContext.callSkippingChecks(supplier::get);
        } catch (RuntimeException | Error exception) {
            throw exception;
        } catch (Throwable throwable) {
            throw new IllegalStateException(throwable);
        }
    }

    private static Map<String, Map<String, ?>> getParametersByWorkflowUuid(
        Set<String> selectedWorkflowUuids, List<McpAutomationWorkflowConfigurationDTO> workflowConfigurations) {

        Map<String, Map<String, ?>> parametersByWorkflowUuid = new HashMap<>();

        Set<String> configuredWorkflowUuids = new HashSet<>();

        for (McpAutomationWorkflowConfigurationDTO workflowConfiguration : workflowConfigurations) {
            String workflowUuid = workflowConfiguration.workflowUuid();

            if (!selectedWorkflowUuids.contains(workflowUuid)) {
                throw new IllegalArgumentException("Workflow " + workflowUuid + " is not among the selected workflows");
            }

            if (!configuredWorkflowUuids.add(workflowUuid)) {
                throw new IllegalArgumentException("Workflow " + workflowUuid + " is configured more than once");
            }

            parametersByWorkflowUuid.put(workflowUuid, workflowConfiguration.parameters());
        }

        return parametersByWorkflowUuid;
    }

    private static boolean hasWorkflowCallTrigger(Workflow workflow) {
        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.ofEnabled(workflow)) {
            WorkflowNodeType workflowNodeType = WorkflowNodeType.ofType(workflowTrigger.getType());

            if (Objects.equals(workflowNodeType.name(), WorkflowConstants.WORKFLOW) &&
                Objects.equals(workflowNodeType.operation(), WorkflowConstants.NEW_WORKFLOW_CALL)) {

                return true;
            }
        }

        return false;
    }
}
