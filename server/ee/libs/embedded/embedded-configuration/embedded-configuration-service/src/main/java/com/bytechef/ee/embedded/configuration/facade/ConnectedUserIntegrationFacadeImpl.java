/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.dto.ConnectedUserIntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationDTO;
import com.bytechef.ee.embedded.configuration.dto.IntegrationInstanceConfigurationDTO;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.component.domain.Authorization;
import com.bytechef.platform.component.domain.ComponentDefinition;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.domain.OAuth2AuthorizationParameters;
import com.bytechef.platform.component.service.ComponentDefinitionService;
import com.bytechef.platform.configuration.facade.OAuth2ParametersFacade;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.Environment;
import com.bytechef.platform.oauth2.service.OAuth2Service;
import java.util.List;
import java.util.Objects;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserIntegrationFacadeImpl implements ConnectedUserIntegrationFacade {

    private final ComponentDefinitionService componentDefinitionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionService connectionService;
    private final IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final OAuth2ParametersFacade oAuth2ParametersFacade;
    private final OAuth2Service oAuth2Service;

    public ConnectedUserIntegrationFacadeImpl(
        ComponentDefinitionService componentDefinitionService, ConnectedUserService connectedUserService,
        ConnectionService connectionService,
        IntegrationInstanceConfigurationFacade integrationInstanceConfigurationFacade,
        IntegrationInstanceService integrationInstanceService, OAuth2ParametersFacade oAuth2ParametersFacade,
        OAuth2Service oAuth2Service) {

        this.componentDefinitionService = componentDefinitionService;
        this.connectedUserService = connectedUserService;
        this.connectionService = connectionService;
        this.integrationInstanceConfigurationFacade = integrationInstanceConfigurationFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.oAuth2ParametersFacade = oAuth2ParametersFacade;
        this.oAuth2Service = oAuth2Service;
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

        IntegrationInstance integrationInstance = integrationInstanceService
            .fetchIntegrationInstance(connectedUser.getId(), integrationDTO.componentName(), environment)
            .orElse(null);

        Connection connection = null;

        if (integrationInstance != null) {
            connection = connectionService.getConnection(integrationInstance.getConnectionId());
        }

        ComponentDefinition componentDefinition = componentDefinitionService.getComponentDefinition(
            integrationDTO.componentName(), integrationInstanceConfigurationDTO.integrationVersion());

        ConnectionDefinition connectionDefinition = componentDefinition.getConnection();

        Authorization authorization = Objects.requireNonNull(connectionDefinition)
            .getAuthorizations()
            .getFirst();

        AuthorizationType authorizationType = authorization.getType();

        OAuth2AuthorizationParameters oAuth2AuthorizationParameters = oAuth2ParametersFacade
            .getOAuth2AuthorizationParameters(integrationDTO.componentName(), connectionDefinition.getVersion(),
                integrationInstanceConfigurationDTO.connectionParameters(), authorizationType.getName());

        return new ConnectedUserIntegrationDTO(
            authorization, connection, integrationInstance, integrationInstanceConfigurationDTO,
            oAuth2AuthorizationParameters, oAuth2Service.getRedirectUri());
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

        IntegrationInstance integrationInstance = integrationInstanceService
            .fetchIntegrationInstance(connectedUser.getId(), integrationDTO.componentName(), environment)
            .orElse(null);

        Connection connection = null;

        if (integrationInstance != null) {
            connection = connectionService.getConnection(integrationInstance.getConnectionId());
        }

        return new ConnectedUserIntegrationDTO(connection, integrationInstance, integrationInstanceConfigurationDTO);
    }
}
