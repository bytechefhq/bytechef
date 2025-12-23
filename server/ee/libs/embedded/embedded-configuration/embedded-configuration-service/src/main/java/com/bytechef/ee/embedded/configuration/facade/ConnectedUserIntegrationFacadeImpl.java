/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceWorkflow;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceWorkflowService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Objects;
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

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationService integrationService;
    private final OAuth2ParametersFacade oAuth2ParametersFacade;
    private final OAuth2Service oAuth2Service;
    private final IntegrationInstanceWorkflowService integrationInstanceWorkflowService;

    @SuppressFBWarnings("EI")
    public ConnectedUserIntegrationFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        ConnectionService connectionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService, IntegrationService integrationService,
        OAuth2ParametersFacade oAuth2ParametersFacade, OAuth2Service oAuth2Service,
        IntegrationInstanceWorkflowService integrationInstanceWorkflowService) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationService = integrationService;
        this.oAuth2ParametersFacade = oAuth2ParametersFacade;
        this.oAuth2Service = oAuth2Service;
        this.integrationInstanceWorkflowService = integrationInstanceWorkflowService;
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

        Connection connection = connectionService.create(
            integrationInstanceConfiguration.getAuthorizationType(), integration.getComponentName(),
            connectionDefinition.getVersion(), environment.ordinal(),
            integrationInstanceConfiguration.getName(), connectionParameters, PlatformType.EMBEDDED);

        return integrationInstanceService.create(
            connectedUser.getId(), connection.getId(), integrationInstanceConfiguration.getId());
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

        return new ConnectedUserIntegrationDTO(
            authorization, connections, integrationInstanceConfigurationDTO, integrationInstances,
            integrationInstanceWorkflows, oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());
    }

    @Override
    @Transactional(readOnly = true)
    public List<ConnectedUserIntegrationDTO> getConnectedUserIntegrations(
        String externalUserId, boolean enabled, Environment environment) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return integrationInstanceConfigurationFacade
            .getIntegrationInstanceConfigurationIntegrations(enabled, environment)
            .stream()
            .map(integrationInstanceConfigurationDTO -> toConnectedUserIntegrationDTO(
                connectedUser, integrationInstanceConfigurationDTO, environment))
            .toList();
    }

    private ConnectedUserIntegrationDTO toConnectedUserIntegrationDTO(
        ConnectedUser connectedUser, IntegrationInstanceConfigurationDTO integrationInstanceConfigurationDTO,
        Environment environment) {

        IntegrationDTO integrationDTO = integrationInstanceConfigurationDTO.integration();

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

        return new ConnectedUserIntegrationDTO(connections, integrationInstanceConfigurationDTO, integrationInstances,
            integrationInstanceWorkflows);
    }
}
