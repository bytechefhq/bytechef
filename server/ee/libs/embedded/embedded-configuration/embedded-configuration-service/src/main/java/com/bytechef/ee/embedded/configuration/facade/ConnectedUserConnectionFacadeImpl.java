/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
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
public class ConnectedUserConnectionFacadeImpl implements ConnectedUserConnectionFacade {

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionFacade connectionFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionFacade connectionFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionFacade = connectionFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isCurrentConnectedUser(#connectedUserId)")
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return connectionId;
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isCurrentConnectedUser(#connectedUserId)")
    public void deleteConnectedUserConnection(long connectedUserId, long connectionId) {
        requireOwned(connectedUserId, connectionId);

        connectedUserConnectionService.deleteByConnectionId(connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isCurrentConnectedUser(#connectedUserId)")
    public List<ConnectionDTO> getConnectedUserConnections(
        long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        return getConnections(connectedUserId, componentName, connectionIds);
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public List<ConnectionDTO> getConnectedUserConnections(
        String externalUserId, Environment environment, @Nullable String componentName, List<Long> connectionIds) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return getConnections(connectedUser.getId(), componentName, connectionIds);
    }

    @Override
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> entitledConnectionIds = connectedUserConnectionMembership.getConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        List<Long> allowedConnectionIds = entitledConnectionIds.stream()
            .filter(connectionId -> connectionIds.isEmpty() || connectionIds.contains(connectionId))
            .toList();

        return connectionFacade.getConnections(new ArrayList<>(allowedConnectionIds), PlatformType.EMBEDDED)
            .stream()
            .filter(connectionDTO -> componentName == null || componentName.equals(connectionDTO.componentName()))
            .toList();
    }

    @Override
    @PreAuthorize("isTenantAdmin() or isCurrentConnectedUser(#connectedUserId)")
    public void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters) {
        requireOwned(connectedUserId, connectionId);

        connectionFacade.replaceAuthorizationParameters(
            connectionId, withIntegrationCredentials(connectedUserId, connectionId, parameters));
    }

    @Override
    public Set<Long> getOwnedConnectionIds(long connectedUserId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        return connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public Set<Long> getOwnedConnectionIds(String externalUserId, Environment environment) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return getOwnedConnectionIds(connectedUser.getId());
    }

    @Override
    public Set<Long> getSharedConnectionIds() {
        return connectedUserConnectionService.getSharedConnectionIds();
    }

    private void requireOwned(long connectedUserId, long connectionId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        List<ConnectionDTO> ownedConnectionDTOs = connectionFacade.getConnections(
            new ArrayList<>(ownedConnectionIds), PlatformType.EMBEDDED);

        boolean owned = ownedConnectionDTOs.stream()
            .anyMatch(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId));

        if (!owned) {
            throw new NoSuchElementException("Connection id=%s not found".formatted(connectionId));
        }
    }

    private Map<String, ?> withIntegrationCredentials(
        long connectedUserId, long connectionId, Map<String, ?> parameters) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Optional<IntegrationInstance> integrationInstanceOptional = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId, connectedUser.getEnvironment())
            .stream()
            .filter(integrationInstance -> integrationInstance.getConnectionId() == connectionId)
            .findFirst();

        if (integrationInstanceOptional.isEmpty()) {
            return parameters;
        }

        IntegrationInstance integrationInstance = integrationInstanceOptional.get();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        Map<String, Object> mergedParameters =
            new HashMap<>(integrationInstanceConfiguration.getConnectionParameters());

        mergedParameters.putAll(parameters);

        return mergedParameters;
    }
}
