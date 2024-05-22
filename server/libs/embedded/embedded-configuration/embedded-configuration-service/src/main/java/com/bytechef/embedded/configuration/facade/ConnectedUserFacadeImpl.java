/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      https://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.bytechef.embedded.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.embedded.configuration.domain.ConnectedUser;
import com.bytechef.embedded.configuration.domain.Integration;
import com.bytechef.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.embedded.configuration.dto.ConnectedUserDTO;
import com.bytechef.embedded.configuration.service.ConnectedUserService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.embedded.configuration.service.IntegrationService;
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
 * @author Ivica Cardic
 */
@Service
@Transactional
public class ConnectedUserFacadeImpl implements ConnectedUserFacade {

    private final ConnectionService connectionService;
    private final ConnectedUserService connectedUserService;
    private final IntegrationInstanceService integrationInstanceService;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationService integrationService;

    @SuppressFBWarnings("EI")
    public ConnectedUserFacadeImpl(
        ConnectionService connectionService, ConnectedUserService connectedUserService,
        IntegrationInstanceService integrationInstanceService,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationService integrationService) {

        this.connectionService = connectionService;
        this.connectedUserService = connectedUserService;
        this.integrationInstanceService = integrationInstanceService;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationService = integrationService;
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
        String search, CredentialStatus credentialStatus, LocalDate createDateFrom, LocalDate createDateTo,
        Long integrationId, int pageNumber) {

        Page<ConnectedUser> connectedUsers = connectedUserService.getConnectedUsers(
            search, createDateFrom, createDateTo, integrationId, pageNumber);

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
                        Validate.notNull(integrationInstance.getId(), "id"),
                        integrationInstanceConfiguration.getIntegrationId(),
                        integrationInstanceConfiguration.getIntegrationVersion(), integrationInstance.getConnectionId(),
                        connection.getCredentialStatus(), integrationInstance.isEnabled());
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
