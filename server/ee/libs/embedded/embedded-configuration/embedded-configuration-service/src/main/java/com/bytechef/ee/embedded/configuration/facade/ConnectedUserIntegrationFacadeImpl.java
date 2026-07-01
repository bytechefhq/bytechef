/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.ai.mcp.domain.McpIntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.ai.mcp.service.McpIntegrationInstanceToolService;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfigurationWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.domain.IntegrationWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationWorkflowDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.ClusterElementDefinition;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.domain.PropertyGroup;
import com.bytechef.platform.component.service.ClusterElementDefinitionService;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.domain.WorkflowInput;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.mcp.domain.McpComponent;
import com.bytechef.platform.mcp.domain.McpServer;
import com.bytechef.platform.mcp.domain.McpTool;
import com.bytechef.platform.mcp.service.McpComponentService;
import com.bytechef.platform.mcp.service.McpServerService;
import com.bytechef.platform.mcp.service.McpToolService;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.AbstractMap;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
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
public class ConnectedUserIntegrationFacadeImpl implements ConnectedUserIntegrationFacade {

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final McpComponentService mcpComponentService;
    private final McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService;
    private final McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService;
    private final McpIntegrationInstanceToolService mcpIntegrationInstanceToolService;
    private final McpServerService mcpServerService;
    private final McpToolService mcpToolService;
    private final OAuth2ParametersFacade oAuth2ParametersFacade;
    private final OAuth2Service oAuth2Service;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;
    private final IntegrationWorkflowService integrationWorkflowService;
    private final WorkflowService workflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserIntegrationFacadeImpl(
        ClusterElementDefinitionService clusterElementDefinitionService,
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade, ConnectionService connectionService,
        EmbeddedPermissionEvaluator embeddedPermissionEvaluator,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        McpComponentService mcpComponentService,
        McpIntegrationInstanceConfigurationService mcpIntegrationInstanceConfigurationService,
        McpIntegrationInstanceConfigurationWorkflowService mcpIntegrationInstanceConfigurationWorkflowService,
        McpIntegrationInstanceToolService mcpIntegrationInstanceToolService, McpServerService mcpServerService,
        McpToolService mcpToolService, OAuth2ParametersFacade oAuth2ParametersFacade, OAuth2Service oAuth2Service,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService,
        IntegrationWorkflowService integrationWorkflowService, WorkflowService workflowService) {

        this.clusterElementDefinitionService = clusterElementDefinitionService;
        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.connectionService = connectionService;
        this.embeddedPermissionEvaluator = embeddedPermissionEvaluator;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.mcpComponentService = mcpComponentService;
        this.mcpIntegrationInstanceConfigurationService = mcpIntegrationInstanceConfigurationService;
        this.mcpIntegrationInstanceConfigurationWorkflowService = mcpIntegrationInstanceConfigurationWorkflowService;
        this.mcpIntegrationInstanceToolService = mcpIntegrationInstanceToolService;
        this.mcpServerService = mcpServerService;
        this.mcpToolService = mcpToolService;
        this.oAuth2ParametersFacade = oAuth2ParametersFacade;
        this.oAuth2Service = oAuth2Service;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
        this.integrationWorkflowService = integrationWorkflowService;
        this.workflowService = workflowService;
    }

    @Override
    public IntegrationInstance createIntegrationInstance(
        String externalUserId, long integrationId, Map<String, Object> connectionParameters, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationIntegrationInstanceConfiguration(integrationId, environment, true);

        Integration integration = integrationService.getIntegration(integrationId);

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            integration.getComponentName(), integration.getComponentVersion());

        ConnectionDefinition connectionDefinition = Objects.requireNonNull(componentDefinition.getConnection());

        Map<String, Object> mergedParameters = new HashMap<>(
            integrationInstanceConfiguration.getConnectionParameters());

        mergedParameters.putAll(connectionParameters);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(integrationInstanceConfiguration.getAuthorizationType())
            .componentName(integration.getComponentName())
            .connectionVersion(connectionDefinition.getVersion())
            .environmentId(environment.ordinal())
            .name(integrationInstanceConfiguration.getName())
            .parameters(mergedParameters)
            .tags(List.of())
            .build();

        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        return integrationInstanceService.create(
            connectedUser.getId(), connectionId, integrationInstanceConfiguration.getId());
    }

    @Override
    public void deleteIntegrationInstance(String externalUserId, long integrationInstanceId) {
        integrationInstanceWorkflowService.deleteByIntegrationInstanceId(integrationInstanceId);

        IntegrationInstance integrationInstance =
            integrationInstanceService.getIntegrationInstance(integrationInstanceId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        connectedUserService.fetchConnectedUser(externalUserId, integrationInstanceConfiguration.getEnvironment())
            .ifPresent(connectedUser -> {
                if (Objects.equals(connectedUser.getExternalId(), externalUserId)) {
                    integrationInstanceService.delete(integrationInstanceId);
                }
            });
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserIntegrationDTO getConnectedUserIntegration(
        String externalUserId, long integrationId, boolean enabled, Environment environment) {

        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
            integrationInstanceConfigurationFacade.getIntegrationInstanceConfigurationIntegration(
                integrationId, enabled, environment);

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();

        if (!isIntegrationVisible(integrationDTO, connectedUser)) {
            throw new EmbeddedIntegrationNotVisibleException(integrationId);
        }

        List<ConnectedUserIntegrationDTO.McpWorkflowInfo> mcpWorkflows = getMcpWorkflows(
            integrationId, connectedUser);

        IntegrationInstanceConfigurationDTO filteredConfiguration =
            filterWorkflows(integrationInstanceConfigurationDTO, connectedUser, mcpWorkflows);

        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            connectedUser.getId(), integrationDTO.componentName(), environment);

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            integrationDTO.componentName(), integrationDTO.componentVersion());

        ConnectionDefinition connectionDefinition = Objects.requireNonNull(componentDefinition.getConnection());

        Authorization authorization = Objects.requireNonNull(connectionDefinition)
            .getAuthorizations()
            .stream()
            .filter(curAuthorization -> curAuthorization.getType() == integrationInstanceConfigurationDTO
                .authorizationType())
            .findFirst()
            .orElseThrow();

        List<Connection> connections = connectionService.getConnections(
            integrationInstances.stream()
                .map(IntegrationInstance::getConnectionId)
                .toList());

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(
                integrationInstances.stream()
                    .map(IntegrationInstance::getId)
                    .toList());

        OAuth2AuthorizationParameters oAuth2AuthorizationParameters = null;

        AuthorizationType type = authorization.getType();

        if (StringUtils.startsWith(type.name(), "OAUTH2")) {
            oAuth2AuthorizationParameters = oAuth2ParametersFacade
                .getOAuth2AuthorizationParameters(integrationDTO.componentName(), connectionDefinition.getVersion(),
                    integrationInstanceConfigurationDTO.connectionParameters(),
                    integrationInstanceConfigurationDTO.authorizationType());
        }

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            authorization, connections, filteredConfiguration, integrationInstances,
            integrationInstanceWorkflows, oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());

        return connectedUserIntegrationDTO.withMcp(
            getMcpTools(integrationDTO.componentName()), mcpWorkflows,
            attachInstanceMcpData(connectedUserIntegrationDTO.integrationInstances(), connectedUser));
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserIntegrationDTO> getConnectedUserIntegrations(
        String externalUserId, boolean enabled, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return integrationInstanceConfigurationFacade
            .getIntegrationInstanceConfigurationIntegrations(enabled, environment)
            .stream()
            .filter(integrationInstanceConfigurationDTO -> isIntegrationVisible(
                integrationInstanceConfigurationDTO.integration(), connectedUser))
            .map(integrationInstanceConfigurationDTO -> toConnectedUserIntegrationDTO(
                connectedUser, integrationInstanceConfigurationDTO, environment))
            .toList();
    }

    boolean isIntegrationVisible(IntegrationDTO integrationDTO, ConnectedUser connectedUser) {
        return embeddedPermissionEvaluator.evaluate(integrationDTO.permissionExpression(), connectedUser);
    }

    IntegrationInstanceConfigurationDTO filterWorkflows(
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO, ConnectedUser connectedUser,
        List<ConnectedUserIntegrationDTO.McpWorkflowInfo> mcpWorkflows) {

        List<IntegrationInstanceConfigurationWorkflowDTO> workflows =
            integrationInstanceConfigurationDTO.integrationInstanceConfigurationWorkflows();

        if (workflows == null || workflows.isEmpty()) {
            return integrationInstanceConfigurationDTO;
        }

        Set<String> mcpWorkflowUuids = mcpWorkflows.stream()
            .map(ConnectedUserIntegrationDTO.McpWorkflowInfo::workflowUuid)
            .collect(Collectors.toSet());

        Map<String, String> permissionExpressionsByUuid = new HashMap<>();

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflowService.getIntegrationWorkflows(
            integrationInstanceConfigurationDTO.integrationId())) {

            String uuid = integrationWorkflow.getUuidAsString();

            if (uuid != null) {
                permissionExpressionsByUuid.put(uuid, integrationWorkflow.getPermissionExpression());
            }
        }

        List<IntegrationInstanceConfigurationWorkflowDTO> visibleWorkflows = workflows.stream()
            .filter(IntegrationInstanceConfigurationWorkflowDTO::enabled)
            .filter(workflowDTO -> !mcpWorkflowUuids.contains(workflowDTO.workflowUuid()))
            .filter(workflowDTO -> embeddedPermissionEvaluator.evaluate(
                permissionExpressionsByUuid.get(workflowDTO.workflowUuid()), connectedUser))
            .map(this::resolveComponentInputGroups)
            .toList();

        return integrationInstanceConfigurationDTO.toBuilder()
            .integrationInstanceConfigurationWorkflows(visibleWorkflows)
            .build();
    }

    private IntegrationInstanceConfigurationWorkflowDTO resolveComponentInputGroups(
        IntegrationInstanceConfigurationWorkflowDTO workflowDTO) {

        Workflow workflow = workflowDTO.workflow();

        if (workflow == null) {
            return workflowDTO;
        }

        Map<String, PropertyGroup> componentInputGroups = new HashMap<>();

        for (WorkflowInput workflowInput : WorkflowInput.of(workflow)) {
            WorkflowInput.ComponentInputReference componentReference = workflowInput.getComponentInputReference();

            if (componentReference == null) {
                continue;
            }

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                componentReference.componentName(), componentReference.componentVersion());

            componentDefinition.getInputs()
                .stream()
                .filter(propertyGroup -> Objects.equals(propertyGroup.getName(), componentReference.groupName()))
                .findFirst()
                .ifPresent(propertyGroup -> componentInputGroups.put(workflowInput.getName(), propertyGroup));
        }

        if (componentInputGroups.isEmpty()) {
            return workflowDTO;
        }

        return workflowDTO.withComponentInputGroups(componentInputGroups);
    }

    private ConnectedUserIntegrationDTO toConnectedUserIntegrationDTO(
        ConnectedUser connectedUser, IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO,
        Environment environment) {

        IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();

        List<ConnectedUserIntegrationDTO.McpWorkflowInfo> mcpWorkflows = getMcpWorkflows(
            integrationDTO.id(), connectedUser);

        IntegrationInstanceConfigurationDTO filteredConfiguration =
            filterWorkflows(integrationInstanceConfigurationDTO, connectedUser, mcpWorkflows);

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getIntegrationInstances(connectedUser.getId(), integrationDTO.componentName(), environment);

        List<Connection> connections = connectionService.getConnections(
            integrationInstances.stream()
                .map(IntegrationInstance::getConnectionId)
                .toList());

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(
                integrationInstances.stream()
                    .map(IntegrationInstance::getId)
                    .toList());

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            connections, filteredConfiguration, integrationInstances, integrationInstanceWorkflows);

        return connectedUserIntegrationDTO.withMcp(
            getMcpTools(integrationDTO.componentName()), mcpWorkflows,
            attachInstanceMcpData(connectedUserIntegrationDTO.integrationInstances(), connectedUser));
    }

    private boolean isEmbeddedMcpServerEnabled(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        return mcpServer.getType() == PlatformType.EMBEDDED && mcpServer.isEnabled();
    }

    private List<ConnectedUserIntegrationDTO.McpToolInfo> getMcpTools(String componentName) {
        return mcpComponentService.getMcpComponentsByComponentName(componentName)
            .stream()
            .filter(mcpComponent -> isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId()))
            .flatMap(mcpComponent -> mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .stream()
                .map(mcpTool -> {
                    ClusterElementDefinition clusterElementDefinition =
                        clusterElementDefinitionService.getClusterElementDefinition(
                            mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

                    return new ConnectedUserIntegrationDTO.McpToolInfo(
                        mcpTool.getId(), mcpTool.getName(), clusterElementDefinition.getDescription());
                }))
            .toList();
    }

    private List<ConnectedUserIntegrationDTO.McpWorkflowInfo> getMcpWorkflows(
        long integrationId, ConnectedUser connectedUser) {

        return mcpIntegrationInstanceConfigurationService
            .getMcpIntegrationInstanceConfigurationsByIntegrationId(integrationId)
            .stream()
            .filter(mcpIntegrationInstanceConfiguration -> isEmbeddedMcpServerEnabled(
                mcpIntegrationInstanceConfiguration.getMcpServerId()))
            .map(McpIntegrationInstanceConfiguration::getId)
            .flatMap(mcpIntegrationInstanceConfigurationId -> mcpIntegrationInstanceConfigurationWorkflowService
                .getMcpIntegrationInstanceConfigurationMcpIntegrationInstanceConfigurationWorkflows(
                    mcpIntegrationInstanceConfigurationId)
                .stream())
            .map(mcpIntegrationInstanceConfigurationWorkflow -> {
                IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                    integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                        mcpIntegrationInstanceConfigurationWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                String workflowId = integrationInstanceConfigurationWorkflow.getWorkflowId();

                Workflow workflow = workflowService.getWorkflow(workflowId);

                IntegrationWorkflow integrationWorkflow =
                    integrationWorkflowService.getWorkflowIntegrationWorkflow(workflowId);

                return new AbstractMap.SimpleEntry<>(integrationWorkflow, workflow);
            })
            .filter(entry -> entry.getKey()
                .getUuidAsString() != null)
            .filter(entry -> embeddedPermissionEvaluator.evaluate(
                entry.getKey()
                    .getPermissionExpression(),
                connectedUser))
            .map(entry -> {
                IntegrationWorkflow integrationWorkflow = entry.getKey();
                Workflow workflow = entry.getValue();

                List<ConnectedUserIntegrationDTO.WorkflowInputInfo> inputs = workflow.getInputs()
                    .stream()
                    .map(input -> new ConnectedUserIntegrationDTO.WorkflowInputInfo(
                        input.name(), input.label(), input.required(), input.type()))
                    .toList();

                return new ConnectedUserIntegrationDTO.McpWorkflowInfo(
                    workflow.getLabel(), workflow.getDescription(), inputs, integrationWorkflow.getUuidAsString());
            })
            .toList();
    }

    private List<ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance> attachInstanceMcpData(
        List<ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance> integrationInstances,
        ConnectedUser connectedUser) {

        return integrationInstances.stream()
            .map(integrationInstance -> {
                long integrationInstanceId = integrationInstance.integrationInstance()
                    .getId();

                List<ConnectedUserIntegrationDTO.McpInstanceToolInfo> mcpInstanceTools =
                    mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId)
                        .stream()
                        .filter(mcpIntegrationInstanceTool -> {
                            McpTool mcpTool = mcpToolService.fetchMcpTool(mcpIntegrationInstanceTool.getMcpToolId())
                                .orElse(null);

                            if (mcpTool == null) {
                                return false;
                            }

                            McpComponent mcpComponent =
                                mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

                            return isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId());
                        })
                        .map(mcpIntegrationInstanceTool -> new ConnectedUserIntegrationDTO.McpInstanceToolInfo(
                            mcpIntegrationInstanceTool.getMcpToolId(), mcpIntegrationInstanceTool.isEnabled()))
                        .toList();

                List<ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstanceWorkflow> mcpInstanceWorkflows =
                    integrationInstanceWorkflowService.getIntegrationInstanceWorkflows(integrationInstanceId)
                        .stream()
                        .map(integrationInstanceWorkflow -> {
                            McpIntegrationInstanceConfigurationWorkflow mcpIntegrationInstanceConfigurationWorkflow =
                                mcpIntegrationInstanceConfigurationWorkflowService
                                    .fetchMcpIntegrationInstanceConfigurationWorkflowByIntegrationInstanceConfigurationWorkflowId(
                                        integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId())
                                    .orElse(null);

                            if (mcpIntegrationInstanceConfigurationWorkflow == null) {
                                return null;
                            }

                            McpIntegrationInstanceConfiguration mcpIntegrationInstanceConfiguration =
                                mcpIntegrationInstanceConfigurationService
                                    .fetchMcpIntegrationInstanceConfiguration(
                                        mcpIntegrationInstanceConfigurationWorkflow
                                            .getMcpIntegrationInstanceConfigurationId())
                                    .orElse(null);

                            if (mcpIntegrationInstanceConfiguration == null
                                || !isEmbeddedMcpServerEnabled(mcpIntegrationInstanceConfiguration.getMcpServerId())) {

                                return null;
                            }

                            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                                integrationInstanceConfigurationWorkflowService
                                    .getIntegrationInstanceConfigurationWorkflow(
                                        integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

                            IntegrationWorkflow integrationWorkflow =
                                integrationWorkflowService.getWorkflowIntegrationWorkflow(
                                    integrationInstanceConfigurationWorkflow.getWorkflowId());

                            if (!embeddedPermissionEvaluator.evaluate(
                                integrationWorkflow.getPermissionExpression(), connectedUser)) {

                                return null;
                            }

                            return new ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstanceWorkflow(
                                integrationInstanceWorkflow, integrationWorkflow.getUuidAsString());
                        })
                        .filter(Objects::nonNull)
                        .toList();

                return new ConnectedUserIntegrationDTO.ConnectedUserIntegrationInstance(
                    integrationInstance.connection(), integrationInstance.integrationInstance(),
                    integrationInstance.workflows(), mcpInstanceTools, mcpInstanceWorkflows);
            })
            .toList();
    }
}
