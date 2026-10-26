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
import com.bytechef.platform.connection.domain.Connection;
import com.bytechef.platform.connection.domain.Connection.CredentialStatus;
import com.bytechef.platform.connection.domain.ConnectionStatus;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException;
import edu.umd.cs.findbugs.annotations.SuppressFBWarnings;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import java.util.Optional;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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

    private final List<CredentialStore> credentialStores;
    private final ConnectionRepository connectionRepository;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(List<CredentialStore> credentialStores, ConnectionRepository connectionRepository) {
        this.credentialStores = credentialStores;
        this.connectionRepository = connectionRepository;
    }

    @Override
    public Connection create(Connection connection) {
        Assert.notNull(connection, "'connection' must not be null");
        Assert.hasText(connection.getComponentName(), "'componentName' must not be empty");
        Assert.hasText(connection.getName(), "'name' must not be empty");
        Assert.isTrue(connection.getId() == null, "'id' must be null");

        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        Map<String, ?> parameters = connection.getParameters();

        store.storeSecret(connection, parameters);

        Connection saved = connectionRepository.save(connection);

        return populateParameters(saved);
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
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (!store.isReadOnly()) {
            store.deleteSecret(connection);
        }

        connectionRepository.delete(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        return populateParameters(
            OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Connection> fetchConnection(long id) {
        return connectionRepository.findById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        return populateAll(
            CollectionUtils.filter(
                connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        return populateAll(
            connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                componentName, version, type.ordinal()));
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

        return populateAll(CollectionUtils.toList(connections));
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        return populateAll(connectionRepository.findAllByIdIn(connectionIds));
    }

    @Override
    public Connection registerExisting(
        Connection connection, CredentialStoreType storeType, String credentialRef) {

        Assert.notNull(connection, "'connection' must not be null");
        Assert.notNull(storeType, "'storeType' must not be null");
        Assert.isTrue(storeType != CredentialStoreType.DATABASE, "registerExisting requires an external store");
        Assert.hasText(credentialRef, "'credentialRef' must not be empty");

        CredentialStore store = getStore(storeType);

        connection.setCredentialStoreType(storeType);
        connection.setCredentialRef(credentialRef);
        connection.setParameters(Map.of());

        store.getSecret(connection);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        Connection connection = getConnection(id);

        connection.setTagIds(tagIds);

        if (connection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            connection.setParameters(Map.of());
        }

        return populateParameters(connectionRepository.save(connection));
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

        if (curConnection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            curConnection.setParameters(Map.of());
        }

        return populateParameters(connectionRepository.save(curConnection));
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, CredentialStatus status) {
        Assert.notNull(status, "'status' must not be null");

        Connection connection = getConnection(connectionId);

        connection.setCredentialStatus(status);
        connection.setCredentialsStatusUpdated();

        if (connection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            connection.setParameters(Map.of());
        }

        return populateParameters(connectionRepository.save(connection));
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
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
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connection.setCreatedBy(newCreatedBy);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        if (log.isTraceEnabled()) {
            log.trace("New....: {}", FormatUtils.toString(parameters));
        }

        Map<String, Object> curParameters = new HashMap<>(store.getSecret(connection));

        if (log.isTraceEnabled()) {
            log.trace("Current: {}", FormatUtils.toString(curParameters));
        }

        curParameters.putAll(parameters);

        store.storeSecret(connection, curParameters);

        if (log.isTraceEnabled()) {
            log.trace("Saved..: {}", FormatUtils.toString(curParameters));
        }

        return populateParameters(connectionRepository.save(connection));
    }

    private CredentialStore getStore(CredentialStoreType type) {
        return credentialStores.stream()
            .filter(store -> store.getType() == type)
            .findFirst()
            .orElseThrow(() -> new IllegalStateException(
                ("No CredentialStore registered for type %s. Configure bytechef.credential-store.external.provider" +
                    " or migrate this row to DATABASE.").formatted(type)));
    }

    private Connection populateParameters(Connection connection) {
        CredentialStore store = getStore(connection.getCredentialStoreType());

        connection.setParameters(store.getSecret(connection));

        return connection;
    }

    private List<Connection> populateAll(List<Connection> connections) {
        connections.forEach(this::populateParameters);

        return connections;
    }

    @Override
    public Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters) {
        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        store.storeSecret(connection, new HashMap<>(parameters));

        return populateParameters(connectionRepository.save(connection));
    }
}
