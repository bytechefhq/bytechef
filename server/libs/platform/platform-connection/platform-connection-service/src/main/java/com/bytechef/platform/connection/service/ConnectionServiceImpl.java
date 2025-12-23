/*
 * Copyright 2025 ByteChef
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

package com.bytechef.platform.connection.service;

import com.bytechef.commons.util.CollectionUtils;
import com.bytechef.commons.util.FormatUtils;
import com.bytechef.commons.util.OptionalUtils;
import com.bytechef.component.definition.Authorization.AuthorizationType;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import org.apache.commons.lang3.StringUtils;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.domain.Sort;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service("connectionService")
@Transactional
public class ConnectionServiceImpl implements ConnectionService {

    private static final Logger logger = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    private final ConnectionRepository connectionRepository;

    public ConnectionServiceImpl(ConnectionRepository connectionRepository) {
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(Connection connection) {
        Assert.notNull(connection, "'connection' must not be null");
        Assert.hasText(connection.getComponentName(), "'componentName' must not be empty");
        Assert.hasText(connection.getName(), "'name' must not be empty");
        Assert.isTrue(connection.getId() == null, "'id' must be null");

        return connectionRepository.save(connection);
    }

    @Override
    public Connection create(
        @Nullable AuthorizationType authorizationType, String componentName, int connectionVersion,
        int environmentId, String name, Map<String, Object> parameters, PlatformType type) {

        Assert.hasText(componentName, "'componentName' must not be empty");
        Assert.hasText(name, "'name' must not be empty");
        Assert.notNull(environmentId, "'environment' must not be null");
        Assert.notNull(parameters, "'parameters' must not be null");
        Assert.notNull(type, "'type' must not be null");

        Connection connection = new Connection();

        connection.setAuthorizationType(authorizationType);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setEnvironmentId(environmentId);
        connection.setName(name);
        connection.setParameters(parameters);
        connection.setType(type);

        if (logger.isTraceEnabled()) {
            logger.trace("Saved..: {}", FormatUtils.toString(parameters));
        }

        return create(connection);
    }

    @Override
    public void delete(long id) {
        connectionRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        return CollectionUtils.filter(
            connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        return connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
            componentName, version, type.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(
        String componentName, Integer connectionVersion, Long tagId, Long environmentId, PlatformType type) {

        List<Connection> connections;

        if (StringUtils.isBlank(componentName) && tagId == null) {
            connections = connectionRepository.findAllByTypeOrderByName(type.ordinal());
        } else if (StringUtils.isNotBlank(componentName) && tagId == null) {
            if (connectionVersion == null) {
                connections = connectionRepository.findAllByComponentNameAndTypeOrderByName(
                    componentName, type.ordinal());
            } else {
                connections = connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                    componentName, connectionVersion, type.ordinal());
            }
        } else if (StringUtils.isBlank(componentName)) {
            connections = connectionRepository.findAllByTagIdAndTypeOrderByName(tagId, type.ordinal());
        } else {
            if (connectionVersion == null) {
                connections = connectionRepository.findAllByComponentNameAndTagIdAndTypeOrderByName(
                    componentName, tagId, type.ordinal());
            } else {
                connections = connectionRepository.findAllByCNCVTITOrderByName(
                    componentName, connectionVersion, tagId, type.ordinal());
            }
        }

        if (environmentId != null) {
            connections = connections.stream()
                .filter(connection -> connection.getEnvironmentId() == environmentId)
                .toList();
        }

        return CollectionUtils.toList(connections);
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        return connectionRepository.findAllByIdIn(connectionIds);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        Connection connection = getConnection(id);

        connection.setTagIds(tagIds);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        Connection curConnection = getConnection(id);

        if (name != null) {
            curConnection.setName(name);
        }

        if (tagIds != null) {
            curConnection.setTagIds(tagIds);
        }

        curConnection.setVersion(version);

        return connectionRepository.save(curConnection);
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, CredentialStatus status) {
        Assert.notNull(status, "'status' must not be null");

        Connection connection = getConnection(connectionId);

        connection.setCredentialStatus(status);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);

        if (logger.isTraceEnabled()) {
            logger.trace("New....: {}", FormatUtils.toString(parameters));
        }

        Map<String, Object> curParameters = new HashMap<>(connection.getParameters());

        if (logger.isTraceEnabled()) {
            logger.trace("Current: {}", FormatUtils.toString(curParameters));
        }

        curParameters.putAll(parameters);

        connection.setParameters(curParameters);

        if (logger.isTraceEnabled()) {
            logger.trace("Saved..: {}", FormatUtils.toString(curParameters));
        }

        return connectionRepository.save(connection);
    }

}
