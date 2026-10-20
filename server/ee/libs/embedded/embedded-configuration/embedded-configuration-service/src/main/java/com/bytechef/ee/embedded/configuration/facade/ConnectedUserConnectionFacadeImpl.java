/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.embedded.configuration.facade;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstance;
import com.bytechef.ee.embedded.configuration.domain.IntegrationInstanceConfiguration;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionMembership;
import com.bytechef.ee.embedded.configuration.security.ConnectedUserConnectionReader;
import com.bytechef.ee.embedded.configuration.service.ConnectedUserConnectionService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceConfigurationService;
import com.bytechef.ee.embedded.configuration.service.IntegrationInstanceService;
import com.bytechef.ee.embedded.connected.user.domain.ConnectedUser;
import com.bytechef.ee.embedded.connected.user.service.ConnectedUserService;
import com.bytechef.platform.annotation.ConditionalOnEEVersion;
import com.bytechef.platform.component.domain.ConnectionDefinition;
import com.bytechef.platform.component.service.ConnectionDefinitionService;
import com.bytechef.platform.configuration.domain.Environment;
import com.bytechef.platform.connection.dto.ConnectionDTO;
import com.bytechef.platform.connection.facade.ConnectionFacade;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.domain.BaseProperty;
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
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private static final Logger log = LoggerFactory.getLogger(ConnectedUserConnectionFacadeImpl.class);

    private final ConnectedUserConnectionMembership connectedUserConnectionMembership;
    private final ConnectedUserConnectionReader connectedUserConnectionReader;
    private final ConnectedUserConnectionService connectedUserConnectionService;
    private final ConnectedUserService connectedUserService;
    private final ConnectionDefinitionService connectionDefinitionService;
    private final ConnectionFacade connectionFacade;
    private final IntegrationInstanceConfigurationService integrationInstanceConfigurationService;
    private final IntegrationInstanceService integrationInstanceService;

    @SuppressFBWarnings("EI")
    public ConnectedUserConnectionFacadeImpl(
        ConnectedUserConnectionMembership connectedUserConnectionMembership,
        ConnectedUserConnectionReader connectedUserConnectionReader,
        ConnectedUserConnectionService connectedUserConnectionService, ConnectedUserService connectedUserService,
        ConnectionDefinitionService connectionDefinitionService, ConnectionFacade connectionFacade,
        IntegrationInstanceConfigurationService integrationInstanceConfigurationService,
        IntegrationInstanceService integrationInstanceService) {

        this.connectedUserConnectionMembership = connectedUserConnectionMembership;
        this.connectedUserConnectionReader = connectedUserConnectionReader;
        this.connectedUserConnectionService = connectedUserConnectionService;
        this.connectedUserService = connectedUserService;
        this.connectionDefinitionService = connectionDefinitionService;
        this.connectionFacade = connectionFacade;
        this.integrationInstanceConfigurationService = integrationInstanceConfigurationService;
        this.integrationInstanceService = integrationInstanceService;
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_MANAGE') " +
        "or isCurrentConnectedUser(#connectedUserId)")
    public long createConnectedUserConnection(long connectedUserId, ConnectionDTO connectionDTO) {
        long connectionId = connectionFacade.create(connectionDTO, PlatformType.EMBEDDED);

        connectedUserConnectionService.create(connectedUserId, connectionId);

        return connectionId;
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_MANAGE') " +
        "or isCurrentConnectedUser(#connectedUserId)")
    public void deleteConnectedUserConnection(long connectedUserId, long connectionId) {
        requireOwned(connectedUserId, connectionId);

        connectedUserConnectionService.deleteByConnectionId(connectionId);

        connectionFacade.delete(connectionId);
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_VIEW') " +
        "or isCurrentConnectedUser(#connectedUserId)")
    public List<ConnectionDTO> getConnectedUserConnections(
        long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        return getConnections(connectedUserId, componentName, connectionIds);
    }

    @Override
    @PreAuthorize("#externalUserId == authentication.name")
    public List<ConnectionDTO> getConnectedUserConnections(
        String externalUserId, Environment environment, @Nullable String componentName, List<Long> connectionIds) {

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(externalUserId, environment);

        return connectedUserConnectionReader.getConnections(connectedUser.getId(), componentName, connectionIds);
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_VIEW') " +
        "or isCurrentConnectedUser(#connectedUserId)")
    public List<ConnectionDTO> getConnections(
        Long connectedUserId, @Nullable String componentName, List<Long> connectionIds) {

        return connectedUserConnectionReader.getConnections(connectedUserId, componentName, connectionIds);
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_MANAGE') " +
        "or isCurrentConnectedUser(#connectedUserId)")
    public void reauthorizeConnectedUserConnection(long connectedUserId, long connectionId, Map<String, ?> parameters) {
        ConnectionDTO connectionDTO = requireOwned(connectedUserId, connectionId);

        connectionFacade.replaceAuthorizationParameters(
            connectionId, withIntegrationCredentials(connectedUserId, connectionDTO, parameters));
    }

    @Override
    @PreAuthorize("hasPermission(#connectedUserId, 'ConnectedUser', 'CONNECTED_USER_VIEW') " +
        "or isCurrentConnectedUser(#connectedUserId)")
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
    @PreAuthorize("hasEmbeddedScope('CONNECTION_VIEW') or isConnectedUser()")
    public Set<Long> getSharedConnectionIds() {
        return connectedUserConnectionService.getSharedConnectionIds();
    }

    private ConnectionDTO requireOwned(long connectedUserId, long connectionId) {
        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Set<Long> ownedConnectionIds = connectedUserConnectionMembership.getOwnedConnectionIds(
            connectedUser.getId(), connectedUser.getEnvironment());

        List<ConnectionDTO> ownedConnectionDTOs = connectionFacade.getConnections(
            new ArrayList<>(ownedConnectionIds), PlatformType.EMBEDDED);

        return ownedConnectionDTOs.stream()
            .filter(connectionDTO -> Objects.equals(connectionDTO.id(), connectionId))
            .findFirst()
            .orElseThrow(() -> new NoSuchElementException("Connection id=%s not found".formatted(connectionId)));
    }

    private Map<String, ?> withIntegrationCredentials(
        long connectedUserId, ConnectionDTO connectionDTO, Map<String, ?> parameters) {

        long connectionId = Objects.requireNonNull(connectionDTO.id(), "connection id");

        ConnectedUser connectedUser = connectedUserService.getConnectedUser(connectedUserId);

        Optional<IntegrationInstance> integrationInstanceOptional = integrationInstanceService
            .getConnectedUserIntegrationInstances(connectedUserId, connectedUser.getEnvironment())
            .stream()
            .filter(integrationInstance -> integrationInstance.getConnectionId() == connectionId)
            .findFirst();

        if (integrationInstanceOptional.isEmpty()) {
            if (log.isDebugEnabled()) {
                log.debug(
                    "Connection id={} of connected user id={} is not backed by an integration instance, " +
                        "reauthorizing without the integration configuration credentials",
                    connectionId, connectedUserId);
            }

            return parameters;
        }

        IntegrationInstance integrationInstance = integrationInstanceOptional.get();

        IntegrationInstanceConfiguration integrationInstanceConfiguration = integrationInstanceConfigurationService
            .getIntegrationInstanceConfiguration(integrationInstance.getIntegrationInstanceConfigurationId());

        List<String> authorizationPropertyNames = getAuthorizationPropertyNames(connectionDTO);

        Map<String, ?> connectionParameters = integrationInstanceConfiguration.getConnectionParameters();

        Map<String, Object> mergedParameters = new HashMap<>();

        for (Map.Entry<String, ?> connectionParameterEntry : connectionParameters.entrySet()) {
            if (authorizationPropertyNames.contains(connectionParameterEntry.getKey())) {
                mergedParameters.put(connectionParameterEntry.getKey(), connectionParameterEntry.getValue());
            }
        }

        mergedParameters.putAll(parameters);

        return mergedParameters;
    }

    private List<String> getAuthorizationPropertyNames(ConnectionDTO connectionDTO) {
        ConnectionDefinition connectionDefinition = connectionDefinitionService.getConnectionConnectionDefinition(
            connectionDTO.componentName(), connectionDTO.connectionVersion());

        return connectionDefinition.getAuthorizations()
            .stream()
            .flatMap(authorization -> CollectionUtils.stream(authorization.getProperties()))
            .map(BaseProperty::getName)
            .toList();
    }
}
