/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.ee.platform.connection.remote.client.service;

import com.bytechef.ee.remote.client.LoadBalancedRestClient;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionEnvironment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.AppType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.List;
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
    public List<Connection> getConnections(AppType type) {
        return loadBalancedRestClient.get(
            uriBuilder -> uriBuilder
                .host("connection-app")
                .path("/remote/connection-service/get-connections")
                .build(),
            new ParameterizedTypeReference<>() {});
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, AppType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, ConnectionEnvironment connectionEnvironment, Long tagId,
        AppType type) {

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

    @Override
    public Connection updateConnectionParameter(long connectionId, String key, Object value) {
        throw new UnsupportedOperationException();
    }
}
