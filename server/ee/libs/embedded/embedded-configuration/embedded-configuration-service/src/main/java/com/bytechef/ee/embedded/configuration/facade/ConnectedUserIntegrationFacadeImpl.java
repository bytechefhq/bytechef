/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.atlas.configuration.domain.Workflow;
import com.bytechef.atlas.configuration.service.WorkflowService;
import com.bytechef.automation.configuration.security.SkipAutomationAuthorization;
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
import com.bytechef.ee.embedded.configuration.dto.ProvisionIntegrationInstanceWorkflowDTO;
import com.bytechef.ee.embedded.configuration.dto.ProvisionableIntegrationDTO;
import com.bytechef.ee.embedded.configuration.exception.EmbeddedIntegrationNotVisibleException;
import com.bytechef.ee.embedded.configuration.exception.IntegrationInstanceConfigurationErrorType;
import com.bytechef.ee.embedded.configuration.security.EmbeddedPermissionEvaluator;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationServiceImpl;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationWorkflowService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.exception.ConfigurationException;
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
import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.stream.Collectors;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
@ConditionalOnEEVersion
@SkipAutomationAuthorization
public class ConnectedUserIntegrationFacadeImpl implements ConnectedUserIntegrationFacade {

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserIntegrationFacadeImpl.class);

    private final ClusterElementDefinitionService clusterElementDefinitionService;
    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final ConnectionService connectionService;
    private final EmbeddedPermissionEvaluator embeddedPermissionEvaluator;
    private final IntegrationInstanceConfigurationDTOAssembler integrationInstanceConfigurationDTOAssembler;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
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
        IntegrationInstanceConfigurationDTOAssembler integrationInstanceConfigurationDTOAssembler,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceConfigurationWorkflowService integrationInstanceConfigurationWorkflowService,
        IntegrationInstanceFacade integrationInstanceFacade, IntegrationInstanceService integrationInstanceService,
        IntegrationService integrationService, McpComponentService mcpComponentService,
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
        this.integrationInstanceConfigurationDTOAssembler = integrationInstanceConfigurationDTOAssembler;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceConfigurationWorkflowService = integrationInstanceConfigurationWorkflowService;
        this.integrationInstanceFacade = integrationInstanceFacade;
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

        return createIntegrationInstance(connectedUser, integrationInstanceConfiguration, connectionParameters);
    }

    private IntegrationInstance createIntegrationInstance(
        ConnectedUser connectedUser, IntegrationInstanceConfiguration integrationInstanceConfiguration,
        Map<String, Object> connectionParameters) {

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        long connectionId = createConnection(
            connectedUser, integration, integrationInstanceConfiguration, connectionParameters);

        return integrationInstanceService.create(
            connectedUser.getId(), connectionId, integrationInstanceConfiguration.getId());
    }

    @Override
    public void deleteIntegrationInstance(String externalUserId, long integrationInstanceId) {
        IntegrationInstance integrationInstance = integrationInstanceService.getIntegrationInstance(
            integrationInstanceId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(
            externalUserId, integrationInstanceConfiguration.getEnvironment());

        if (!Objects.equals(integrationInstance.getConnectedUserId(), connectedUser.getId())) {
            throw new AccessDeniedException(
                "Integration instance " + integrationInstanceId + " is not owned by the connected user");
        }

        if (integrationInstance.isEnabled()) {
            disableIntegrationInstanceWorkflows(integrationInstanceId);
        }

        integrationInstanceWorkflowService.deleteByIntegrationInstanceId(integrationInstanceId);

        integrationInstanceService.delete(integrationInstanceId);
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

        List<IntegrationInstance> integrationInstances = getIntegrationInstances(
            connectedUser, integrationDTO, environment);

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            integrationDTO.componentName(), integrationDTO.componentVersion());

        ConnectionDefinition connectionDefinition = Objects.requireNonNull(componentDefinition.getConnection());

        Map<Long, IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationsById =
            getInstanceConfigurations(integrationInstances, filteredConfiguration, connectedUser, mcpWorkflows);

        Map<Long, ConnectedUserIntegrationDTO.ConnectionConfig> connectionConfigsById = new HashMap<>();

        for (IntegrationInstanceConfigurationDTO instanceConfiguration : integrationInstanceConfigurationsById
            .values()) {

            connectionConfigsById.put(
                instanceConfiguration.id(),
                getConnectionConfig(integrationDTO, connectionDefinition, instanceConfiguration));
        }

        Authorization authorization = Objects.requireNonNull(connectionDefinition)
            .getAuthorizations()
            .stream()
            .filter(curAuthorization -> curAuthorization.getType() == integrationInstanceConfigurationDTO
                .authorizationType())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                ("Integration instance configuration id=%s has authorization type %s, which the %s connection " +
                    "does not define").formatted(
                        integrationInstanceConfigurationDTO.id(),
                        integrationInstanceConfigurationDTO.authorizationType(),
                        integrationDTO.componentName())));

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
            integrationInstanceWorkflows, integrationInstanceConfigurationsById, connectionConfigsById,
            oAuth2AuthorizationParameters,
            oAuth2Service.getRedirectUri());

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

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @PreAuthorize("isTenantAdmin()")
    public List<ProvisionableIntegrationDTO> getProvisionableIntegrations(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Map<Long, List<IntegrationInstanceConfiguration>> integrationInstanceConfigurationsByIntegrationId =
            integrationInstanceConfigurationService
                .getIntegrationInstanceConfigurations(connectedUser.getEnvironment(), true)
                .stream()
                .collect(Collectors.groupingBy(
                    IntegrationInstanceConfiguration::getIntegrationId, LinkedHashMap::new, Collectors.toList()));

        List<ProvisionableIntegrationDTO> provisionableIntegrations = new ArrayList<>();

        for (List<IntegrationInstanceConfiguration> integrationInstanceConfigurations : integrationInstanceConfigurationsByIntegrationId
            .values()) {

            Integration integration = integrationService.getIntegration(
                integrationInstanceConfigurations.getFirst()
                    .getIntegrationId());

            if (!embeddedPermissionEvaluator.evaluate(integration.getPermissionExpression(), connectedUser)) {
                continue;
            }

            toProvisionableIntegrationDTO(connectedUser, integration, integrationInstanceConfigurations)
                .ifPresent(provisionableIntegrations::add);
        }

        return provisionableIntegrations;
    }

    @Override
    @Transactional(propagation = Propagation.NOT_SUPPORTED)
    @PreAuthorize("isTenantAdmin()")
    public long provisionIntegrationInstance(
        long connectedUserId, long integrationInstanceConfigurationId,
        @Nullable Map<String, Object> connectionParameters,
        @Nullable List<ProvisionIntegrationInstanceWorkflowDTO> workflows) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstanceConfigurationId);

        if (integrationInstanceConfiguration.getEnvironment() != connectedUser.getEnvironment()) {
            throw new ConfigurationException(
                "Integration instance configuration id=%s does not exist in environment=%s".formatted(
                    integrationInstanceConfigurationId, connectedUser.getEnvironment()),
                IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_NOT_FOUND);
        }

        if (!integrationInstanceConfiguration.isEnabled()) {
            throw new ConfigurationException(
                "Integration instance configuration id=%s is disabled".formatted(integrationInstanceConfigurationId),
                IntegrationInstanceConfigurationErrorType.INSTANCE_CONFIGURATION_DISABLED);
        }

        Integration integration = integrationService.getIntegration(
            integrationInstanceConfiguration.getIntegrationId());

        if (!embeddedPermissionEvaluator.evaluate(integration.getPermissionExpression(), connectedUser)) {
            throw new EmbeddedIntegrationNotVisibleException(integration.getId());
        }

        Map<String, Map<String, Object>> inputsByWorkflowId = getProvisionedWorkflowInputs(
            connectedUser, integration, integrationInstanceConfiguration, workflows == null ? List.of() : workflows);

        long connectionId = createConnection(
            connectedUser, integration, integrationInstanceConfiguration,
            connectionParameters == null ? Map.of() : connectionParameters);

        IntegrationInstance integrationInstance = null;
        List<String> enableAttemptedWorkflowIds = new ArrayList<>();

        try {
            connectionService.updateCreatedBy(connectionId, connectedUser.getExternalId());

            integrationInstance = integrationInstanceService.create(
                connectedUser.getId(), connectionId, integrationInstanceConfiguration.getId());

            long integrationInstanceId = integrationInstance.getId();

            for (Map.Entry<String, Map<String, Object>> entry : inputsByWorkflowId.entrySet()) {
                integrationInstanceFacade.updateIntegrationInstanceWorkflow(
                    integrationInstanceId, entry.getKey(), entry.getValue());

                enableAttemptedWorkflowIds.add(entry.getKey());

                integrationInstanceFacade.enableIntegrationInstanceWorkflow(integrationInstanceId, entry.getKey(),
                    true);
            }

            return integrationInstanceId;
        } catch (RuntimeException exception) {
            removeProvisionedIntegrationInstance(
                integrationInstance, connectionId, enableAttemptedWorkflowIds, exception);

            throw exception;
        }
    }

    private long createConnection(
        ConnectedUser connectedUser, Integration integration,
        IntegrationInstanceConfiguration integrationInstanceConfiguration, Map<String, Object> connectionParameters) {

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            integration.getComponentName(), integration.getComponentVersion());

        ConnectionDefinition connectionDefinition = Objects.requireNonNull(componentDefinition.getConnection());

        Map<String, Object> mergedParameters = new HashMap<>(
            integrationInstanceConfiguration.getConnectionParameters());

        mergedParameters.putAll(connectionParameters);

        Environment environment = integrationInstanceConfiguration.getEnvironment();

        String connectionName = getConnectionName(
            connectedUser.getId(), integration.getComponentName(), integrationInstanceConfiguration.getName(),
            environment);

        ConnectionDTO connectionDTO = ConnectionDTO.builder()
            .authorizationType(integrationInstanceConfiguration.getAuthorizationType())
            .componentName(integration.getComponentName())
            .connectionVersion(connectionDefinition.getVersion())
            .environmentId(environment.ordinal())
            .name(connectionName)
            .parameters(mergedParameters)
            .tags(List.of())
            .build();

        return connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);
    }

    private Map<String, Map<String, Object>> getProvisionedWorkflowInputs(
        ConnectedUser connectedUser, Integration integration,
        IntegrationInstanceConfiguration integrationInstanceConfiguration,
        List<ProvisionIntegrationInstanceWorkflowDTO> workflows) {

        List<IntegrationWorkflow> integrationWorkflows = integrationWorkflowService.getIntegrationWorkflows(
            integration.getId(), integrationInstanceConfiguration.getIntegrationVersion());

        Set<String> enabledConfigurationWorkflowIds = integrationInstanceConfigurationWorkflowService
            .getIntegrationInstanceConfigurationWorkflows(integrationInstanceConfiguration.getId())
            .stream()
            .filter(IntegrationInstanceConfigurationWorkflow::isEnabled)
            .map(IntegrationInstanceConfigurationWorkflow::getWorkflowId)
            .collect(Collectors.toSet());

        Set<String> mcpWorkflowUuids = getMcpWorkflows(integration.getId(), connectedUser)
            .stream()
            .map(ConnectedUserIntegrationDTO.McpWorkflowInfo::workflowUuid)
            .collect(Collectors.toSet());

        Map<String, Map<String, Object>> inputsByWorkflowId = new LinkedHashMap<>();

        for (ProvisionIntegrationInstanceWorkflowDTO workflow : workflows) {
            IntegrationWorkflow integrationWorkflow = integrationWorkflows.stream()
                .filter(curIntegrationWorkflow -> Objects.equals(
                    curIntegrationWorkflow.getUuidAsString(), workflow.workflowUuid()))
                .filter(curIntegrationWorkflow -> !mcpWorkflowUuids.contains(curIntegrationWorkflow.getUuidAsString()))
                .filter(curIntegrationWorkflow -> enabledConfigurationWorkflowIds.contains(
                    curIntegrationWorkflow.getWorkflowId()))
                .findFirst()
                .orElseThrow(() -> new ConfigurationException(
                    "Workflow uuid=%s is not an enabled workflow of integration instance configuration id=%s".formatted(
                        workflow.workflowUuid(), integrationInstanceConfiguration.getId()),
                    IntegrationInstanceConfigurationErrorType.WORKFLOW_NOT_IN_CONFIGURATION));

            if (!embeddedPermissionEvaluator.evaluate(integrationWorkflow.getPermissionExpression(), connectedUser)) {
                throw new EmbeddedIntegrationNotVisibleException(integration.getId());
            }

            Map<String, Object> inputs = workflow.inputs();

            inputsByWorkflowId.put(integrationWorkflow.getWorkflowId(), inputs == null ? Map.of() : inputs);
        }

        return inputsByWorkflowId;
    }

    private void removeProvisionedIntegrationInstance(
        @Nullable IntegrationInstance integrationInstance, long connectionId, List<String> enableAttemptedWorkflowIds,
        RuntimeException failure) {

        if (integrationInstance != null) {
            long integrationInstanceId = integrationInstance.getId();

            for (String enableAttemptedWorkflowId : enableAttemptedWorkflowIds) {
                try {
                    integrationInstanceFacade.enableIntegrationInstanceWorkflow(
                        integrationInstanceId, enableAttemptedWorkflowId, false);
                } catch (RuntimeException exception) {
                    failure.addSuppressed(exception);
                }
            }

            try {
                integrationInstanceWorkflowService.deleteByIntegrationInstanceId(integrationInstanceId);

                integrationInstanceService.delete(integrationInstanceId);
            } catch (RuntimeException exception) {
                failure.addSuppressed(exception);
            }
        }

        try {
            connectionFacade.delete(connectionId);
        } catch (RuntimeException exception) {
            failure.addSuppressed(exception);
        }
    }

    private Optional<ProvisionableIntegrationDTO> toProvisionableIntegrationDTO(
        ConnectedUser connectedUser, Integration integration,
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {

        List<IntegrationInstanceConfiguration> sortedIntegrationInstanceConfigurations =
            integrationInstanceConfigurations.stream()
                .sorted(IntegrationInstanceConfigurationServiceImpl.SELF_SERVE_ORDER)
                .toList();

        IntegrationInstanceConfiguration selfServeIntegrationInstanceConfiguration =
            sortedIntegrationInstanceConfigurations.getLast();

        List<ConnectedUserIntegrationDTO.McpWorkflowInfo> mcpWorkflows;
        ConnectionDefinition connectionDefinition;

        try {
            mcpWorkflows = getMcpWorkflows(integration.getId(), connectedUser);

            ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
                integration.getComponentName(), integration.getComponentVersion());

            connectionDefinition = componentDefinition.getConnection();
        } catch (RuntimeException exception) {
            log.warn("Skipping provisionable integration id={}, it could not be assembled", integration.getId(),
                exception);

            return Optional.empty();
        }

        IntegrationDTO integrationDTO = null;
        List<ProvisionableIntegrationDTO.ProvisionableIntegrationInstanceConfiguration> provisionableConfigurations =
            new ArrayList<>();

        for (IntegrationInstanceConfiguration integrationInstanceConfiguration : sortedIntegrationInstanceConfigurations) {
            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO;
            ConnectedUserIntegrationDTO.OAuth2 oAuth2;

            try {
                integrationInstanceConfigurationDTO = filterWorkflows(
                    integrationInstanceConfigurationDTOAssembler.toConnectedUserIntegrationInstanceConfigurationDTO(
                        integrationInstanceConfiguration.getId()),
                    connectedUser, mcpWorkflows);

                oAuth2 = connectionDefinition == null
                    ? null
                    : getOAuth2(
                        integration.getComponentName(), connectionDefinition, integrationInstanceConfigurationDTO);
            } catch (RuntimeException exception) {
                log.warn(
                    "Skipping provisionable integration instance configuration id={}, it could not be assembled",
                    integrationInstanceConfiguration.getId(), exception);

                continue;
            }

            integrationDTO = integrationInstanceConfigurationDTO.integration();

            List<String> sharedConnectionParameterNames = integrationInstanceConfiguration.getConnectionParameters()
                .keySet()
                .stream()
                .sorted()
                .toList();

            provisionableConfigurations.add(
                new ProvisionableIntegrationDTO.ProvisionableIntegrationInstanceConfiguration(
                    integrationInstanceConfigurationDTO,
                    Objects.equals(
                        integrationInstanceConfiguration.getId(), selfServeIntegrationInstanceConfiguration.getId()),
                    sharedConnectionParameterNames,
                    oAuth2 == null ? null : oAuth2.oAuth2AuthorizationParameters(),
                    oAuth2 == null ? null : oAuth2.redirectUri()));
        }

        if (provisionableConfigurations.isEmpty()) {
            return Optional.empty();
        }

        return Optional.of(
            new ProvisionableIntegrationDTO(
                integrationDTO, integration.getComponentVersion(), provisionableConfigurations));
    }

    private String getConnectionName(
        long connectedUserId, String componentName, String name, Environment environment) {

        List<IntegrationInstance> integrationInstances = integrationInstanceService.getIntegrationInstances(
            connectedUserId, componentName, environment);

        if (integrationInstances.isEmpty()) {
            return name;
        }

        Set<String> connectionNames = connectionService.getConnections(
            integrationInstances.stream()
                .map(IntegrationInstance::getConnectionId)
                .toList())
            .stream()
            .map(Connection::getName)
            .collect(Collectors.toSet());

        if (!connectionNames.contains(name)) {
            return name;
        }

        int number = 2;

        while (connectionNames.contains(name + " " + number)) {
            number++;
        }

        return name + " " + number;
    }

    private boolean isIntegrationVisible(IntegrationDTO integrationDTO, ConnectedUser connectedUser) {
        return embeddedPermissionEvaluator.evaluate(integrationDTO.permissionExpression(), connectedUser);
    }

    private IntegrationInstanceConfigurationDTO filterWorkflows(
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

        Integer integrationVersion = integrationInstanceConfigurationDTO.integrationVersion();

        List<IntegrationWorkflow> integrationWorkflows = integrationVersion == null
            ? integrationWorkflowService.getIntegrationWorkflows(integrationInstanceConfigurationDTO.integrationId())
            : integrationWorkflowService.getIntegrationWorkflows(
                integrationInstanceConfigurationDTO.integrationId(), integrationVersion);

        for (IntegrationWorkflow integrationWorkflow : integrationWorkflows) {

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

    private void disableIntegrationInstanceWorkflows(long integrationInstanceId) {
        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(integrationInstanceId);

        for (IntegrationInstanceWorkflow integrationInstanceWorkflow : integrationInstanceWorkflows) {
            IntegrationInstanceConfigurationWorkflow integrationInstanceConfigurationWorkflow =
                integrationInstanceConfigurationWorkflowService.getIntegrationInstanceConfigurationWorkflow(
                    integrationInstanceWorkflow.getIntegrationInstanceConfigurationWorkflowId());

            if (integrationInstanceConfigurationWorkflow.isEnabled() && integrationInstanceWorkflow.isEnabled()) {
                integrationInstanceFacade.enableIntegrationInstanceWorkflow(
                    integrationInstanceId, integrationInstanceConfigurationWorkflow.getWorkflowId(), false);
            }
        }
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

        List<IntegrationInstance> integrationInstances = getIntegrationInstances(
            connectedUser, integrationDTO, environment);

        List<Connection> connections = connectionService.getConnections(
            integrationInstances.stream()
                .map(IntegrationInstance::getConnectionId)
                .toList());

        List<IntegrationInstanceWorkflow> integrationInstanceWorkflows = integrationInstanceWorkflowService
            .getIntegrationInstanceWorkflows(
                integrationInstances.stream()
                    .map(IntegrationInstance::getId)
                    .toList());

        Map<Long, IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationsById =
            getInstanceConfigurations(integrationInstances, filteredConfiguration, connectedUser, mcpWorkflows);

        ConnectedUserIntegrationDTO connectedUserIntegrationDTO = new ConnectedUserIntegrationDTO(
            connections, filteredConfiguration, integrationInstances, integrationInstanceWorkflows,
            integrationInstanceConfigurationsById);

        return connectedUserIntegrationDTO.withMcp(
            getMcpTools(integrationDTO.componentName()), mcpWorkflows,
            attachInstanceMcpData(connectedUserIntegrationDTO.integrationInstances(), connectedUser));
    }

    private ConnectedUserIntegrationDTO.ConnectionConfig getConnectionConfig(
        IntegrationDTO integrationDTO, ConnectionDefinition connectionDefinition,
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        Authorization authorization = connectionDefinition.getAuthorizations()
            .stream()
            .filter(curAuthorization -> curAuthorization.getType() == integrationInstanceConfigurationDTO
                .authorizationType())
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                ("Integration instance configuration id=%s has authorization type %s, which the %s connection " +
                    "does not define").formatted(
                        integrationInstanceConfigurationDTO.id(),
                        integrationInstanceConfigurationDTO.authorizationType(),
                        integrationDTO.componentName())));

        ConnectedUserIntegrationDTO.OAuth2 oAuth2 = getOAuth2(
            integrationDTO.componentName(), connectionDefinition, integrationInstanceConfigurationDTO);

        return new ConnectedUserIntegrationDTO.ConnectionConfig(
            authorization.getType(), oAuth2, authorization.getProperties());
    }

    private ConnectedUserIntegrationDTO.@Nullable OAuth2 getOAuth2(
        String componentName, ConnectionDefinition connectionDefinition,
        IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO) {

        AuthorizationType authorizationType = integrationInstanceConfigurationDTO.authorizationType();

        if (authorizationType == null || !StringUtils.startsWith(authorizationType.name(), "OAUTH2")) {
            return null;
        }

        OAuth2AuthorizationParameters oAuth2AuthorizationParameters = oAuth2ParametersFacade
            .getOAuth2AuthorizationParameters(
                componentName, connectionDefinition.getVersion(),
                integrationInstanceConfigurationDTO.connectionParameters(), authorizationType);

        return new ConnectedUserIntegrationDTO.OAuth2(oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());
    }

    private Map<Long, IntegrationInstanceConfigurationDTO> getInstanceConfigurations(
        List<IntegrationInstance> integrationInstances, IntegrationInstanceConfigurationDTO selfServeConfiguration,
        ConnectedUser connectedUser, List<ConnectedUserIntegrationDTO.McpWorkflowInfo> mcpWorkflows) {

        Map<Long, IntegrationInstanceConfigurationDTO> integrationInstanceConfigurationsById = new HashMap<>();

        integrationInstanceConfigurationsById.put(selfServeConfiguration.id(), selfServeConfiguration);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            Long integrationInstanceConfigurationId = integrationInstance.getIntegrationInstanceConfigurationId();

            if (integrationInstanceConfigurationsById.containsKey(integrationInstanceConfigurationId)) {
                continue;
            }

            IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO =
                integrationInstanceConfigurationDTOAssembler.toConnectedUserIntegrationInstanceConfigurationDTO(
                    integrationInstanceConfigurationId);

            integrationInstanceConfigurationsById.put(
                integrationInstanceConfigurationId,
                filterWorkflows(integrationInstanceConfigurationDTO, connectedUser, mcpWorkflows));
        }

        return integrationInstanceConfigurationsById;
    }

    private List<IntegrationInstance> getIntegrationInstances(
        ConnectedUser connectedUser, IntegrationDTO integrationDTO, Environment environment) {

        Set<Long> integrationInstanceConfigurationIds = integrationInstanceConfigurationService
            .getIntegrationInstanceConfigurations(Objects.requireNonNull(integrationDTO.id()))
            .stream()
            .map(IntegrationInstanceConfiguration::getId)
            .collect(Collectors.toSet());

        return integrationInstanceService
            .getIntegrationInstances(connectedUser.getId(), integrationDTO.componentName(), environment)
            .stream()
            .filter(integrationInstance -> integrationInstanceConfigurationIds.contains(
                integrationInstance.getIntegrationInstanceConfigurationId()))
            .toList();
    }

    private boolean isEmbeddedMcpServerEnabled(long mcpServerId) {
        McpServer mcpServer = mcpServerService.getMcpServer(mcpServerId);

        return mcpServer.getType() == PlatformType.EMBEDDED && mcpServer.isEnabled();
    }

    List<ConnectedUserIntegrationDTO.McpToolInfo> getMcpTools(String componentName) {
        return mcpComponentService.getMcpComponentsByComponentName(componentName)
            .stream()
            .filter(mcpComponent -> isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId()))
            .flatMap(mcpComponent -> mcpToolService.getMcpComponentMcpTools(mcpComponent.getId())
                .stream()
                .filter(McpTool::isEnabled)
                .map(mcpTool -> {
                    ClusterElementDefinition clusterElementDefinition =
                        clusterElementDefinitionService.getClusterElementDefinition(
                            mcpComponent.getComponentName(), mcpComponent.getComponentVersion(), mcpTool.getName());

                    return new ConnectedUserIntegrationDTO.McpToolInfo(
                        mcpTool.getId(), mcpTool.getName(), clusterElementDefinition.getTitle(),
                        clusterElementDefinition.getDescription());
                }))
            .toList();
    }

    List<ConnectedUserIntegrationDTO.McpInstanceToolInfo> getMcpInstanceTools(long integrationInstanceId) {
        return mcpIntegrationInstanceToolService.getMcpIntegrationInstanceTools(integrationInstanceId)
            .stream()
            .filter(mcpIntegrationInstanceTool -> {
                McpTool mcpTool = mcpToolService.fetchMcpTool(mcpIntegrationInstanceTool.getMcpToolId())
                    .orElse(null);

                if (mcpTool == null || !mcpTool.isEnabled()) {
                    return false;
                }

                McpComponent mcpComponent = mcpComponentService.getMcpComponent(mcpTool.getMcpComponentId());

                return isEmbeddedMcpServerEnabled(mcpComponent.getMcpServerId());
            })
            .map(mcpIntegrationInstanceTool -> new ConnectedUserIntegrationDTO.McpInstanceToolInfo(
                mcpIntegrationInstanceTool.getMcpToolId(), mcpIntegrationInstanceTool.isEnabled()))
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

                List<ConnectedUserIntegrationDTO.McpInstanceToolInfo> mcpInstanceTools = getMcpInstanceTools(
                    integrationInstanceId);

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
                    integrationInstance.workflows(), mcpInstanceTools, mcpInstanceWorkflows,
                    integrationInstance.integrationInstanceConfiguration(), integrationInstance.connectionConfig());
            })
            .toList();
    }
}
