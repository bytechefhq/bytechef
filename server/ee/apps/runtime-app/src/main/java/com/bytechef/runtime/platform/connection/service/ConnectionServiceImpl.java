/*
 * Copyright 2023-present ByteChef Inc.
 *
 * Licensed under the ByteChef Enterprise license (the "Enterprise License");
 * you may not use this file except in compliance with the Enterprise License.
 */

package com.bytechef.runtime.platform.connection.service;

import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.ConnectionEnvironment;
import com.bytechef.platform.connection.service.ConnectionService;
import com.bytechef.platform.constant.ModeType;
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
        return null;
    }

    @Override
    public void delete(long id) {

    }

    @Override
    public Connection getConnection(long id) {
        return null;
    }

    @Override
    public List<Connection> getConnections(ModeType type) {
        return List.of();
    }

    @Override
    public List<Connection> getConnections(String componentName, int version, ModeType type) {
        return List.of();
    }

    @Override
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, ConnectionEnvironment connectionEnvironment, Long tagId,
        ModeType type) {
        return List.of();
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        return null;
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        return null;
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, Connection.CredentialStatus status) {
        return null;
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        return null;
    }
}
