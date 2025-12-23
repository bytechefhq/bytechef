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
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
import java.util.Map;
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
    public List<Connection> getConnections(PlatformType type) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host("connection-app")
                .path("/remote/connection-service/get-connections")
                .build(),
            new ParameterizedTypeReference<>() {});
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
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        throw new UnsupportedOperationException();
    }
}
