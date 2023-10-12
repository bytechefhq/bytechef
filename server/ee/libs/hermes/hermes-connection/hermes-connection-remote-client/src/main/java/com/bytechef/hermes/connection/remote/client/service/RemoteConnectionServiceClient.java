/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.hermes.connection.remote.client.service;

import com.bytechef.commons.webclient.LoadBalancedWebClient;
import com.bytechef.hermes.connection.domain.Connection;
import com.bytechef.hermes.connection.service.ConnectionService;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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

    private final LoadBalancedWebClient loadBalancedWebClient;

    @SuppressFBWarnings("EI")
    public RemoteConnectionServiceClient(LoadBalancedWebClient loadBalancedWebClient) {
        this.loadBalancedWebClient = loadBalancedWebClient;
    }

    @Override
    public Connection create(Connection connection) {
        throw new UnsupportedOperationException();
    }

    @Override
    public void delete(long id) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Optional<Connection> fetchConnection(long id) {
        return Optional.ofNullable(
            loadBalancedWebClient.get(
                uriBuilder -> uriBuilder
                    .host("connection-app")
                    .path("/remote/connection-service/fetch-connection/{id}")
                    .build(id),
                Connection.class));
    }

    @Override
    public Connection getConnection(long id) {
        return fetchConnection(id).orElseThrow();
    }

    @Override
    public List<Connection> getConnections() {
        return loadBalancedWebClient.get(
            uriBuilder -> uriBuilder
                .host("connection-app")
                .path("/remote/connection-service/get-connections")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Connection> getConnections(List<Long> ids) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, Integer connectionVersion, Long tagId) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        throw new UnsupportedOperationException();
    }

    @Override
    public Connection update(Connection connection) {
        throw new UnsupportedOperationException();
    }
}
