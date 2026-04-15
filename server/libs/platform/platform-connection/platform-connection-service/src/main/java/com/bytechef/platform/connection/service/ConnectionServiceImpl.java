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
import com.bytechef.platform.connection.domain.ConnectionVisibility;
import com.bytechef.platform.connection.exception.ConnectionErrorType;
import com.bytechef.platform.connection.repository.ConnectionRepository;
import com.bytechef.platform.constant.PlatformType;
import com.bytechef.platform.security.constant.AuthorityConstants;
import com.bytechef.platform.security.util.SecurityUtils;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.NoSuchElementException;
import org.apache.commons.lang3.StringUtils;
import org.jspecify.annotations.Nullable;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
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
        int environmentId, String name, Map<String, Object> parameters, PlatformType platformType) {

        Assert.hasText(componentName, "'componentName' must not be empty");
        Assert.hasText(name, "'name' must not be empty");
        Assert.notNull(environmentId, "'environment' must not be null");
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

        if (logger.isTraceEnabled()) {
            logger.trace("Saved..: {}", FormatUtils.toString(parameters));
        }

        return create(connection);
    }

    @Override
    public void delete(long id) {
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        validateOwnerOrAdmin(connection);

        connectionRepository.delete(connection);
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
    public List<Connection> getConnectionsByVisibility(ConnectionVisibility visibility, PlatformType type) {
        return connectionRepository.findAllByVisibilityAndTypeOrderByName(visibility.ordinal(), type.ordinal());
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

        validateOwnerOrAdmin(connection);

        connection.setTagIds(tagIds);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection update(long id, String name, List<Long> tagIds, int version) {
        Connection curConnection = getConnection(id);

        validateOwnerOrAdmin(curConnection);

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

        validateOwnerOrAdmin(connection);

        connection.setCredentialStatus(status);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateConnectionStatus(long connectionId, ConnectionStatus status) {
        // No @PreAuthorize here by design. Authorization lives at the public entry points:
        // ConnectionReassignmentGraphQlController marks every reassignment mutation admin-only, and
        // WorkspaceUserRemovalListener is a trusted system listener that runs after-commit with a
        // cleared SecurityContext — a service-level ADMIN guard would throw AccessDeniedException
        // for the listener path and leave orphaned connections stuck in ACTIVE. Mirrors the
        // convention already applied to updateVisibility.
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
        // No @PreAuthorize here by design — see updateConnectionStatus for the rationale. The
        // listener-driven reassignment path needs to run without an authenticated principal.
        Connection connection = connectionRepository.findById(id)
            .orElseThrow(() -> new NoSuchElementException("Connection not found: " + id));

        connection.setCreatedBy(newCreatedBy);

        return connectionRepository.save(connection);
    }

    @Override
    public Connection updateVisibility(long id, ConnectionVisibility visibility) {
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
        Assert.notNull(parameters, "'parameters' must not be null");

        Connection connection = getConnection(connectionId);

        validateOwnerOrAdmin(connection);

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
