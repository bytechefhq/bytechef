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
import com.bytechef.platform.credential.store.CredentialStore;
import com.bytechef.platform.credential.store.CredentialStoreType;
import com.bytechef.platform.credential.store.exception.ReadOnlyCredentialStoreException;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.domain.ResourceVisibility;
import com.bytechef.platform.security.util.SecurityUtils;
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
import org.springframework.security.access.AccessDeniedException;
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
    private final ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider;

    @SuppressFBWarnings("EI2")
    public ConnectionServiceImpl(
        List<CredentialStore> credentialStores, ConnectionRepository connectionRepository,
        ObjectProvider<AiProviderConnectionRepository> aiProviderConnectionRepositoryProvider) {

        this.credentialStores = credentialStores;
        this.connectionRepository = connectionRepository;
        this.aiProviderConnectionRepositoryProvider = aiProviderConnectionRepositoryProvider;
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
        rejectIfAiProviderConnection(id);

        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        validateOwnerOrAdmin(connection);

        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (!store.isReadOnly()) {
            store.deleteSecret(connection);
        }
        // When the active store is read-only, the connection row is deleted but the external secret is left intact.
        // The operator owns the vault lifecycle; documented behavior.

        connectionRepository.delete(connection);
    }

    @Override
    @Transactional(readOnly = true)
    public Connection getConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            AiProviderConnectionRepository repository = aiProviderConnectionRepository();

            if (repository != null) {
                return repository.findById(id)
                    .orElseThrow(() -> new NoSuchElementException("Connection does not exist for id=" + id));
            }
        }

        return populateParameters(
            OptionalUtils.get(connectionRepository.findById(id), "Connection does not exist for id=" + id));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Connection> fetchConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            AiProviderConnectionRepository repository = aiProviderConnectionRepository();

            return repository == null ? Optional.empty() : repository.findById(id);
        }

        return connectionRepository.findById(id)
            .map(this::populateParameters);
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(PlatformType type) {
        List<Connection> connections = populateAll(
            CollectionUtils.filter(
                connectionRepository.findAll(Sort.by("name", "id")), connection -> connection.getType() == type));

        return mergeByName(connections, projectedConnections(type, null, null, null));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnectionsByVisibility(ResourceVisibility visibility, PlatformType type) {
        return connectionRepository.findAllByVisibilityAndTypeOrderByName(visibility.ordinal(), type.ordinal());
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getConnections(String componentName, int version, PlatformType type) {
        List<Connection> connections = populateAll(
            connectionRepository.findAllByComponentNameAndConnectionVersionAndTypeOrderByName(
                componentName, version, type.ordinal()));

        return mergeByName(connections, projectedConnections(type, componentName, version, null));
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

        List<Connection> result = populateAll(CollectionUtils.toList(connections));

        if (tagId != null) {
            return result;
        }

        return mergeByName(result, projectedConnections(type, componentName, connectionVersion, environmentId));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getAiProviderConnections(
        String componentName, Integer connectionVersion, Integer environmentId) {

        AiProviderConnectionRepository repository = aiProviderConnectionRepository();

        if (repository == null) {
            return List.of();
        }

        return repository.find(componentName, connectionVersion, environmentId);
    }

    @Override
    public List<Connection> getConnections(List<Long> connectionIds) {
        List<Long> realIds = connectionIds.stream()
            .filter(id -> id != null && !AiProviderConnectionId.isAiProviderConnectionId(id))
            .toList();

        List<Connection> connections = populateAll(connectionRepository.findAllByIdIn(realIds));

        List<Long> virtualIds = connectionIds.stream()
            .filter(id -> id != null && AiProviderConnectionId.isAiProviderConnectionId(id))
            .toList();

        AiProviderConnectionRepository repository = aiProviderConnectionRepository();

        if (repository != null && !virtualIds.isEmpty()) {
            return mergeByName(connections, repository.findAllByIdIn(virtualIds));
        }

        return connections;
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

        // Probe existence — fail-fast if the secret doesn't actually exist in the external store.
        store.getSecret(connection);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, List<Long> tagIds) {
        rejectIfAiProviderConnection(id);

        Connection connection = getConnection(id);

        validateOwnerOrAdmin(connection);

        connection.setTagIds(tagIds);

        // Clear the in-memory populated parameters before saving so that external-store rows
        // (credentialStoreType != DATABASE) do not write the secret back into the inline column.
        if (connection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            connection.setParameters(Map.of());
        }

        return populateParameters(connectionRepository.save(connection));
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        rejectIfAiProviderConnection(id);

        Connection curConnection = getConnection(id);

        validateOwnerOrAdmin(curConnection);

        if (name != null) {
            curConnection.setName(name);
        }

        if (tagIds != null) {
            curConnection.setTagIds(tagIds);
        }

        curConnection.setVersion(version);

        // Clear the in-memory populated parameters before saving so that external-store rows
        // (credentialStoreType != DATABASE) do not write the secret back into the inline column.
        if (curConnection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            curConnection.setParameters(Map.of());
        }

        return populateParameters(connectionRepository.save(curConnection));
    }

    @Override
    public Connection updateConnectionCredentialStatus(long connectionId, CredentialStatus status) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(status, "'status' must not be null");

        Connection connection = getConnection(connectionId);

        validateOwnerOrAdmin(connection);

        connection.setCredentialStatus(status);

        // Clear the in-memory populated parameters before saving so that external-store rows
        // (credentialStoreType != DATABASE) do not write the secret back into the inline column.
        if (connection.getCredentialStoreType() != CredentialStoreType.DATABASE) {
            connection.setParameters(Map.of());
        }

        Connection updatedConnection = connectionRepository.save(connection);

        updatedConnection.setCredentialsStatusUpdated();

        return populateParameters(updatedConnection);
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        // No @PreAuthorize here by design. Authorization lives at the public entry points:
        // ConnectionReassignmentGraphQlController marks every reassignment mutation admin-only, and
        // WorkspaceUserRemovalListener is a trusted system listener that runs after-commit with a
        // cleared SecurityContext — a service-level ADMIN guard would throw AccessDeniedException
        // for the listener path and leave orphaned connections stuck in ACTIVE. Mirrors the
        // convention already applied to updateVisibility.
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

        // No @PreAuthorize here by design — see updateConnectionStatus for the rationale. The
        // listener-driven reassignment path needs to run without an authenticated principal.
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connection.setCreatedBy(newCreatedBy);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateVisibility(long id, ResourceVisibility visibility) {
        rejectIfAiProviderConnection(id);

        // No @PreAuthorize here by design. The caller (WorkspaceConnectionFacadeImpl) performs the
        // admin-OR-creator check so the orphan-recovery demote path works when no admins remain.
        // A service-level ADMIN guard would block that flow even though the facade authorised it.
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connection.setVisibility(visibility);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionParameters(long connectionId, Map<String, ?> parameters) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        validateOwnerOrAdmin(connection);

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

    @Override
    public Connection replaceConnectionParameters(long connectionId, Map<String, ?> parameters) {
        rejectIfAiProviderConnection(connectionId);

        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);
        CredentialStore store = getStore(connection.getCredentialStoreType());

        if (store.isReadOnly()) {
            throw new ReadOnlyCredentialStoreException(store.getType());
        }

        validateOwnerOrAdmin(connection);

        store.storeSecret(connection, new HashMap<>(parameters));

        return populateParameters(connectionRepository.save(connection));
    }

    @Override
    @Transactional(readOnly = true)
    public List<Connection> getInactiveConnections(List<Long> connectionIds) {
        if (connectionIds == null || connectionIds.isEmpty()) {
            return List.of();
        }

        // Connection.getStatus() is total: it throws IllegalStateException for a corrupted INT ordinal
        // rather than returning null, so a simple != ACTIVE filter captures every non-ACTIVE status
        // including any future value added by append-only enum extension. A corrupted row surfaces
        // loudly here rather than silently masking as ACTIVE.
        return connectionRepository.findAllByIdIn(connectionIds)
            .stream()
            .filter(connection -> connection.getStatus() != ConnectionStatus.ACTIVE)
            .toList();
    }

    @Override
    @Transactional(readOnly = true)
    public void validateConnectionsActive(List<Long> connectionIds) {
        List<Connection> inactive = getInactiveConnections(connectionIds);

        if (inactive.isEmpty()) {
            return;
        }

        String detail = inactive.stream()
            .map(connection -> "id=%s status=%s".formatted(connection.getId(), connection.getStatus()))
            .reduce((left, right) -> left + ", " + right)
            .orElse("");

        throw new ConfigurationException(
            "Workflow execution blocked: %d non-ACTIVE connection(s): %s. Reassign or reactivate to resume."
                .formatted(inactive.size(), detail),
            ConnectionErrorType.CONNECTION_NOT_ACTIVE);
    }

    @Nullable
    private AiProviderConnectionRepository aiProviderConnectionRepository() {
        return aiProviderConnectionRepositoryProvider.getIfAvailable();
    }

    private List<Connection> projectedConnections(
        PlatformType type, @Nullable String componentName, @Nullable Integer connectionVersion,
        @Nullable Long environmentId) {

        AiProviderConnectionRepository repository = aiProviderConnectionRepository();

        if (repository == null || type != PlatformType.AUTOMATION) {
            return List.of();
        }

        return repository.find(
            componentName, connectionVersion, environmentId == null ? null : environmentId.intValue());
    }

    private static List<Connection> mergeByName(List<Connection> connections, List<Connection> projected) {
        if (projected.isEmpty()) {
            return connections;
        }

        List<Connection> merged = new ArrayList<>(connections);

        merged.addAll(projected);
        merged.sort(Comparator.comparing(Connection::getName, String.CASE_INSENSITIVE_ORDER));

        return merged;
    }

    private static void rejectIfAiProviderConnection(long id) {
        if (AiProviderConnectionId.isAiProviderConnectionId(id)) {
            throw new ConfigurationException(
                "Connection id=%s is an AI provider connection and is read-only".formatted(id),
                ConnectionErrorType.AI_PROVIDER_CONNECTION_READ_ONLY);
        }
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

    private void validateOwnerOrAdmin(Connection connection) {
        if (!SecurityUtils.isAuthenticated()) {
            throw new AccessDeniedException(
                "Authentication required to modify connection " + connection.getId());
        }

        String currentUserLogin = SecurityUtils.getCurrentUserLogin();

        if (!currentUserLogin.equals(connection.getCreatedBy()) &&
            !SecurityUtils.hasCurrentUserThisAuthority(AuthorityConstants.ADMIN)) {

            throw new AccessDeniedException(
                "Only the connection creator or an admin can modify connection " + connection.getId());
        }
    }
}
