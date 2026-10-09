/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.atlas.execution.domain.Job;
import com.bytechef.atlas.execution.service.JobService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerToolDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.workflow.execution.service.PrincipalJobService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.Instant;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
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
public class ConnectedUserMcpServerFacadeImpl implements ConnectedUserMcpServerFacade {

    private static final String TOOL_DESCRIPTION = "toolDescription";
    private static final String TOOL_NAME = "toolName";

    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final JobService jobService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final PrincipalJobService principalJobService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserMcpServerFacadeImpl(
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        JobService jobService, McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerService mcpServerService,
        McpToolService mcpToolService, PrincipalJobService principalJobService, WorkflowService workflowService) {

        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationService = integrationService;
        this.jobService = jobService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.principalJobService = principalJobService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void deleteConnectedUserMcpServer(long connectedUserId, long mcpServerId) {
        // Remove every per-user tool row whose underlying McpComponent points at the given server.
        // Since the read query rebuilds the (server -> tools) shape from these rows, removing them
        // makes the server vanish from this user's MCP Servers tab without any soft-delete flag.
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                if (mcpComponent.getMcpServerId() != mcpServerId) {
                    continue;
                }

                mcpIntegrationInstanceToolService.delete(toolRow.getId());
            }
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void enableConnectedUserMcpServer(long connectedUserId, long mcpServerId, boolean enable) {
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                if (mcpComponent.getMcpServerId() != mcpServerId) {
                    continue;
                }

                mcpIntegrationInstanceToolService.updateEnabled(toolRow.getId(), enable);
            }
        }

        Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> workflowsByServerId = getWorkflowsByMcpServerId(
            integrationInstances);

        for (ConnectedUserMcpServerWorkflowDTO workflow : workflowsByServerId.getOrDefault(mcpServerId, List.of())) {
            if (workflow.enabled() == enable) {
                continue;
            }

            integrationInstanceFacade.enableIntegrationInstanceWorkflow(
                workflow.integrationInstanceId(), workflow.workflowId(), enable);
        }
    }

    @Override
    @PreAuthorize("isTenantAdmin()")
    public void enableMcpTool(long mcpIntegrationInstanceToolId, boolean enable) {
        mcpIntegrationInstanceToolService.updateEnabled(mcpIntegrationInstanceToolId, enable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("isTenantAdmin()")
    public List<ConnectedUserMcpServerDTO> getConnectedUserMcpServers(long connectedUserId) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId);

        Map<Long, List<ConnectedUserMcpServerToolDTO>> toolsByServerId = new HashMap<>();
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceTool> toolRows = mcpIntegrationInstanceToolService
                .getMcpIntegrationInstanceTools(integrationInstance.getId());

            for (McpIntegrationInstanceTool toolRow : toolRows) {
                Optional<McpTool> mcpToolOptional = mcpToolService.fetchMcpTool(toolRow.getMcpToolId());

                if (mcpToolOptional.isEmpty()) {
                    continue;
                }

                McpTool mcpTool = mcpToolOptional.get();

                if (!mcpTool.isEnabled()) {
                    continue;
                }

                McpComponent mcpComponent = mcpComponentCache.computeIfAbsent(
                    mcpTool.getMcpComponentId(), mcpComponentService::getMcpComponent);

                toolsByServerId.computeIfAbsent(mcpComponent.getMcpServerId(), key -> new ArrayList<>())
                    .add(new ConnectedUserMcpServerToolDTO(
                        toolRow.getId(), mcpComponent.getComponentName(), mcpComponent.getComponentVersion(),
                        integrationInstance.getId(), mcpTool.getName(), toolRow.isEnabled()));
            }
        }

        Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> workflowsByServerId = getWorkflowsByMcpServerId(
            integrationInstances);

        Set<Long> mcpServerIds = new LinkedHashSet<>(toolsByServerId.keySet());

        mcpServerIds.addAll(workflowsByServerId.keySet());

        List<ConnectedUserMcpServerDTO> connectedUserMcpServers = new ArrayList<>();

        for (Long mcpServerId : mcpServerIds) {
            McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

            List<ConnectedUserMcpServerToolDTO> tools = new ArrayList<>(
                toolsByServerId.getOrDefault(mcpServerId, List.of()));

            tools.sort(Comparator.comparing(ConnectedUserMcpServerToolDTO::name));

            List<ConnectedUserMcpServerWorkflowDTO> workflows = new ArrayList<>(
                workflowsByServerId.getOrDefault(mcpServerId, List.of()));

            workflows
                .sort(Comparator.comparing(ConnectedUserMcpServerWorkflowDTO::name, String.CASE_INSENSITIVE_ORDER));

            // "enabled for user" is computed per-user, not taken from the workspace-level flag: the
            // server is considered active for this user when at least one of their tools or workflows is enabled.
            boolean enabledForUser = tools.stream()
                .anyMatch(ConnectedUserMcpServerToolDTO::enabled) ||
                workflows.stream()
                    .anyMatch(ConnectedUserMcpServerWorkflowDTO::enabled);

            connectedUserMcpServers.add(new ConnectedUserMcpServerDTO(
                mcpServer.getId(), mcpServer.getName(), enabledForUser, mcpServer.getEnvironmentId(),
                mcpServer.getLastModifiedDate(), tools, workflows));
        }

        connectedUserMcpServers.sort(Comparator.comparing(ConnectedUserMcpServerDTO::name));

        return connectedUserMcpServers;
    }

    private Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> getWorkflowsByMcpServerId(
        List<IntegrationInstance> integrationInstances) {

        Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> workflowsByServerId = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            long integrationInstanceConfigurationId = integrationInstance.getIntegrationInstanceConfigurationId();

            List<McpIntegrationInstanceConfiguration> mcpIntegrationInstanceConfigurations =
                mcpIntegrationInstanceConfigurationService
                    .getMcpIntegrationInstanceConfigurationsByIntegrationInstanceConfigurationId(
                        integrationInstanceConfigurationId);

            if (mcpIntegrationInstanceConfigurations.isEmpty()) {
                continue;
            }

            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationId);

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfiguration.getIntegrationId());

            for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurations) {
                List<McpIntegrationInstanceConfigurationWorkflow> mcpIntegrationInstanceConfigurationWorkflows =
                    mcpIntegrationInstanceConfigurationWorkflowService
                        .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                            mcpIntegrationInstanceConfiguration.getId());

                for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflows) {
                    IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                        integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                            mcpIntegrationInstanceConfigurationWorkflow
                                .getIntegrationInstanceConfigurationWorkflowId());

                    if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                        continue;
                    }

                    Workflow workflow = workflowService.getWorkflow(
                        integrationInstanceConfigurationWorkflow.getWorkflowId());

                    boolean enabled = integrationInstanceWorkflowService
                        .fetchIntegrationInstanceWorkflow(
                            integrationInstance.getId(), integrationInstanceConfigurationWorkflow.getId())
                        .map(IntegrationInstanceWorkflow::isEnabled)
                        .orElse(false);

                    workflowsByServerId
                        .computeIfAbsent(mcpIntegrationInstanceConfiguration.getMcpServerId(), key -> new ArrayList<>())
                        .add(new ConnectedUserMcpServerWorkflowDTO(
                            integrationInstance.getId(), integration.getComponentName(),
                            integrationInstanceConfiguration.getIntegrationVersion(), workflow.getId(),
                            getWorkflowToolName(mcpIntegrationInstanceConfigurationWorkflow, workflow),
                            getWorkflowToolDescription(mcpIntegrationInstanceConfigurationWorkflow, workflow), enabled,
                            getWorkflowLastExecutionDate(integrationInstance.getId(), workflow.getId())));
                }
            }
        }

        return workflowsByServerId;
    }

    private Instant getWorkflowLastExecutionDate(long integrationInstanceId, String workflowId) {
        return principalJobService
            .fetchLastWorkflowJobId(integrationInstanceId, List.of(workflowId), PlatformType.EMBEDDED)
            .map(jobService::getJob)
            .map(Job::getEndDate)
            .orElse(null);
    }

    private static @Nullable String getWorkflowToolDescription(
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow, Workflow workflow) {

        Map<String, ?> parameters = mcpIntegrationInstanceConfigurationWorkflow.getParameters();

        if (parameters != null && parameters.get(TOOL_DESCRIPTION) instanceof String toolDescription &&
            !toolDescription.isBlank()) {

            return toolDescription;
        }

        return workflow.getDescription();
    }

    private static String getWorkflowToolName(
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow, Workflow workflow) {

        Map<String, ?> parameters = mcpIntegrationInstanceConfigurationWorkflow.getParameters();

        if (parameters != null && parameters.get(TOOL_NAME) instanceof String toolName && !toolName.isBlank()) {
            return toolName;
        }

        return Objects.requireNonNullElse(workflow.getLabel(), workflow.getId());
    }
}
