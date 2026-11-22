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
import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditEvents;
import com.bytechef.ee.embedded.ai.mcp.audit.ConnectedUserMcpServerAuditMapper;
import com.bytechef.ee.embedded.ai.mcp.domain.McpConnectedUserProjectWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceTool;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerAutomationDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerToolDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ConnectedUserMcpServerWorkflowDTO;
import com.bytechef.ee.embedded.ai.mcp.dto.ProvisionableConnectedUserMcpServerDTO;
import com.bytechef.ee.embedded.ai.mcp.exception.ConnectedUserMcpServerErrorType;
import com.bytechef.ee.embedded.ai.mcp.service.McpConnectedUserProjectWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.ai.tool.util.ToolNameUtils;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.audit.AuditData;
import com.bytechef.platform.audit.Audited;
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
import java.util.TreeSet;
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

    private final ConnectedUserMcpAutomations connectedUserMcpAutomations;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final JobService jobService;
    private final McpAutomationRowFacade mcpAutomationRowFacade;
    private final McpComponentService mcpComponentService;
    private final McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final PrincipalJobService principalJobService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    @SuppressWarnings("PMD.ExcessiveParameterList")
    public ConnectedUserMcpServerFacadeImpl(
        ConnectedUserMcpAutomations connectedUserMcpAutomations, ConnectedUserService connectedUserService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        JobService jobService, McpAutomationRowFacade mcpAutomationRowFacade, McpComponentService mcpComponentService,
        McpConnectedUserProjectWorkflowService mcpConnectedUserProjectWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceRowFacade mcpIntegrationInstanceRowFacade,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService,
        McpIntegrationInstanceWorkflowService mcpIntegrationInstanceWorkflowService, McpServerService mcpServerService,
        McpToolService mcpToolService, PrincipalJobService principalJobService, WorkflowService workflowService) {

        this.connectedUserMcpAutomations = connectedUserMcpAutomations;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.jobService = jobService;
        this.mcpAutomationRowFacade = mcpAutomationRowFacade;
        this.mcpComponentService = mcpComponentService;
        this.mcpConnectedUserProjectWorkflowService = mcpConnectedUserProjectWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceRowFacade = mcpIntegrationInstanceRowFacade;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpIntegrationInstanceWorkflowService = mcpIntegrationInstanceWorkflowService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.principalJobService = principalJobService;
        this.workflowService = workflowService;
    }

    @Override
    @Audited(
        event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_DELETED,
        mapper = ConnectedUserMcpServerAuditMapper.class, data = {
            @AuditData(key = "connectedUserId", value = "#connectedUserId"),
            @AuditData(key = "mcpServerId", value = "#mcpServerId")
        })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_DELETE')")
    public boolean deleteConnectedUserMcpServer(long connectedUserId, long mcpServerId) {
        Map<Long, McpComponent> mcpComponentCache = new HashMap<>();
        boolean deleted = false;

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

                deleted = true;
            }

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : getMcpIntegrationInstanceWorkflows(
                integrationInstance.getId(), mcpServerId)) {

                mcpIntegrationInstanceWorkflowService.delete(mcpIntegrationInstanceWorkflow.getId());

                deleted = true;
            }
        }

        for (McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow : connectedUserMcpAutomations
            .getMcpServerRows(connectedUserId, mcpServerId)) {

            mcpConnectedUserProjectWorkflowService.delete(mcpConnectedUserProjectWorkflow.getId());

            deleted = true;
        }

        return deleted;
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_ENABLED_UPDATED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "mcpServerId", value = "#mcpServerId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT')")
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

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : getMcpIntegrationInstanceWorkflows(
                integrationInstance.getId(), mcpServerId)) {

                if (mcpIntegrationInstanceWorkflow.isEnabled() != enable) {
                    mcpIntegrationInstanceWorkflowService.updateEnabled(mcpIntegrationInstanceWorkflow.getId(), enable);
                }
            }
        }

        for (McpConnectedUserProjectWorkflow mcpConnectedUserProjectWorkflow : connectedUserMcpAutomations
            .getMcpServerRows(connectedUserId, mcpServerId)) {

            if (mcpConnectedUserProjectWorkflow.isEnabled() != enable) {
                mcpConnectedUserProjectWorkflowService.updateEnabled(mcpConnectedUserProjectWorkflow.getId(), enable);
            }
        }
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_AUTOMATION_ENABLED_UPDATED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "mcpConnectedUserProjectWorkflowId", value = "#mcpConnectedUserProjectWorkflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'MCP_EDIT')")
    public ConnectedUserMcpServerAutomationDTO enableMcpAutomation(
        long connectedUserId, long mcpConnectedUserProjectWorkflowId, boolean enable) {

        connectedUserMcpAutomations.requireConnectedUserRow(connectedUserId, mcpConnectedUserProjectWorkflowId);

        mcpConnectedUserProjectWorkflowService.updateEnabled(mcpConnectedUserProjectWorkflowId, enable);

        return connectedUserMcpAutomations.getAutomation(connectedUserId, mcpConnectedUserProjectWorkflowId);
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_TOOL_ENABLED_UPDATED, data = {
        @AuditData(key = "mcpIntegrationInstanceToolId", value = "#mcpIntegrationInstanceToolId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceToolId, 'McpIntegrationInstanceTool', 'MCP_EDIT')")
    public void enableMcpTool(long mcpIntegrationInstanceToolId, boolean enable) {
        mcpIntegrationInstanceToolService.updateEnabled(mcpIntegrationInstanceToolId, enable);
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_WORKFLOW_ENABLED_UPDATED, data = {
        @AuditData(key = "mcpIntegrationInstanceWorkflowId", value = "#mcpIntegrationInstanceWorkflowId"),
        @AuditData(key = "enabled", value = "#enable")
    })
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceWorkflowId, 'McpIntegrationInstanceWorkflow', 'MCP_EDIT')")
    public void enableMcpWorkflow(long mcpIntegrationInstanceWorkflowId, boolean enable) {
        mcpIntegrationInstanceWorkflowService.updateEnabled(mcpIntegrationInstanceWorkflowId, enable);
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'MCP_VIEW')")
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

        Map<Long, List<ConnectedUserMcpServerAutomationDTO>> automationsByServerId =
            connectedUserMcpAutomations.getAutomationsByMcpServerId(connectedUserId);

        Set<Long> mcpServerIds = new LinkedHashSet<>(toolsByServerId.keySet());

        mcpServerIds.addAll(workflowsByServerId.keySet());
        mcpServerIds.addAll(automationsByServerId.keySet());

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

            List<ConnectedUserMcpServerAutomationDTO> automations = automationsByServerId.getOrDefault(
                mcpServerId, List.of());

            boolean enabledForUser = tools.stream()
                .anyMatch(ConnectedUserMcpServerToolDTO::enabled) ||
                workflows.stream()
                    .anyMatch(ConnectedUserMcpServerWorkflowDTO::enabled)
                ||
                automations.stream()
                    .anyMatch(ConnectedUserMcpServerAutomationDTO::enabled);

            connectedUserMcpServers.add(new ConnectedUserMcpServerDTO(
                mcpServer.getId(), mcpServer.getName(), enabledForUser, mcpServer.getEnvironmentId(),
                mcpServer.getLastModifiedDate(), tools, workflows, automations));
        }

        connectedUserMcpServers.sort(Comparator.comparing(ConnectedUserMcpServerDTO::name));

        return connectedUserMcpServers;
    }

    @Override
    @Transactional(readOnly = true)
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_VIEW')")
    public List<ProvisionableConnectedUserMcpServerDTO> getProvisionableMcpServers(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        UserInstanceCoverage userInstanceCoverage = getUserInstanceCoverage(connectedUserId);

        Set<Long> provisionedMcpServerIds = new LinkedHashSet<>();

        for (ConnectedUserMcpServerDTO connectedUserMcpServer : getConnectedUserMcpServers(connectedUserId)) {
            provisionedMcpServerIds.add(connectedUserMcpServer.id());
        }

        List<ProvisionableConnectedUserMcpServerDTO> provisionableMcpServers = new ArrayList<>();

        for (McpServer mcpServer : mcpServerService.getMcpServers(PlatformType.EMBEDDED)) {
            if (!mcpServer.isEnabled() || mcpServer.getEnvironment() != connectedUser.getEnvironment()) {
                continue;
            }

            McpServerCoverage mcpServerCoverage = getMcpServerCoverage(mcpServer.getId(), userInstanceCoverage);

            ConnectedUserMcpAutomations.AutomationCoverage automationCoverage = connectedUserMcpAutomations
                .getCoverage(connectedUserId, mcpServer.getId());

            provisionableMcpServers.add(new ProvisionableConnectedUserMcpServerDTO(
                mcpServer.getId(), mcpServer.getName(), provisionedMcpServerIds.contains(mcpServer.getId()),
                new ArrayList<>(mcpServerCoverage.coveredComponentNames()),
                new ArrayList<>(mcpServerCoverage.missingComponentNames()),
                new ArrayList<>(automationCoverage.coveredNames()),
                new ArrayList<>(automationCoverage.missingNames())));
        }

        provisionableMcpServers.sort(
            Comparator.comparing(ProvisionableConnectedUserMcpServerDTO::name, String.CASE_INSENSITIVE_ORDER));

        return provisionableMcpServers;
    }

    @Override
    @Audited(event = ConnectedUserMcpServerAuditEvents.CONNECTED_USER_MCP_SERVER_PROVISIONED, data = {
        @AuditData(key = "connectedUserId", value = "#connectedUserId"),
        @AuditData(key = "mcpServerId", value = "#mcpServerId")
    })
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_MANAGE')")
    public void provisionMcpServer(long connectedUserId, long mcpServerId) {
        ProvisionableConnectedUserMcpServerDTO provisionableMcpServer = getProvisionableMcpServers(connectedUserId)
            .stream()
            .filter(candidate -> candidate.id() == mcpServerId)
            .findFirst()
            .orElseThrow(() -> new ConfigurationException(
                "MCP server %d cannot be provisioned for connected user %d".formatted(mcpServerId, connectedUserId),
                ConnectedUserMcpServerErrorType.MCP_SERVER_NOT_PROVISIONABLE));

        if (provisionableMcpServer.coveredComponentNames()
            .isEmpty() &&
            provisionableMcpServer.coveredAutomationNames()
                .isEmpty()) {

            throw new ConfigurationException(
                "MCP server %d has no integration or automation covered by connected user %d".formatted(
                    mcpServerId, connectedUserId),
                ConnectedUserMcpServerErrorType.NO_COVERED_INTEGRATION);
        }

        mcpIntegrationInstanceRowFacade.provisionMcpServer(connectedUserId, mcpServerId);
        mcpAutomationRowFacade.provisionMcpServer(connectedUserId, mcpServerId);
    }

    private Optional<McpIntegrationInstanceConfiguration> fetchMcpIntegrationInstanceConfiguration(
        McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow) {

        return mcpIntegrationInstanceConfigurationService.fetchMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfigurationWorkflow.getMcpIntegrationInstanceConfigurationId());
    }

    private Optional<McpIntegrationInstanceConfigurationWorkflow> fetchMcpIntegrationInstanceConfigurationWorkflow(
        McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow) {

        return mcpIntegrationInstanceConfigurationWorkflowService.fetchMcpIntegrationInstanceConfigurationWorkflow(
            mcpIntegrationInstanceWorkflow.getMcpIntegrationInstanceConfigurationWorkflowId());
    }

    private List<McpIntegrationInstanceWorkflow> getMcpIntegrationInstanceWorkflows(
        long integrationInstanceId, long mcpServerId) {

        List<McpIntegrationInstanceWorkflow> mcpIntegrationInstanceWorkflows = new ArrayList<>();

        for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflowService
            .getMcpIntegrationInstanceWorkflows(integrationInstanceId)) {

            boolean onMcpServer = fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceWorkflow)
                .flatMap(this::fetchMcpIntegrationInstanceConfiguration)
                .filter(mcpIntegrationInstanceConfiguration -> Objects.equals(
                    mcpIntegrationInstanceConfiguration.getMcpServerId(), mcpServerId))
                .isPresent();

            if (onMcpServer) {
                mcpIntegrationInstanceWorkflows.add(mcpIntegrationInstanceWorkflow);
            }
        }

        return mcpIntegrationInstanceWorkflows;
    }

    private McpServerCoverage getMcpServerCoverage(long mcpServerId, UserInstanceCoverage userInstanceCoverage) {
        Set<String> coveredComponentNames = new TreeSet<>();
        Set<String> missingComponentNames = new TreeSet<>();

        for (McpComponent mcpComponent : mcpComponentService.getMcpServerMcpComponents(mcpServerId)) {
            if (!hasEnabledMcpTool(mcpComponent)) {
                continue;
            }

            if (userInstanceCoverage.componentNames()
                .contains(mcpComponent.getComponentName())) {

                coveredComponentNames.add(mcpComponent.getComponentName());
            } else {
                missingComponentNames.add(mcpComponent.getComponentName());
            }
        }

        for (McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration : mcpIntegrationInstanceConfigurationService
            .getMcpServerMcpIntegrationInstanceConfigurations(mcpServerId)) {

            if (!hasEnabledConfigurationWorkflow(mcpIntegrationInstanceConfiguration)) {
                continue;
            }

            long integrationInstanceConfigurationId =
                Objects.requireNonNull(mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());

            if (userInstanceCoverage.integrationInstanceConfigurationIds()
                .contains(integrationInstanceConfigurationId)) {

                coveredComponentNames.add(getComponentName(integrationInstanceConfigurationId));
            } else {
                missingComponentNames.add(getComponentName(integrationInstanceConfigurationId));
            }
        }

        missingComponentNames.removeAll(coveredComponentNames);

        return new McpServerCoverage(coveredComponentNames, missingComponentNames);
    }

    private String getComponentName(long integrationInstanceConfigurationId) {
        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        Integration integration =
            integrationService.getIntegration(integrationInstanceConfiguration.getIntegrationId());

        return integration.getComponentName();
    }

    private boolean hasEnabledConfigurationWorkflow(
        McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration) {

        for (McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow : mcpIntegrationInstanceConfigurationWorkflowService
            .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                mcpIntegrationInstanceConfiguration.getId())) {

            if (!mcpIntegrationInstanceConfigurationWorkflow.isEnabled()) {
                continue;
            }

            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (integrationInstanceConfigurationWorkflow.isEnabled()) {
                return true;
            }
        }

        return false;
    }

    private boolean hasEnabledMcpTool(McpComponent mcpComponent) {
        for (McpTool mcpTool : mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())) {
            if (mcpTool.isEnabled()) {
                return true;
            }
        }

        return false;
    }

    private UserInstanceCoverage getUserInstanceCoverage(long connectedUserId) {
        Set<String> componentNames = new LinkedHashSet<>();
        Set<Long> integrationInstanceConfigurationIds = new LinkedHashSet<>();

        for (IntegrationInstance integrationInstance : integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId)) {

            long integrationInstanceConfigurationId = integrationInstance.getIntegrationInstanceConfigurationId();

            integrationInstanceConfigurationIds.add(integrationInstanceConfigurationId);
            componentNames.add(getComponentName(integrationInstanceConfigurationId));
        }

        return new UserInstanceCoverage(componentNames, integrationInstanceConfigurationIds);
    }

    private Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> getWorkflowsByMcpServerId(
        List<IntegrationInstance> integrationInstances) {

        Map<Long, List<ConnectedUserMcpServerWorkflowDTO>> workflowsByServerId = new HashMap<>();

        for (IntegrationInstance integrationInstance : integrationInstances) {
            List<McpIntegrationInstanceWorkflow> mcpIntegrationInstanceWorkflows =
                mcpIntegrationInstanceWorkflowService.getMcpIntegrationInstanceWorkflows(integrationInstance.getId());

            if (mcpIntegrationInstanceWorkflows.isEmpty()) {
                continue;
            }

            IntegrationInstanceConfiguration integrationInstanceConfiguration =
                integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                    integrationInstance.getIntegrationInstanceConfigurationId());

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfiguration.getIntegrationId());

            for (McpIntegrationInstanceWorkflow mcpIntegrationInstanceWorkflow : mcpIntegrationInstanceWorkflows) {
                McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                    fetchMcpIntegrationInstanceConfigurationWorkflow(mcpIntegrationInstanceWorkflow)
                        .orElse(null);

                if (mcpIntegrationInstanceConfigurationWorkflow == null ||
                    !mcpIntegrationInstanceConfigurationWorkflow.isEnabled()) {

                    continue;
                }

                McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
                    fetchMcpIntegrationInstanceConfiguration(mcpIntegrationInstanceConfigurationWorkflow)
                        .orElse(null);

                if (mcpIntegrationInstanceConfiguration == null) {
                    continue;
                }

                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                        mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
                    continue;
                }

                Workflow workflow =
                    workflowService.getWorkflow(integrationInstanceConfigurationWorkflow.getWorkflowId());

                workflowsByServerId
                    .computeIfAbsent(mcpIntegrationInstanceConfiguration.getMcpServerId(), key -> new ArrayList<>())
                    .add(new ConnectedUserMcpServerWorkflowDTO(
                        mcpIntegrationInstanceWorkflow.getId(), integrationInstance.getId(),
                        integration.getComponentName(), integrationInstanceConfiguration.getIntegrationVersion(),
                        workflow.getId(), getWorkflowToolName(mcpIntegrationInstanceConfigurationWorkflow, workflow),
                        getWorkflowToolDescription(mcpIntegrationInstanceConfigurationWorkflow, workflow),
                        mcpIntegrationInstanceWorkflow.isEnabled(),
                        getWorkflowLastExecutionDate(integrationInstance.getId(), workflow.getId())));
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

        return Objects.requireNonNullElse(
            ToolNameUtils.getWorkflowToolName(
                mcpIntegrationInstanceConfigurationWorkflow.getParameters(), workflow.getLabel()),
            workflow.getId());
    }

    private record McpServerCoverage(Set<String> coveredComponentNames, Set<String> missingComponentNames) {
    }

    private record UserInstanceCoverage(Set<String> componentNames, Set<Long> integrationInstanceConfigurationIds) {
    }
}
