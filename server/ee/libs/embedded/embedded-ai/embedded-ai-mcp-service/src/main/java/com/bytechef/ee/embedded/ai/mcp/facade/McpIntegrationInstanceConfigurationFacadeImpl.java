/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.ai.mcp.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.domain.WorkflowTask;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.service.PermissionService;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflowConnection;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.ComponentConnection;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowTrigger;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO;
import com.bytechef.platform.configuration.dto.WorkflowConfigurationDTO.WorkflowConnectionDTO;
import com.bytechef.platform.configuration.facade.ComponentConnectionFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.service.McpServerService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
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

    private final ComponentConnectionFacade componentConnectionFacade;
    private final ConnectionService connectionService;
    private final EmbeddedMcpReader embeddedMcpReader;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationService integrationService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpServerService mcpServerService;
    private final PermissionService permissionService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public McpIntegrationInstanceConfigurationFacadeImpl(
        ComponentConnectionFacade componentConnectionFacade, ConnectionService connectionService,
        EmbeddedMcpReader embeddedMcpReader,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService, IntegrationService integrationService,
        IntegrationWorkflowService integrationWorkflowService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpServerService mcpServerService, @Lazy PermissionService permissionService,
        WorkflowService workflowService) {

        this.componentConnectionFacade = componentConnectionFacade;
        this.connectionService = connectionService;
        this.embeddedMcpReader = embeddedMcpReader;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationService = integrationService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpServerService = mcpServerService;
        this.permissionService = permissionService;
        this.workflowService = workflowService;
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_CREATE')")
    public McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        return createMcpIntegrationInstanceConfiguration(
            mcpServerId, integrationInstanceConfigurationId, selectedWorkflowIds, List.of());
    }

    @Override
    @PreAuthorize("hasPermission(#mcpServerId, 'McpServer', 'MCP_EDIT') and " +
        "hasPermission(#integrationInstanceConfigurationId, 'IntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_CREATE')")
    public McpIntegrationInstanceConfiguration createMcpIntegrationInstanceConfiguration(
        long mcpServerId, long integrationInstanceConfigurationId, List<String> selectedWorkflowIds,
        List<WorkflowConfigurationDTO> workflowConfigurations) {

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                integrationInstanceConfigurationId);

        checkSameEnvironment(mcpServerId, integrationInstanceConfiguration);

        Map<String, WorkflowConfigurationDTO> workflowConfigurationMap = toWorkflowConfigurationMap(
            workflowConfigurations);

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
                saveIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceConfiguration, existingWorkflowsByWorkflowId.get(workflowId), workflowId,
                    workflowConfigurationMap.get(workflowId));

            mcpIntegrationInstanceConfigurationWorkflowService.create(
                mcpIntegrationInstanceConfiguration.getId(), integrationInstanceConfigurationWorkflow.getId());
        }

        return mcpIntegrationInstanceConfiguration;
    }

    @Override
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
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds) {

        return updateMcpIntegrationInstanceConfiguration(
            mcpIntegrationInstanceConfigurationId, selectedWorkflowIds, List.of());
    }

    @Override
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
    public McpIntegrationInstanceConfiguration updateMcpIntegrationInstanceConfiguration(
        long mcpIntegrationInstanceConfigurationId, List<String> selectedWorkflowIds,
        List<WorkflowConfigurationDTO> workflowConfigurations) {

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

        IntegrationInstanceConfiguration integrationInstanceConfiguration =
            integrationInstanceConfigurationService.getIntegrationInstanceConfiguration(
                mcpIntegrationInstanceConfiguration.getIntegrationInstanceConfigurationId());

        Map<String, WorkflowConfigurationDTO> workflowConfigurationMap = toWorkflowConfigurationMap(
            workflowConfigurations);

        for (String workflowId : selectedWorkflowIds) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowByWorkflowIdMap.get(workflowId);

            if (existingWorkflowIdMap.containsKey(workflowId)) {
                WorkflowConfigurationDTO workflowConfiguration = workflowConfigurationMap.get(workflowId);

                if (workflowConfiguration != null) {
                    saveIntegrationInstanceConfigurationWorkflow(
                        integrationInstanceConfiguration, integrationInstanceConfigurationWorkflow, workflowId,
                        workflowConfiguration);
                }
            } else {
                integrationInstanceConfigurationWorkflow = saveIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceConfiguration, integrationInstanceConfigurationWorkflow, workflowId,
                    workflowConfigurationMap.get(workflowId));

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
    @PreAuthorize("hasPermission(#mcpIntegrationInstanceConfigurationId, 'McpIntegrationInstanceConfiguration', " +
        "'INTEGRATION_INSTANCE_CONFIGURATION_EDIT')")
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

    private static boolean isSameConnectionSlot(
        WorkflowConnectionDTO workflowConnection, ComponentConnection componentConnection) {

        return Objects.equals(workflowConnection.workflowNodeName(), componentConnection.workflowNodeName()) &&
            Objects.equals(workflowConnection.workflowConnectionKey(), componentConnection.key());
    }

    private static Map<String, WorkflowConfigurationDTO> toWorkflowConfigurationMap(
        List<WorkflowConfigurationDTO> workflowConfigurations) {

        Map<String, WorkflowConfigurationDTO> workflowConfigurationMap = new HashMap<>();

        for (WorkflowConfigurationDTO workflowConfiguration : workflowConfigurations) {
            workflowConfigurationMap.put(workflowConfiguration.workflowId(), workflowConfiguration);
        }

        return workflowConfigurationMap;
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

    private List<ComponentConnection> getComponentConnections(Workflow workflow) {
        List<ComponentConnection> componentConnections = new ArrayList<>();

        for (WorkflowTrigger workflowTrigger : WorkflowTrigger.of(workflow)) {
            componentConnections.addAll(componentConnectionFacade.getComponentConnections(workflowTrigger));
        }

        for (WorkflowTask workflowTask : workflow.getTasks(true)) {
            componentConnections.addAll(componentConnectionFacade.getComponentConnections(workflowTask));
        }

        return componentConnections;
    }

    private boolean isConnectionUsable(
        long connectionId, ComponentConnection componentConnection, Environment environment) {

        Connection connection = connectionService.getConnection(connectionId);

        return connection.getEnvironmentId() == environment.ordinal() &&
            Objects.equals(connection.getComponentName(), componentConnection.componentName());
    }

    private IntegrationInstanceConfigurationWorkflow saveIntegrationInstanceConfigurationWorkflow(
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        @Nullable IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow, String workflowId,
        @Nullable WorkflowConfigurationDTO workflowConfiguration) {

        if (integrationInstanceConfigurationWorkflow == null) {
            integrationInstanceConfigurationWorkflow = new IntegrationInstanceConfigurationWorkflow();

            integrationInstanceConfigurationWorkflow.setIntegrationInstanceConfigurationId(
                integrationInstanceConfiguration.getId());
            integrationInstanceConfigurationWorkflow.setWorkflowId(workflowId);
            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setInputs(Map.of());

            if (workflowConfiguration != null) {
                integrationInstanceConfigurationWorkflow.setConnections(
                    toIntegrationInstanceConfigurationWorkflowConnections(
                        integrationInstanceConfiguration, workflowId, workflowConfiguration.connections()));
                integrationInstanceConfigurationWorkflow.setInputs(workflowConfiguration.inputs());
            }

            return integrationInstanceConfigurationWorkflowService.create(integrationInstanceConfigurationWorkflow);
        }

        if (workflowConfiguration != null) {
            integrationInstanceConfigurationWorkflow.setConnections(
                toIntegrationInstanceConfigurationWorkflowConnections(
                    integrationInstanceConfiguration, workflowId, workflowConfiguration.connections()));
            integrationInstanceConfigurationWorkflow.setEnabled(true);
            integrationInstanceConfigurationWorkflow.setInputs(workflowConfiguration.inputs());

            return integrationInstanceConfigurationWorkflowService.update(integrationInstanceConfigurationWorkflow);
        }

        if (!integrationInstanceConfigurationWorkflow.isEnabled()) {
            integrationInstanceConfigurationWorkflowService.updateEnabled(
                integrationInstanceConfigurationWorkflow.getId(), true);
        }

        return integrationInstanceConfigurationWorkflow;
    }

    private List<IntegrationInstanceConfigurationWorkflowConnection>
        toIntegrationInstanceConfigurationWorkflowConnections(
            IntegrationInstanceConfiguration integrationInstanceConfiguration, String workflowId,
            List<WorkflowConnectionDTO> workflowConnections) {

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        Environment environment = integrationInstanceConfiguration.getEnvironment();

        List<ComponentConnection> componentConnections = getComponentConnections(
            workflowService.getWorkflow(workflowId));

        for (WorkflowConnectionDTO workflowConnection : workflowConnections) {
            boolean configurable = componentConnections.stream()
                .anyMatch(componentConnection -> isSameConnectionSlot(workflowConnection, componentConnection) &&
                    !Objects.equals(componentConnection.componentName(), integration.getComponentName()));

            if (!configurable) {
                throw new IllegalArgumentException(
                    "Connection for " + workflowConnection.workflowNodeName() + " of workflow " + workflowId +
                        " cannot be configured: it is not a connection of a component other than " +
                        integration.getComponentName());
            }
        }

        List<IntegrationInstanceConfigurationWorkflowConnection> integrationInstanceConfigurationWorkflowConnections =
            new ArrayList<>();

        for (ComponentConnection componentConnection : componentConnections) {
            if (Objects.equals(componentConnection.componentName(), integration.getComponentName())) {
                continue;
            }

            Optional<WorkflowConnectionDTO> workflowConnection = workflowConnections.stream()
                .filter(curWorkflowConnection -> isSameConnectionSlot(curWorkflowConnection, componentConnection))
                .findFirst();

            if (workflowConnection.isPresent()) {
                long connectionId = workflowConnection.get()
                    .connectionId();

                if (!isConnectionUsable(connectionId, componentConnection, environment)) {
                    throw new IllegalArgumentException(
                        "Connection " + connectionId + " cannot be used for " + componentConnection.workflowNodeName() +
                            " of workflow " + workflowId + ": it must be a " + componentConnection.componentName() +
                            " connection in the " + environment + " environment");
                }

                integrationInstanceConfigurationWorkflowConnections.add(
                    new IntegrationInstanceConfigurationWorkflowConnection(
                        connectionId, componentConnection.key(), componentConnection.workflowNodeName()));
            } else if (componentConnection.required()) {
                throw new IllegalArgumentException(
                    "Workflow " + workflowId + " requires a " + componentConnection.componentName() +
                        " connection for " + componentConnection.workflowNodeName());
            }
        }

        return integrationInstanceConfigurationWorkflowConnections;
    }
}
