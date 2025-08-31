/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.connected.user.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.Integration;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.facade.IntegrationInstanceFacade;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.configuration.service.IntegrationService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.dto.ConnectedUserDTO;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.configuration.service.EnvironmentService;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.time.LocalDate;
import java.util.List;
import java.util.Objects;
import java.util.stream.Stream;
import org.apache.commons.lang3.Validate;
import org.springframework.data.domain.Page;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserFacadeImpl implements ConnectedUserFacade {

    private final ConnectionService connectionService;
    private final ConnectedUserService connectedUserService;
    private final EnvironmentService environmentService;
    private final IntegrationInstanceFacade integrationInstanceFacade;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public ConnectedUserFacadeImpl(
        ConnectionService connectionService, ConnectedUserService connectedUserService,
        EnvironmentService environmentService, IntegrationInstanceFacade integrationInstanceFacade,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService) {

        this.connectionService = connectionService;
        this.connectedUserService = connectedUserService;
        this.environmentService = environmentService;
        this.integrationInstanceFacade = integrationInstanceFacade;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
    }

    @Override
    public void enableConnectedUser(long id, boolean enable) {
        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(id);

        for (IntegrationInstance integrationInstance : integrationInstances) {
            if (integrationInstance.isEnabled()) {
                integrationInstanceFacade.enableIntegrationInstanceWorkflowTriggers(
                    integrationInstance.getId(), enable);
            }
        }

        connectedUserService.enableConnectedUser(id, enable);
    }

    @Override
    @Transactional(readOnly = true)
    public ConnectedUserDTO getConnectedUser(long id) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(id);

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(id);

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                CollectionUtils.map(integrationInstances, IntegrationInstance::getIntegrationInstanceConfigurationId));

        List<Integration> integrations = integrationService.getIntegrations(
            CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfiguration::getIntegrationId));

        return createConnectedUserDTO(
            connectedUser, integrationInstances, integrationInstanceConfigurations, integrations);
    }

    // TODO Add paging and filtering
    @Override
    @Transactional(readOnly = true)
    public Page<ConnectedUserDTO> getConnectedUsers(
        Long environmentId, String search, CredentialStatus credentialStatus, LocalDate createDateFrom,
        LocalDate createDateTo, Long integrationId, int pageNumber) {

        Environment environment = environmentId == null ? null : environmentService.getEnvironment(environmentId);

        Page<ConnectedUser> connectedUsers = connectedUserService.getConnectedUsers(
            environment, search, createDateFrom, createDateTo, integrationId, pageNumber);

        Stream<ConnectedUser> stream = connectedUsers.stream();

        List<IntegrationInstance> integrationInstances = integrationInstanceService
            .getConnectedUserIntegrationInstances(CollectionUtils.map(stream.toList(), ConnectedUser::getId));

        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations =
            integrationInstanceConfigurationService.getIntegrationInstanceConfigurations(
                CollectionUtils.map(integrationInstances, IntegrationInstance::getIntegrationInstanceConfigurationId));

        List<Integration> integrations = integrationService.getIntegrations(
            CollectionUtils.map(integrationInstanceConfigurations, IntegrationInstanceConfiguration::getIntegrationId));

        return connectedUsers.map(connectedUser -> createConnectedUserDTO(
            connectedUser,
            CollectionUtils.filter(
                integrationInstances,
                integrationInstance -> Objects.equals(
                    integrationInstance.getConnectedUserId(), connectedUser.getId())),
            integrationInstanceConfigurations, integrations));
    }

    private ConnectedUserDTO createConnectedUserDTO(
        ConnectedUser connectedUser, List<IntegrationInstance> integrationInstances,
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations, List<Integration> integrations) {

        return new ConnectedUserDTO(
            connectedUser,
            CollectionUtils.map(
                integrationInstances,
                integrationInstance -> {
                    Connection connection = connectionService.getConnection(integrationInstance.getConnectionId());
                    IntegrationInstanceConfiguration integrationInstanceConfiguration =
                        getIntegrationInstanceConfiguration(
                            integrationInstance, integrationInstanceConfigurations);

                    return new ConnectedUserDTO.IntegrationInstance(
                        getComponentName(integrationInstance, integrationInstanceConfigurations, integrations),
                        connection.getCredentialStatus(), integrationInstance.isEnabled(),
                        Validate.notNull(integrationInstance.getId(), "id"),
                        integrationInstanceConfiguration.getIntegrationId(), integrationInstanceConfiguration.getId(),
                        integrationInstanceConfiguration.getIntegrationVersion(),
                        integrationInstance.getConnectionId());
                }));
    }

    private String getComponentName(
        IntegrationInstance integrationInstance,
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations, List<Integration> integrations) {

        return CollectionUtils.getFirstFilter(
            integrations,
            integration -> {
                IntegrationInstanceConfiguration integrationInstanceConfiguration =
                    getIntegrationInstanceConfiguration(
                        integrationInstance, integrationInstanceConfigurations);

                return Objects.equals(integrationInstanceConfiguration.getIntegrationId(), integration.getId());
            },
            Integration::getComponentName);
    }

    private static IntegrationInstanceConfiguration getIntegrationInstanceConfiguration(
        IntegrationInstance integrationInstance,
        List<IntegrationInstanceConfiguration> integrationInstanceConfigurations) {

        return CollectionUtils.getFirst(
            integrationInstanceConfigurations,
            curIntegrationInstanceConfiguration -> Objects.equals(
                curIntegrationInstanceConfiguration.getId(),
                integrationInstance.getIntegrationInstanceConfigurationId()));
    }
}
