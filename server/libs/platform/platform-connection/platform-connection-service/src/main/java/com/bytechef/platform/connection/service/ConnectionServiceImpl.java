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
import com.bytechef.exception.ConfigurationException;
import com.bytechef.platform.connection.domain.AiProviderConnectionId;
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.repository.AiProviderConnectionRepository;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.ObjectProvider;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.Assert;

/**
 * @author Ivica Cardic
 */
@Service("connectionService")
@Transactional
public class ConnectionServiceImpl implements ConnectionService {
    private static final Logger log = LoggerFactory.getLogger(ConnectionServiceImpl.class);

    private final ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider;
    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI")
    public ConnectionServiceImpl(
        ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider,
        ConnectionRepository connectionRepository) {

        this.aiProviderConnectionRepositoryProvider = aiProviderConnectionRepositoryProvider;
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
        int environmentId, String name, Map<String, Object> parameters, PlatformType platformType) {
        Assert.hasText(componentName, "'componentName' must not be empty");
        Assert.hasText(name, "'name' must not be empty");
        Assert.notNull(parameters, "'parameters' must not be null");
        Assert.notNull(platformType, "'platformType' must not be null");

        Connection connection = new Connection();

        connection.setAuthorizationType(authorizationType);
        connection.setComponentName(componentName);
        connection.setConnectionVersion(connectionVersion);
        connection.setEnvironmentId(environmentId);
        connection.setName(name);
        connection.setParameters(parameters);
        connection.setType(platformType);

        if (log.isTraceEnabled()) {
            log.trace("Saved..: {}", FormatUtils.toString(parameters));
        }

        return create(connection);
    }

    @Override
    public void delete(long id) {
        rejectIfAiProviderConnection(id);

        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connectionRepository.delete(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            AiProviderConnectionRepository aiProviderConnectionRepository = getAiProviderConnectionRepository();

            if (aiProviderConnectionRepository != null) {
                return OptionalUtils.get(
                    aiProviderConnectionRepository.findById(id), "Connection does not exist for id=" + id);
            }
        }

        return OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id);
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Connection> fetchConnection(long id) {
        return connectionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        List<Connection> connections = CollectionUtils.filter(
            connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type);

        return mergeByName(connections, getProjectedConnections(type, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        List<Connection> connections =
            connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                componentName, version, type.ordinal());

        return mergeByName(connections, getProjectedConnections(type, componentName, version, null));
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

        List<Connection> connectionList = CollectionUtils.toList(connections);

        if (tagId != null) {
            return connectionList;
        }

        return mergeByName(
            connectionList, getProjectedConnections(type, componentName, connectionVersion, environmentId));
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        List<Long> realConnectionIds = connectionIds.stream()
            .filter(
                connectionId -> connectionId != null && !AiProviderConnectionId.isAiProviderConnectionId(connectionId))
            .toList();

        List<Connection> connections = connectionRepository.findAllByIdIn(realConnectionIds);

        List<Long> aiProviderConnectionIds = connectionIds.stream()
            .filter(
                connectionId -> connectionId != null && AiProviderConnectionId.isAiProviderConnectionId(connectionId))
            .toList();

        AiProviderConnectionRepository aiProviderConnectionRepository = getAiProviderConnectionRepository();

        if (aiProviderConnectionRepository == null || aiProviderConnectionIds.isEmpty()) {
            return connections;
        }

        return mergeByName(connections, aiProviderConnectionRepository.findAllByIdIn(aiProviderConnectionIds));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getAiProviderConnections(
        @Nullable String componentName, @Nullable Integer connectionVersion, @Nullable Integer environmentId) {

        AiProviderConnectionRepository aiProviderConnectionRepository = getAiProviderConnectionRepository();

        if (aiProviderConnectionRepository == null) {
            return List.of();
        }

        return aiProviderConnectionRepository.find(componentName, connectionVersion, environmentId);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        rejectIfAiProviderConnection(id);

        Connection connection = getConnection(id);

        connection.setTagIds(tagIds);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        rejectIfAiProviderConnection(id);

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
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(status, "'status' must not be null");

        Connection connection = getConnection(connectionId);

        connection.setCredentialStatus(status);
        connection.setCredentialsStatusUpdated();

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(status, "'status' must not be null");

        Connection connection = connectionRepository.findById(connectionId)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + connectionId));

        try {
            connection.setStatus(status);
        } catch (IllegalStateException exception) {
            throw new ConfigurationException(exception.getMessage(), ConnectionErrorType.INVALID_CONNECTION);
        }

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateCreatedBy(long id, String newCreatedBy) {
        rejectIfAiProviderConnection(id);

        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connection.setCreatedBy(newCreatedBy);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);

        if (log.isTraceEnabled()) {
            log.trace("New....: {}", FormatUtils.toString(parameters));
        }

        Map<String, Object> curParameters = new HashMap<>(connection.getParameters());

        if (log.isTraceEnabled()) {
            log.trace("Current: {}", FormatUtils.toString(curParameters));
        }

        curParameters.putAll(parameters);

        connection.setParameters(curParameters);

        if (log.isTraceEnabled()) {
            log.trace("Saved..: {}", FormatUtils.toString(curParameters));
        }

        return connectionRepository.save(connection);
    }

    @Override
    public Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);

        connection.setParameters(new HashMap<>(parameters));

        return connectionRepository.save(connection);
    }

    private @Nullable AiProviderConnectionRepository getAiProviderConnectionRepository() {
        return aiProviderConnectionRepositoryProvider.getIfAvailable();
    }

    private List<Connection> getProjectedConnections(
        PlatformType type, @Nullable String componentName, @Nullable Integer connectionVersion,
        @Nullable Long environmentId) {

        AiProviderConnectionRepository aiProviderConnectionRepository = getAiProviderConnectionRepository();

        if (aiProviderConnectionRepository == null || type != PlatformType.AUTOMATION) {
            return List.of();
        }

        return aiProviderConnectionRepository.find(
            componentName, connectionVersion, environmentId == null ? null : environmentId.intValue());
    }

    private static List<Connection> mergeByName(List<Connection> connections, List<Connection> projectedConnections) {
        if (projectedConnections.isEmpty()) {
            return connections;
        }

        List<Connection> mergedConnections = new ArrayList<>(connections);

        mergedConnections.addAll(projectedConnections);

        mergedConnections.sort(Comparator.comparing(Connection::getName, String.CASE_INSENSITIVE_ORDER));

        return mergedConnections;
    }

    private static void rejectIfAiProviderConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            throw new ConfigurationException(
                "Connection id=%s is an AI provider connection and is read-only".formatted(id),
                ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY);
        }
    }
}
