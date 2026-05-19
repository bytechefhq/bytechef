/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.security.domain.ResourceVisibility;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.springframework.core.ParameterizedTypeReference;
import org.springframework.stereotype.Component;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Component
public class RemoteConnectionServiceClient implements ConnectionService {

    private final LoadBalancedRestClient loadBalancedRestClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectionServiceClient(LoadBalancedRestClient loadBalancedRestClient) {
        this.loadBalancedRestClient = loadBalancedRestClient;
    }

    @Override
    public Connection create(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection create(
        AuthorizationType authorizationType, String componentName, int connectionVersion, int environmentId,
        String name, Map<String, Object> parameters, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection getConnection(long id) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host("connection-app")
                .path("/remote/connection-service/get-connection/{id}")
                .build(id),
            Connection.class);
    }

    @Override
    public Optional<Connection> fetchConnection(long id) {
        // The remote endpoint has no not-found variant, so absence cannot be distinguished here. Callers on this
        // path address ids the coordinator already resolved.
        return Optional.ofNullable(getConnection(id));
    }

    @Override
    public List<Connection> getConnections(PlatformType type) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host("connection-app")
                .path("/remote/connection-service/get-connections")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Connection> getConnectionsByVisibility(ResourceVisibility visibility, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long typeId, Long environmentId, PlatformType type) {

        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection registerExisting(
        Connection connection, CredentialStoreType storeType, String credentialRef) {

        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, Connection.CredentialStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateCreatedBy(long id, String newCreatedBy) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection updateVisibility(long id, ResourceVisibility visibility) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getInactiveConnections(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void validateConnectionsActive(List<Long> connectionIds) {
        throw new UnsupportedOperationException();
    }
}
