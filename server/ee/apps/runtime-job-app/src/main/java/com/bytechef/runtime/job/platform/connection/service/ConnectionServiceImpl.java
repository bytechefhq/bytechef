/*
 * Copyright 2025 ByteChef
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.job.platform.connection.service;

import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.runtime.job.platform.connection.ConnectionContext;
import java.util.List;
import java.util.Map;
import org.springframework.stereotype.Service;

/**
 * @version ee
 *
 * @author Ivica Cardic
 */
@Service
public class ConnectionServiceImpl implements ConnectionService {

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
        Connection connection = new Connection();

        connection.setId(id);
        connection.setParameters(ConnectionContext.getConnectionParameters(id));

        return connection;
    }

    @Override
    public List<Connection> getConnections(PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        throw new UnsupportedOperationException();
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Long environmentId, PlatformType type) {

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
